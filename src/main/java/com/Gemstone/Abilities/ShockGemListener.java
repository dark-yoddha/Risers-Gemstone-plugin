package com.Gemstone.Abilities;

import com.Gemstone.Listeners.ActiveGemTracker;
import java.util.HashMap;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ShockGemListener implements Listener {
    private final HashMap<UUID, Long> lightningCooldowns = new HashMap<>();
    private final long cooldownDuration = 60000L;
    private final JavaPlugin plugin;
    private final HashMap<UUID, Long> lastLightningApplied = new HashMap<>();
    private final long lightningCooldown = 1000L;

    // Add a temporary flag to prevent re-triggering the event for lightning damage
    private boolean dealingLightningDamage = false;

    public ShockGemListener(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) continue;

                UUID uuid = player.getUniqueId();
                if (lightningCooldowns.containsKey(uuid)) {
                    long lastUsed = lightningCooldowns.get(uuid);
                    long timeElapsed = System.currentTimeMillis() - lastUsed;
                    long cooldownLeft = cooldownDuration - timeElapsed;
                    if (cooldownLeft > 0L) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.RED + "Ability Cooldown: " + ChatColor.GOLD + cooldownLeft / 1000L + "s"));
                    } else {
                        lightningCooldowns.remove(uuid);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.GREEN + "Shock Ability Ready!"));
                    }
                }
            }
        }, 0L, 10L);
    }

    @EventHandler
    public void onPlayerDamagedByLightning(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == DamageCause.LIGHTNING) {
                ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
                if (activeGem != null &&
                        activeGem.getType() == Material.TRIDENT &&
                        ChatColor.stripColor(activeGem.getItemMeta().getDisplayName()).equals("Shock Gem")) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                }
            }
        }
    }

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
        // Prevent recursive calls when lightning damage is being applied by the plugin itself
        if (dealingLightningDamage) {
            return;
        }

        if (!(event.getDamager() instanceof Player player)) return;
        if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        // Ensure this is a direct melee attack, not from other sources
        // This line is already good, keep it:
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;


        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand != null &&
                itemInHand.getType() == Material.TRIDENT &&
                ChatColor.stripColor(itemInHand.getItemMeta().getDisplayName()).equals("Shock Gem")) {

            Entity target = event.getEntity();

            // Prevent applying lightning if it's already been applied to the target recently.
            // Make sure we're not processing the same entity multiple times.
            if (target != null && !hasLightningBeenAppliedRecently(target)) {
                // Set the flag before calling the ability
                dealingLightningDamage = true;
                try {
                    applyLightningAbility(player, target);
                } finally {
                    // Always reset the flag, even if an error occurs
                    dealingLightningDamage = false;
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player player)) return;
        if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        Entity target = event.getHitEntity();
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);

        if (activeGem != null &&
                activeGem.getType() == Material.TRIDENT &&
                ChatColor.stripColor(activeGem.getItemMeta().getDisplayName()).equals("Shock Gem") &&
                target != null) {

            // Prevent applying lightning if it's already been applied to the target recently.
            if (!hasLightningBeenAppliedRecently(target)) {
                applyLightningAbility(player, target);
            }
        }
    }

    private boolean hasLightningBeenAppliedRecently(Entity target) {
        UUID targetId = target.getUniqueId();
        long now = System.currentTimeMillis();
        if (lastLightningApplied.containsKey(targetId)) {
            long lastApplied = lastLightningApplied.get(targetId);
            return (now - lastApplied) < lightningCooldown;
        }
        return false;
    }

    private void applyLightningAbility(Player player, Entity target) {
        if (target == null || !target.isValid()) return; // Safety first

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Check if cooldown exists and if it's still valid
        if (lightningCooldowns.containsKey(playerId)) {
            long lastUsed = lightningCooldowns.get(playerId);
            if (now - lastUsed < cooldownDuration) return; // Prevent triggering if cooldown is active
        }

        // Strike lightning effect only once
        target.getWorld().strikeLightningEffect(target.getLocation());

        // Damage living entities only
        if (target instanceof org.bukkit.entity.LivingEntity livingTarget && !livingTarget.isDead()) {
            livingTarget.damage(8.0, player); // 4 hearts damage
        }

        // Buff player
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0));

        player.sendMessage(ChatColor.YELLOW + "You summoned lightning on " + target.getName() + "!");

        // Update cooldowns
        lightningCooldowns.put(playerId, now);
        lastLightningApplied.put(target.getUniqueId(), now); // Update the last time lightning was applied to this entity
    }
}