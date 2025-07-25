package com.Gemstone.Listeners;

import com.Gemstone.Main;
import com.Gemstone.GemConfig;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class DeathListener implements Listener {
    private final Main main;
    private final Map<UUID, List<ItemStack>> gemsToRestore = new HashMap<>();

    public DeathListener(JavaPlugin plugin) {
        this.main = (Main) plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!main.isWorldEnabled(player.getWorld().getName())) return;
        if (!GemConfig.returnOnDeath()) return;

        List<ItemStack> retainedGems = new ArrayList<>();

        event.getDrops().removeIf(item -> {
            if (!isGem(item)) return false;
            String stripped = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            switch (stripped) {
                case "Strength Gem", "Fire Gem", "Speed Gem", "Spirit Gem",
                     "Luck Gem", "Curse Gem", "Healing Gem", "Air Gem",
                     "Shock Gem", "Water Gem" -> {
                    retainedGems.add(item);
                    return true;
                }
                default -> { return false; }
            }
        });

        if (!retainedGems.isEmpty()) {
            gemsToRestore.put(player.getUniqueId(), retainedGems);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!main.isWorldEnabled(player.getWorld().getName())) return;
        if (!GemConfig.returnOnDeath()) return;

        List<ItemStack> gems = gemsToRestore.remove(player.getUniqueId());
        if (gems != null) {
            for (ItemStack gem : gems) {
                player.getInventory().addItem(gem);
            }
        }
    }

    private boolean isGem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        return item.getType() == Material.WOODEN_SWORD || item.getType() == Material.TRIDENT;
    }
}