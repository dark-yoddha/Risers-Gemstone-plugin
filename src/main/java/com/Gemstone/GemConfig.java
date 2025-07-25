package com.Gemstone;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class GemConfig {
    private static FileConfiguration config;

    public static void init(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
    }

    public static boolean returnOnDrop() {
        return config.getBoolean("gem-settings.return-on-drop", true);
    }

    public static boolean returnOnDeath() {
        return config.getBoolean("gem-settings.return-on-death", true);
    }

    public static boolean returnOnContainer() {
        return config.getBoolean("gem-settings.return-on-container", true);
    }
}