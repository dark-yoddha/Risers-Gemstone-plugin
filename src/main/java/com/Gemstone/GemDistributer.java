package com.Gemstone;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GemDistributer {
    private static final String DB_NAME = "distributed_gem.db";
    private static final String TABLE   = "gems";

    // Keep this list in sync with your GemGenerator IDs
    private static final List<String> ALL_GEMS = List.of(
            "air_gem",
            "fire_gem",
            "strength_gem",
            "speed_gem",
            "spirit_gem",
            "luck_gem",
            "curse_gem",
            "healing_gem",
            "shock_gem",
            "water_gem"
    );

    private final Connection connection;
    private final Random random = new Random();

    public GemDistributer(JavaPlugin plugin) throws SQLException {
        // ensure plugin folder
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // open SQLite connection
        String path = plugin.getDataFolder() + File.separator + DB_NAME;
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);

        // create table if not exists
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS %s (
                  gem_type TEXT PRIMARY KEY,
                  status   INTEGER NOT NULL
                )
                """.formatted(TABLE));
        }

        // insert any missing gems with status=1
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO " + TABLE + " (gem_type, status) VALUES (?, 1)"
        )) {
            for (String gem : ALL_GEMS) {
                ps.setString(1, gem);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Picks a random gem whose status=1, marks it distributed (status=0).
     * When none remain, resets all to status=1 and retries.
     */
    public String getNextGem() throws SQLException {
        List<String> pool = new ArrayList<>();

        // collect undistributed gems
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT gem_type FROM " + TABLE + " WHERE status = 1"
             )) {
            while (rs.next()) {
                pool.add(rs.getString("gem_type"));
            }
        }

        // if empty, reset and recurse
        if (pool.isEmpty()) {
            resetAll();
            return getNextGem();
        }

        // pick and mark distributed
        String chosen = pool.get(random.nextInt(pool.size()));
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE " + TABLE + " SET status = 0 WHERE gem_type = ?"
        )) {
            ps.setString(1, chosen);
            ps.executeUpdate();
        }

        return chosen;
    }

    /**
     * Resets every gem’s status back to 1 (new set).
     */
    private void resetAll() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("UPDATE " + TABLE + " SET status = 1");
        }
    }

    /**
     * Closes the DB connection. Call in onDisable().
     */
    public void close() {
        try {
            if (!connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
    }
}