package com.Gemstone.Abilities;

import com.Gemstone.Listeners.ActiveGemTracker;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class HealingGemListener implements Listener {
    private final JavaPlugin plugin;
    private final long cooldownMillis = 60000L;
    private final long buffDurationMillis = 15000L; // 15 seconds for buff

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> boostedUntil = new HashMap<>(); // Still useful for tracking the active buff from your gem

    public HealingGemListener(JavaPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) continue;

                UUID uuid = player.getUniqueId();
                ItemStack main = player.getInventory().getItemInMainHand();
                ItemStack off = player.getInventory().getItemInOffHand();
                ItemStack activeGem = ActiveGemTracker.getActiveGem(player);

                boolean holdingHealingGem = activeGem != null &&
                        ((activeGem.isSimilar(main) && isHealingGem(main)) ||
                                (main.getType() == Material.AIR && activeGem.isSimilar(off) && isHealingGem(off)));

                // Cooldown feedback
                if (cooldowns.containsKey(uuid)) {
                    long timeLeft = cooldownMillis - (now - cooldowns.get(uuid));
                    if (timeLeft > 0) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.RED + "Healing Cooldown: " +
                                        ChatColor.GOLD + timeLeft / 1000 + "s"));
                    } else {
                        cooldowns.remove(uuid);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.GREEN + "Healing Ability Ready!"));
                    }
                }

                // Check if our custom buff has worn off (for message and resistance removal)
                if (boostedUntil.containsKey(uuid) && now > boostedUntil.get(uuid)) {
                    boostedUntil.remove(uuid);
                    // Remove the RESISTANCE effect that was applied with the buff
                    if (player.hasPotionEffect(PotionEffectType.RESISTANCE) && player.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 1) {
                        player.removePotionEffect(PotionEffectType.RESISTANCE);
                    }
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(ChatColor.YELLOW + "Healing Gem buff has worn off."));
                }


                // Permanent Resistance effect while holding the gem in NORMAL world
                if (holdingHealingGem && player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                    if (!player.hasPotionEffect(PotionEffectType.RESISTANCE) ||
                            player.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() < 0 ||
                            player.getPotionEffect(PotionEffectType.RESISTANCE).getDuration() < Integer.MAX_VALUE / 2) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, true, false, true));
                    }
                } else {
                    // Remove permanent resistance if not holding gem or in wrong world
                    if (player.hasPotionEffect(PotionEffectType.RESISTANCE) &&
                            player.getPotionEffect(PotionEffectType.RESISTANCE).getDuration() > Integer.MAX_VALUE / 2) {
                        player.removePotionEffect(PotionEffectType.RESISTANCE);
                    }
                }
            }
        }, 0L, 10L);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);

        boolean rightClickedGem =
                isHealingGem(main) && activeGem != null && main.isSimilar(activeGem) ||
                        main.getType() == Material.AIR && isHealingGem(off) && activeGem != null && off.isSimilar(activeGem);

        if (!rightClickedGem) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid) < cooldownMillis)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.RED + "Healing Gem is on cooldown."));
            return;
        }

        // Apply temporary buff effects using PotionEffect
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int)(buffDurationMillis / 50), 2, true, false, true)); // Level 3 Regeneration
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, (int)(buffDurationMillis / 50), 1, true, false, true)); // Level 2 Resistance
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, (int)(buffDurationMillis / 50), 3, true, false, true)); // +8 hearts

        boostedUntil.put(uuid, now + buffDurationMillis);
        cooldowns.put(uuid, now);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.GREEN + "Healing Gem activated! Regeneration, Resistance, and 8 extra hearts applied."));


        final double HEALING_RADIUS = 5.0; // 5-block radius
        final int REGENERATION_DURATION_TICKS = 10 * 20; // 10 seconds (20 ticks per second)
        final int REGENERATION_AMPLIFIER = 1; // Level 2 (amplifier 1)

        for (Entity nearbyEntity : player.getNearbyEntities(HEALING_RADIUS, HEALING_RADIUS, HEALING_RADIUS)) {
            if (nearbyEntity instanceof Player nearbyPlayer && !nearbyPlayer.equals(player)) {
                nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, REGENERATION_DURATION_TICKS, REGENERATION_AMPLIFIER, true, false, true));
                nearbyPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.GREEN + player.getName() + "'s Healing Gem healed you!"));
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        // Ensure permanent effects are applied/removed on join based on current state
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
            ItemStack main = player.getInventory().getItemInMainHand();
            ItemStack off = player.getInventory().getItemInOffHand();
            boolean holdingHealingGem = activeGem != null &&
                    ((activeGem.isSimilar(main) && isHealingGem(main)) ||
                            (main.getType() == Material.AIR && activeGem.isSimilar(off) && isHealingGem(off)));

            if (!holdingHealingGem || player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                if (player.hasPotionEffect(PotionEffectType.RESISTANCE) &&
                        player.getPotionEffect(PotionEffectType.RESISTANCE).getDuration() > Integer.MAX_VALUE / 2) {
                    player.removePotionEffect(PotionEffectType.RESISTANCE);
                }
                if (player.hasPotionEffect(PotionEffectType.REGENERATION) &&
                        player.getPotionEffect(PotionEffectType.REGENERATION).getDuration() > Integer.MAX_VALUE / 2) {
                    player.removePotionEffect(PotionEffectType.REGENERATION);
                }
            } else {
                // If holding and in normal world, ensure permanent regeneration and resistance are applied
                if (!player.hasPotionEffect(PotionEffectType.RESISTANCE) ||
                        player.getPotionEffect(PotionEffectType.RESISTANCE).getDuration() < Integer.MAX_VALUE / 2) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, true, false, true));
                }
                if (!player.hasPotionEffect(PotionEffectType.REGENERATION) ||
                        player.getPotionEffect(PotionEffectType.REGENERATION).getDuration() < Integer.MAX_VALUE / 2) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, true, false, true));
                }
            }
        }, 20L); // Delay to allow other plugins to load
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        // Apply/remove permanent effects based on the new item held
        // The HEALTH_BOOST effect for the buff is temporary and will expire on its own
        updatePermanentEffects(player);
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        // Apply/remove permanent effects based on the new item in main/off hand
        updatePermanentEffects(player);
    }

    // Helper method to update permanent effects based on currently held gem
    private void updatePermanentEffects(Player player) {
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        boolean holdingHealingGem = activeGem != null &&
                ((activeGem.isSimilar(main) && isHealingGem(main)) ||
                        (main.getType() == Material.AIR && activeGem.isSimilar(off) && isHealingGem(off)));

        if (holdingHealingGem && player.getWorld().getEnvironment() == World.Environment.NORMAL) {
            if (!player.hasPotionEffect(PotionEffectType.RESISTANCE) || player.getPotionEffect(PotionEffectType.RESISTANCE).getDuration() < Integer.MAX_VALUE / 2) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, true, false, true));
            }
            if (!player.hasPotionEffect(PotionEffectType.REGENERATION) || player.getPotionEffect(PotionEffectType.REGENERATION).getDuration() < Integer.MAX_VALUE / 2) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, true, false, true));
            }
        } else {
            if (player.hasPotionEffect(PotionEffectType.RESISTANCE) && player.getPotionEffect(PotionEffectType.RESISTANCE).getDuration() > Integer.MAX_VALUE / 2) {
                player.removePotionEffect(PotionEffectType.RESISTANCE);
            }
            if (player.hasPotionEffect(PotionEffectType.REGENERATION) && player.getPotionEffect(PotionEffectType.REGENERATION).getDuration() > Integer.MAX_VALUE / 2) {
                player.removePotionEffect(PotionEffectType.REGENERATION);
            }
        }
    }


    private boolean isHealingGem(ItemStack item) {
        return item != null &&
                item.getType() == Material.WOODEN_SWORD && // Or whatever your gem material is
                item.hasItemMeta() &&
                ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Healing Gem");
    }
}
