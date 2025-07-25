package com.Gemstone.Listeners;

import com.Gemstone.Main;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class GemGuiClickListener implements Listener {
    private final JavaPlugin plugin;

    public GemGuiClickListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();
        String title = event.getView().getTitle();

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        if (inventory != null && ChatColor.stripColor(title).equals("Activate Gem?")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                String clickedName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

                if (clickedName.equals("Confirm Activation")) {
                    if (player.hasMetadata("ClickedGem")) {
                        ItemStack newGem = (ItemStack) player.getMetadata("ClickedGem").get(0).value();
                        String gemName = ChatColor.stripColor(newGem.getItemMeta().getDisplayName());

                        if (ActiveGemTracker.hasActiveGem(player)) {
                            ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
                            player.getInventory().remove(activeGem);
                            player.sendMessage(ChatColor.RED + "Your previous gem has been deactivated.");
                        }

                        ActiveGemTracker.setActiveGem(player, newGem);
                        player.sendMessage(ChatColor.GREEN + gemName + " is now activated!");
                        player.closeInventory();
                        player.removeMetadata("ClickedGem", plugin);
                    }
                } else if (clickedName.equals("Cancel")) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.RED + "Gem activation canceled.");
                }
            }
        }
    }
}