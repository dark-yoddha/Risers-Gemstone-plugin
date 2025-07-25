package com.Gemstone.Items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GemGenerator implements Listener {

    public static ItemStack createGem(String gemName, ChatColor color, int modelData) {
        Material material;

        String key = ChatColor.stripColor(gemName.toLowerCase());
        if (key.contains("water")) {
            return createWaterGem(modelData);
        } else if (key.contains("shock")) {
            return createShockGem(modelData);
        } else {
            material = Material.WOODEN_SWORD;
        }

        ItemStack gem = new ItemStack(material);
        ItemMeta meta = gem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + gemName);
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.setUnbreakable(true);
            meta.setCustomModelData(modelData);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            gem.setItemMeta(meta);
        }
        return gem;
    }

    public static ItemStack createWaterGem(int modelData) {
        ItemStack waterGem = new ItemStack(Material.TRIDENT);
        ItemMeta meta = waterGem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.BLUE + "Water Gem");
            meta.addEnchant(Enchantment.RIPTIDE, 3, true);
            meta.addEnchant(Enchantment.SHARPNESS, 3, true);
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.setCustomModelData(modelData);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
            waterGem.setItemMeta(meta);
        }
        return waterGem;
    }

    public static ItemStack createShockGem(int modelData) {
        ItemStack shockGem = new ItemStack(Material.TRIDENT);
        ItemMeta meta = shockGem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Shock Gem");
            meta.addEnchant(Enchantment.SHARPNESS, 3, true);
            meta.addEnchant(Enchantment.LOYALTY, 3, true);
            meta.addEnchant(Enchantment.CHANNELING, 1, true);
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.setCustomModelData(modelData);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
            shockGem.setItemMeta(meta);
        }
        return shockGem;
    }
}