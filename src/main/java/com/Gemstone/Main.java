package com.Gemstone;

import com.Gemstone.Abilities.*;
import com.Gemstone.Commands.GemstoneCommand;
import com.Gemstone.Commands.GemstoneTabCompleter;
import com.Gemstone.Items.GemGenerator;
import com.Gemstone.Listeners.*;
import com.Gemstone.GemConfig;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private GemDistributer distributer;

    @Override
    public void onEnable() {
        getLogger().info("════════════════════════════════════════════════════════════");
        getLogger().info("💎  RisersGemstone v1.0 loaded successfully");
        getLogger().info("🔮  Created by Dark_Yoddha — https://github.com/dark_yoddha");
        getLogger().info("🗺️  Elemental gem behavior enabled for your server");
        getLogger().info("════════════════════════════════════════════════════════════");

        // Create plugin folder and config
        saveDefaultConfig();

        // Initialize GemConfig toggle system
        GemConfig.init(this);

        // Initialize gem distribution
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

        // Register commands
        getCommand("gemstone").setExecutor(new GemstoneCommand());
        getCommand("gemstone").setTabCompleter(new GemstoneTabCompleter());

        // Core listeners
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

    public GemDistributer getGemDistributer() {
        return distributer;
    }

    public boolean isWorldEnabled(String worldName) {
        return getConfig().getStringList("enabled-worlds").contains(worldName);
    }
}