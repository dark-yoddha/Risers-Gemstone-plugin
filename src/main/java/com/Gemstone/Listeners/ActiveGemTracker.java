package com.Gemstone.Listeners;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ActiveGemTracker {
    private static final Map<Player, ItemStack> activeGems = new HashMap();

    public static void setActiveGem(Player player, ItemStack gem) {
        activeGems.put(player, gem);
    }

    public static ItemStack getActiveGem(Player player) {
        return (ItemStack)activeGems.get(player);
    }

    public static void removeActiveGem(Player player) {
        activeGems.remove(player);
    }

    public static boolean hasActiveGem(Player player) {
        return activeGems.containsKey(player);
    }

    public static boolean isActiveGem(Player player, ItemStack gem) {
        ItemStack activeGem = (ItemStack)activeGems.get(player);
        return activeGem != null && activeGem.isSimilar(gem);
    }
}
