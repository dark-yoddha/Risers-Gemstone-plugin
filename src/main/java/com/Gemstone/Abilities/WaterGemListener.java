package com.Gemstone.Abilities;

import com.Gemstone.Listeners.ActiveGemTracker;
import java.util.HashSet;
import java.util.Set;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.EventHandler;

public class WaterGemListener implements Listener {
    private final Set<Player> playersWithWaterEffects = new HashSet<>();
    private final Set<Player> playersWithGemEffects = new HashSet<>();
    private final JavaPlugin plugin;

    public WaterGemListener(JavaPlugin plugin) {
        this.plugin = plugin;

        new BukkitRunnable() {
            public void run() {
                for (Player player : getOnlinePlayers()) {
                    if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) continue;

                    if (isWaterGemActivated(player)) {
                        applyPermanentEffects(player);
                        checkForWaterEffects(player);
                        removeMiningFatigue(player);
                    } else {
                        removeWaterEffects(player);
                        removePermanentEffects(player);
                    }
                }
            }
        }.runTaskTimer(this.plugin, 0L, 10L);
    }

    @EventHandler
    public void onPlayerDealDamageInNether(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) return;
        if (!player.getWorld().getEnvironment().equals(World.Environment.NETHER)) return;
        if (!isWaterGemActivated(player)) return;

        double originalDamage = event.getDamage();
        double reducedDamage = originalDamage / 2.0;
        event.setDamage(reducedDamage);

        // Optional: Notify the player (can be removed if too spammy)
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.DARK_RED + "Water Gem weakened in the Nether! (Damage Halved)"));
    }

    private void applyPermanentEffects(Player player) {
        if (!playersWithGemEffects.contains(player)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, Integer.MAX_VALUE, 0, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 0, true, false));
            playersWithGemEffects.add(player);
        }
    }

    private void removePermanentEffects(Player player) {
        if (playersWithGemEffects.contains(player)) {
            player.removePotionEffect(PotionEffectType.CONDUIT_POWER);
            player.removePotionEffect(PotionEffectType.WATER_BREATHING);
            player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
            playersWithGemEffects.remove(player);
        }
    }

    private void checkForWaterEffects(Player player) {
        boolean isInWater = player.isInWater();
        boolean isInRain = isPlayerInRain(player);

        if ((isInWater || isInRain) && isWaterGemActivated(player)) {
            if (playersWithWaterEffects.contains(player)) return;

            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, true, false));
            playersWithWaterEffects.add(player);
        } else {
            removeWaterEffects(player);
        }
    }

    private void removeWaterEffects(Player player) {
        if (playersWithWaterEffects.contains(player)) {
            player.removePotionEffect(PotionEffectType.STRENGTH);
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            player.removePotionEffect(PotionEffectType.REGENERATION);
            playersWithWaterEffects.remove(player);
        }
    }

    private boolean isWaterGemActivated(Player player) {
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        return activeGem != null && isWaterGem(activeGem) && isHoldingWaterGem(player);
    }

    private boolean isHoldingWaterGem(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        return isWaterGem(mainHand) || isWaterGem(offHand);
    }

    private boolean isWaterGem(ItemStack item) {
        return item != null && item.getType() == Material.TRIDENT &&
                item.hasItemMeta() &&
                ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Water Gem");
    }

    private void removeMiningFatigue(Player player) {
        if (player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        }
    }

    private boolean isPlayerInRain(Player player) {
        World world = player.getWorld();
        if (!world.hasStorm()) return false;

        org.bukkit.block.Biome biome = world.getBiome(player.getLocation());
        return world.hasStorm() &&
                player.getLocation().getBlock().getType() == Material.AIR &&
                biome != org.bukkit.block.Biome.DESERT &&
                biome != org.bukkit.block.Biome.SAVANNA &&
                biome != org.bukkit.block.Biome.SAVANNA_PLATEAU;
    }

    private Set<Player> getOnlinePlayers() {
        return new HashSet<>(Bukkit.getOnlinePlayers());
    }
}