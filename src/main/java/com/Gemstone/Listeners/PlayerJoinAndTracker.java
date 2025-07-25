package com.Gemstone.Listeners;

import com.Gemstone.GemDistributer;
import com.Gemstone.Items.GemGenerator;
import com.Gemstone.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public class PlayerJoinAndTracker implements Listener {
    private final JavaPlugin plugin;
    private final GemDistributer distributer;
    private final File file;
    private final FileConfiguration cfg;

    public PlayerJoinAndTracker(JavaPlugin plugin, GemDistributer distributer) {
        this.plugin = plugin;
        this.distributer = distributer;
        this.file = new File(plugin.getDataFolder(), "player.yml");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create player.yml");
            }
        }

        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!((Main) plugin).isWorldEnabled(event.getPlayer().getWorld().getName())) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        if (hasReceivedGem(uuid)) return;

        String gemId;
        try {
            gemId = distributer.getNextGem();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to distribute gem: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "Could not assign a gem.");
            return;
        }

        String display = capitalize(gemId.replace("_gem", "")) + " Gem";
        ChatColor color = getGemColor(gemId);
        int modelData = getGemModelData(gemId);
        ItemStack gem = GemGenerator.createGem(display, color, modelData);

        player.getInventory().addItem(gem);
        player.sendMessage(ChatColor.AQUA + "Welcome! You’ve received a " + display + "!");

        cfg.set(name + ".uuid", uuid.toString());
        cfg.set(name + ".gem", gemId);
        saveConfig();
    }

    private boolean hasReceivedGem(UUID uuid) {
        for (String playerName : cfg.getKeys(false)) {
            String stored = cfg.getString(playerName + ".uuid");
            if (uuid.toString().equals(stored)) {
                return true;
            }
        }
        return false;
    }

    private void saveConfig() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player.yml");
        }
    }

    private String capitalize(String input) {
        return input.isEmpty() ? input : input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private ChatColor getGemColor(String gemId) {
        return switch (gemId) {
            case "air_gem"      -> ChatColor.WHITE;
            case "fire_gem"     -> ChatColor.GOLD;
            case "strength_gem" -> ChatColor.RED;
            case "speed_gem"    -> ChatColor.GREEN;
            case "poison_gem"   -> ChatColor.DARK_PURPLE;
            case "luck_gem"     -> ChatColor.YELLOW;
            case "curse_gem"    -> ChatColor.DARK_RED;
            case "healing_gem"  -> ChatColor.LIGHT_PURPLE;
            default             -> ChatColor.GRAY;
        };
    }

    private int getGemModelData(String gemId) {
        return switch (gemId) {
            case "air_gem"      -> 11001;
            case "fire_gem"     -> 11002;
            case "strength_gem" -> 11003;
            case "speed_gem"    -> 11004;
            case "poison_gem"   -> 11005;
            case "luck_gem"     -> 11006;
            case "curse_gem"    -> 11007;
            case "healing_gem"  -> 11008;
            case "water_gem"    -> 11011;
            case "shock_gem"    -> 11012;
            default             -> 0;
        };
    }
}