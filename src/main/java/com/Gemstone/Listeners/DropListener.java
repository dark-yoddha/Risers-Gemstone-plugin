package com.Gemstone.Listeners;

import com.Gemstone.Main;
import com.Gemstone.GemConfig;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class DropListener implements Listener {
    private final Main main;

    public DropListener(JavaPlugin plugin) {
        this.main = (Main) plugin;
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!main.isWorldEnabled(player.getWorld().getName())) return;
        if (!GemConfig.returnOnDrop()) return;

        Item droppedEntity = event.getItemDrop();
        ItemStack item = droppedEntity.getItemStack();

        if (isGem(item)) {
            event.setCancelled(true);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.RED + "You cannot drop the gem."));
        }
    }

    private boolean isGem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        return item.getType() == Material.WOODEN_SWORD || item.getType() == Material.TRIDENT;
    }
}