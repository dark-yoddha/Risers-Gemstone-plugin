package com.Gemstone.Abilities;

import java.util.HashMap;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CurseGemListener implements Listener {
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();
    private final long cooldownDuration = 60000L; // 60 seconds
    private final JavaPlugin plugin;

    public CurseGemListener(JavaPlugin plugin) {
        this.plugin = plugin;

        // Cooldown status display (action bar message)
        Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                if (cooldowns.containsKey(uuid)) {
                    long lastUsed = cooldowns.get(uuid);
                    long timeElapsed = System.currentTimeMillis() - lastUsed;
                    long cooldownLeft = cooldownDuration - timeElapsed;

                    if (cooldownLeft > 0L) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.RED + "Ability Cooldown: " + ChatColor.GOLD + cooldownLeft / 1000L + "s"));
                    } else {
                        cooldowns.remove(uuid);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.GREEN + "Ability Ready!"));
                    }
                }
            }
        }, 0L, 10L); // Run every 0.5 seconds (10 ticks)
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!((com.Gemstone.Main) plugin).isWorldEnabled(attacker.getWorld().getName())) return;

        ItemStack mainHandItem = attacker.getInventory().getItemInMainHand();
        ItemStack offHandItem = attacker.getInventory().getItemInOffHand();

        boolean holdingCurseGem = isCurseGem(mainHandItem) || isCurseGem(offHandItem);
        if (!holdingCurseGem) return;

        UUID attackerUUID = attacker.getUniqueId();
        Environment environment = attacker.getWorld().getEnvironment();

        if (environment == Environment.NETHER) {
            // Double damage in the Nether
            double originalDamage = event.getDamage();
            event.setDamage(originalDamage * 1.5);
            attacker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.RED + "Nether Power: Your damage was increased!"));
        } else if (environment == Environment.NORMAL) {
            // Halve damage in the Overworld
            double originalDamage = event.getDamage();
            event.setDamage(originalDamage / 1.5);
            attacker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.RED + "Overworld Weakness: Your damage was reduced."));
        }

        // Check cooldown before applying curse
        if (cooldowns.containsKey(attackerUUID)) {
            long lastUsed = cooldowns.get(attackerUUID);
            if (System.currentTimeMillis() - lastUsed < cooldownDuration) return;
        }

        if (event.getEntity() instanceof LivingEntity target) {
            // Apply visual Wither effect only
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0));
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));

            // Deal 1 heart (2 HP) true damage every second for 5 seconds
            int ticks = 5;
            double damagePerTick = 2.0;

            Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                int count = 0;

                @Override
                public void run() {
                    if (count >= ticks || target.isDead()) return;

                    if (target instanceof Player playerTarget && !playerTarget.isOnline()) return;

                    target.damage(damagePerTick, attacker); // true, unblockable damage
                    count++;
                }
            }, 0L, 20L); // start immediately, repeat every 20 ticks (1 second)

            // Action bar messages
            attacker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.DARK_PURPLE + "You have cursed your opponent!"));

            if (target instanceof Player victim) {
                victim.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.DARK_PURPLE + "You have been cursed by the Curse Gem!"));
            }

            // Apply cooldown
            cooldowns.put(attackerUUID, System.currentTimeMillis());
        }
    }

    private boolean isCurseGem(ItemStack item) {
        return item != null &&
                item.getType() == Material.WOODEN_SWORD &&
                item.hasItemMeta() &&
                ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Curse Gem");
    }
}