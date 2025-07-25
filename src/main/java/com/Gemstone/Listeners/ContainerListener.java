package com.Gemstone.Listeners;

import com.Gemstone.GemConfig;
import com.Gemstone.Main;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class ContainerListener implements Listener {
    private final JavaPlugin plugin;

    public ContainerListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!(plugin instanceof Main main)) return;
        if (!main.isWorldEnabled(player.getWorld().getName())) return;
        if (!GemConfig.returnOnContainer()) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        InventoryView view = event.getView();
        boolean containerOpen = view.getTopInventory().getType() != InventoryType.PLAYER
                && view.getTopInventory().getType() != InventoryType.CRAFTING;

        if (containerOpen && (isGem(cursor) || isGem(current))) {
            event.setCancelled(true);

            if (isGem(cursor)) {
                event.setCursor(null);
                HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(cursor.clone());

                if (!leftovers.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), cursor.clone());
                    send(player, "Your inventory was full. The gemstone popped out.");
                } else {
                    send(player, "The gemstone slides back into your inventory.");
                }
            } else {
                send(player, "The gemstone slides back into your inventory.");
            }
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (containerOpen && (isGem(hotbarItem) || isGem(current))) {
                event.setCancelled(true);
                send(player, "You cannot swap gemstones with number keys while a container is open.");
            }
        }

        switch (event.getAction()) {
            case DROP_ALL_SLOT, DROP_ONE_SLOT -> {
                if (isGem(current)) {
                    event.setCancelled(true);
                    send(player, "You cannot drop gemstones like this.");
                }
            }
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isGem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        if (!(plugin instanceof Main main)) return;
        if (!main.isWorldEnabled(player.getWorld().getName())) return;
        if (!GemConfig.returnOnContainer()) return;

        InventoryView view = player.getOpenInventory();
        boolean containerOpen = view.getTopInventory() != view.getBottomInventory();

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (containerOpen && isGem(offhand)) {
            event.setCancelled(true);
            send(player, "You cannot open containers with a gemstone in your offhand.");
        }
    }

    private boolean isGem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        return (item.getType() == Material.WOODEN_SWORD || item.getType() == Material.TRIDENT) &&
                switch (name) {
                    case "Strength Gem", "Fire Gem", "Speed Gem", "Spirit Gem",
                         "Luck Gem", "Curse Gem", "Healing Gem", "Air Gem",
                         "Shock Gem", "Water Gem" -> true;
                    default -> false;
                };
    }

    private void send(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + message));
    }
}