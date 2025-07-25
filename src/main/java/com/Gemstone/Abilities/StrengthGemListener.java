package com.Gemstone.Abilities;

import com.Gemstone.Listeners.ActiveGemTracker;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class StrengthGemListener implements Listener {
    private void applyEffects(Player player, ItemStack heldItem) {
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        if (activeGem != null && activeGem.isSimilar(heldItem) && this.isStrengthGem(activeGem)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, true, false, true));
        }

    }

    private void removeEffects(Player player, ItemStack heldItem) {
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        if (activeGem != null && activeGem.isSimilar(heldItem) && this.isStrengthGem(activeGem)) {
            player.removePotionEffect(PotionEffectType.STRENGTH);
        }

    }

    private void enchantHeldItem(Player player) {
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);

        if (activeGem != null && offHandItem != null && activeGem.isSimilar(offHandItem) && isStrengthGem(offHandItem)) {
            if (isSharpWeapon(mainHandItem) && !mainHandItem.containsEnchantment(Enchantment.SHARPNESS)) {
                mainHandItem.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
            } else if (mainHandItem.getType() == Material.BOW && !mainHandItem.containsEnchantment(Enchantment.POWER)) {
                mainHandItem.addUnsafeEnchantment(Enchantment.POWER, 5);
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!((com.Gemstone.Main) player.getServer().getPluginManager().getPlugin("RisersGemstone"))
                .isWorldEnabled(player.getWorld().getName())) return;
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());
        this.applyEffects(player, newItem);
        this.removeEffects(player, oldItem);
        this.enchantHeldItem(player);
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!((com.Gemstone.Main) player.getServer().getPluginManager().getPlugin("RisersGemstone"))
                .isWorldEnabled(player.getWorld().getName())) return;
        ItemStack mainHandItem = event.getMainHandItem();
        ItemStack offHandItem = event.getOffHandItem();
        this.removeEffects(player, mainHandItem);
        this.removeEffects(player, offHandItem);
        this.applyEffects(player, offHandItem);
        this.applyEffects(player, mainHandItem);
        this.enchantHeldItem(player);
    }

    private boolean isStrengthGem(ItemStack item) {
        return item != null && item.getType() == Material.WOODEN_SWORD && item.hasItemMeta() && ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Strength Gem");
    }

    private boolean isSharpWeapon(ItemStack item) {
        return item != null && switch (item.getType()) {
            case STONE_SWORD, IRON_SWORD, GOLDEN_SWORD,
                 DIAMOND_SWORD, NETHERITE_SWORD,
                 STONE_AXE, IRON_AXE, GOLDEN_AXE,
                 DIAMOND_AXE, NETHERITE_AXE -> true;
            default -> false;
        };
    }
}