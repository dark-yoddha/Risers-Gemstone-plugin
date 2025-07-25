package com.Gemstone.Abilities;

import com.Gemstone.Listeners.ActiveGemTracker;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
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
    private final long buffDurationMillis = 15000L;

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> boostedUntil = new HashMap<>();
    private final Map<UUID, Double> originalHealthCache = new HashMap<>();

    public HealingGemListener(JavaPlugin plugin) {
        this.plugin = plugin;

        // 🕒 Periodic tasks
        Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) continue;

                UUID uuid = player.getUniqueId();
                AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
                ItemStack main = player.getInventory().getItemInMainHand();
                ItemStack off = player.getInventory().getItemInOffHand();
                ItemStack activeGem = ActiveGemTracker.getActiveGem(player);

                boolean holdingGem = activeGem != null &&
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

                // Expired buff check
                if (boostedUntil.containsKey(uuid)) {
                    if (now > boostedUntil.get(uuid)) {
                        resetHealth(player);
                        boostedUntil.remove(uuid);
                        originalHealthCache.remove(uuid);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.YELLOW + "Healing Gem effect has worn off. Extra hearts removed."));
                        continue;
                    }

                    // Buff active, check if gem is held
                    if (holdingGem) {
                        if (attr != null && attr.getBaseValue() == originalHealthCache.getOrDefault(uuid, 20.0)) {
                            attr.setBaseValue(attr.getBaseValue() + 8.0);
                            player.setHealth(Math.min(player.getHealth() + 8.0, attr.getValue()));
                        }
                    } else {
                        resetHealth(player);
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

        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, true, false, true));
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            double base = attr.getBaseValue();
            originalHealthCache.put(uuid, base);
        }

        boostedUntil.put(uuid, now + buffDurationMillis);
        cooldowns.put(uuid, now);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.GREEN + "Healing Gem activated! Regeneration and 2 extra hearts applied."));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        UUID uuid = player.getUniqueId();
        resetHealth(player);
        boostedUntil.remove(uuid);
        originalHealthCache.remove(uuid);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());
        applyEffects(player, newItem);
        removeEffects(player, oldItem);
        resetHealth(event.getPlayer());
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        removeEffects(player, mainHandItem);
        removeEffects(player, offHandItem);
        applyEffects(player, offHandItem);
        applyEffects(player, mainHandItem);
        resetHealth(player);
    }

    private void resetHealth(Player player) {
        UUID uuid = player.getUniqueId();
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        double originalBase = originalHealthCache.getOrDefault(uuid, 20.0);
        if (attr != null && attr.getBaseValue() > originalBase) {
            attr.setBaseValue(originalBase);
            if (player.getHealth() > originalBase) {
                player.setHealth(originalBase);
            }
        }
    }

    private void applyEffects(Player player, ItemStack heldItem) {
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        if (activeGem != null && heldItem != null && activeGem.isSimilar(heldItem) && isHealingGem(heldItem)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, true, false, true));
        }
    }

    private void removeEffects(Player player, ItemStack heldItem) {
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        if (activeGem != null && heldItem != null && activeGem.isSimilar(heldItem) && isHealingGem(heldItem)) {
            player.removePotionEffect(PotionEffectType.REGENERATION);
        }
    }

    private boolean isHealingGem(ItemStack item) {
        return item != null &&
                item.getType() == Material.WOODEN_SWORD &&
                item.hasItemMeta() &&
                ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Healing Gem");
    }
}