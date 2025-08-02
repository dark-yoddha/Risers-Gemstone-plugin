package com.Gemstone;

import com.Gemstone.Abilities.*;
import com.Gemstone.Commands.GemstoneCommand;
import com.Gemstone.Commands.GemstoneTabCompleter;
import com.Gemstone.Items.GemGenerator;
import com.Gemstone.Listeners.*;

import org.bukkit.event.EventHandler; // Import for event handler
import org.bukkit.event.Listener;     // Import for Listener interface
import org.bukkit.event.player.PlayerChangedWorldEvent; // Import for world change event
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect; // Import for PotionEffect
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List; // Ensure this is imported

public class Main extends JavaPlugin implements Listener { // Implement Listener interface
    private GemDistributer distributer;
    private static Main instance;
    private List<String> enabledWorlds; // Use java.util.List

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("════════════════════════════════════════════════════════════");
        getLogger().info("💎  RisersGemstone v1.0 loaded successfully");
        getLogger().info("🔮  Created by Dark_Yoddha — https://github.com/dark_yoddha");
        getLogger().info("🗺️  Elemental gem behavior enabled for your server");
        getLogger().info("════════════════════════════════════════════════════════════");

        saveDefaultConfig();
        loadConfigData();
        try {
            distributer = new GemDistributer(this);
        } catch (Exception e) {
            getLogger().severe("Failed to initialize GemDistributer: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register Gem Generator
        GemGenerator generator = new GemGenerator();
        getServer().getPluginManager().registerEvents(generator, this);

        // Register join + tracker
        getServer().getPluginManager().registerEvents(new PlayerJoinAndTracker(this, distributer), this);
        getServer().getPluginManager().registerEvents(this, this); // Register Main class as a listener

        // Register commands
        getCommand("gemstone").setExecutor(new GemstoneCommand(this));
        getCommand("gemstone").setTabCompleter(new GemstoneTabCompleter());

        registerEvents(
                new DeathListener(this),
                new DropListener(this),
                new ContainerListener(this),
                new GemGuiClickListener(this),
                new GemActivationListener(this)
        );

        // Ability listeners
        registerEvents(
                new SpeedGemListener(this),
                new StrengthGemListener(),
                new LuckGemListener(this),
                new FireGemListener(this),
                new SpiritGemListener(this),
                new CurseGemListener(this),
                new HealingGemListener(this),
                new ShockGemListener(this),
                new WaterGemListener(this),
                new AirGemListener(this)
        );
    }

    @Override
    public void onDisable() {
        getLogger().info("RisersGemstone has been disabled!");
        if (distributer != null) distributer.close();
    }

    private void registerEvents(org.bukkit.event.Listener... listeners) {
        for (org.bukkit.event.Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    public void loadConfigData() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getLogger().info("config.yml not found. Generating default config.yml...");
            saveDefaultConfig();
        }
        reloadConfig();

        this.enabledWorlds = getConfig().getStringList("enabled-worlds");
        getLogger().info("Config Reloaded: Worlds=" + enabledWorlds);
    }

    public static Main getInstance() {
        return instance;
    }

    public GemDistributer getGemDistributer() {
        return distributer;
    }

    public boolean isWorldEnabled(String worldName) {
        return this.enabledWorlds.contains(worldName);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String newWorldName = player.getWorld().getName();

        // If the new world is NOT enabled for the gemstone plugin, remove effects
        if (!isWorldEnabled(newWorldName)) {
            getLogger().info("Player " + player.getName() + " entered a disabled world (" + newWorldName + "). Removing gemstone effects.");

            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
        }
    }
}
