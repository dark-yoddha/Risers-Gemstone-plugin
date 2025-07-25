package com.Gemstone.Listeners;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

import com.Gemstone.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class GemActivationListener implements Listener {
    private final Plugin plugin;
    private final HashMap<UUID, Integer> clickCounters = new HashMap();
    private final HashMap<UUID, Long> clickTimers = new HashMap();
    private final long timeLimit = 1500L;

    private void purgeDuplicateGems(Player player, ItemStack activeGem) {
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];

            if (item == null || item.getType() == Material.AIR) continue;

            boolean sameMaterial = item.getType() == Material.WOODEN_SWORD || item.getType() == Material.TRIDENT;
            boolean isGem = item.hasItemMeta()
                    && item.getItemMeta().hasEnchant(Enchantment.LUCK_OF_THE_SEA)
                    && item.getItemMeta().getEnchantLevel(Enchantment.LUCK_OF_THE_SEA) == 1;

            boolean isSameAsActive = item.isSimilar(activeGem);

            // Delete gem if it's not the one we're activating
            if (sameMaterial && isGem && !isSameAsActive) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    public GemActivationListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        ItemStack itemInMainHand = event.getItem();
        ItemStack itemInOffHand = player.getInventory().getItemInOffHand();
        if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) return;
        if (itemInOffHand == null || !this.isGem(itemInOffHand)) {
            if (itemInMainHand != null && this.isGem(itemInMainHand) && player.isSneaking() && event.getAction().toString().contains("RIGHT_CLICK")) {
                if (!this.clickCounters.containsKey(playerId)) {
                    this.clickCounters.put(playerId, 1);
                    this.clickTimers.put(playerId, System.currentTimeMillis());
                    this.startClickTimer(player, itemInMainHand);
                } else {
                    int currentClicks = (Integer)this.clickCounters.get(playerId);
                    this.clickCounters.put(playerId, currentClicks + 1);
                    if ((Integer)this.clickCounters.get(playerId) == 5) {
                        long startTime = (Long)this.clickTimers.get(playerId);
                        long timeElapsed = System.currentTimeMillis() - startTime;
                        if (timeElapsed <= 1500L) {
                            ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
                            if (activeGem != null && activeGem.isSimilar(itemInMainHand)) {
                                player.sendMessage(String.valueOf(ChatColor.YELLOW) + "This gem is already activated!");
                                this.clickCounters.remove(playerId);
                                this.clickTimers.remove(playerId);
                                return;
                            }

                            this.clickCounters.remove(playerId);
                            this.clickTimers.remove(playerId);
                            this.openActivationGUI(player, itemInMainHand);
                        }
                    }
                }
            }

        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 🌍 Only proceed if plugin is enabled in this world
        if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && clickedItem.getType() == Material.GREEN_CONCRETE) {
            ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
            if (activeGem != null) {
                purgeDuplicateGems(player, activeGem); // ✅ remove other gems
            }
        }
    }

    private void sendActivationMessage(Player player, ItemStack gem) {
        String message = this.getActivationMessage(gem);
        ChatColor color = this.getGemColor(gem);
        if (message != null) {
            String var10001 = String.valueOf(color);
            player.sendMessage(var10001 + message);
        }

    }

    private String getActivationMessage(ItemStack gem) {
        if (gem.hasItemMeta() && gem.getItemMeta().getDisplayName() != null) {
            String var10000;
            switch (ChatColor.stripColor(gem.getItemMeta().getDisplayName())) {
                case "Air Gem" -> var10000 = "Flow towards your consciousness.";
                case "Fire Gem" -> var10000 = "Fierce is the nature of a warrior.";
                case "Speed Gem" -> var10000 = "Faster than everyone.";
                case "Strength Gem" -> var10000 = "Being the strongest depends on you above all.";
                case "Healing Gem" -> var10000 = "Get safe and well as soon as possible.";
                case "Luck Gem" -> var10000 = "Somewhat, makes you come at forwards.";
                case "Poison Gem" -> var10000 = "Even this thing when divided happily turns into nectar.";
                case "Curse Gem" -> var10000 = "Exactly opposite to Pure but maybe better than what is pure!";
                case "Shock Gem" -> var10000 = "Too much shocks makes you smarter.";
                case "Water Gem" -> var10000 = "Can protect as well as destroy!";
                default -> var10000 = "This gem seems inert...";
            }

            return var10000;
        } else {
            return null;
        }
    }

    private ChatColor getGemColor(ItemStack gem) {
        if (gem.hasItemMeta() && gem.getItemMeta().getDisplayName() != null) {
            ChatColor var10000;
            switch (ChatColor.stripColor(gem.getItemMeta().getDisplayName())) {
                case "Air Gem" -> var10000 = ChatColor.WHITE;
                case "Fire Gem" -> var10000 = ChatColor.GOLD;
                case "Speed Gem" -> var10000 = ChatColor.GREEN;
                case "Strength Gem" -> var10000 = ChatColor.RED;
                case "Healing Gem" -> var10000 = ChatColor.LIGHT_PURPLE;
                case "Luck Gem" -> var10000 = ChatColor.YELLOW;
                case "Poison Gem" -> var10000 = ChatColor.DARK_PURPLE;
                case "Curse Gem" -> var10000 = ChatColor.DARK_RED;
                case "Shock Gem" -> var10000 = ChatColor.AQUA;
                case "Water Gem" -> var10000 = ChatColor.BLUE;
                default -> var10000 = ChatColor.GRAY;
            }

            return var10000;
        } else {
            return ChatColor.GRAY;
        }
    }

    private boolean isGem(ItemStack item) {
        return item != null && (item.getType() == Material.WOODEN_SWORD || item.getType() == Material.TRIDENT) && item.hasItemMeta() && item.getItemMeta().hasDisplayName();
    }

    private void startClickTimer(final Player player, ItemStack gem) {
        (new BukkitRunnable() {
            public void run() {
                UUID playerId = player.getUniqueId();
                if (GemActivationListener.this.clickTimers.containsKey(playerId)) {
                    long startTime = (Long)GemActivationListener.this.clickTimers.get(playerId);
                    long timeElapsed = System.currentTimeMillis() - startTime;
                    if (timeElapsed > 1500L) {
                        GemActivationListener.this.clickCounters.remove(playerId);
                        GemActivationListener.this.clickTimers.remove(playerId);
                        this.cancel();
                    }
                } else {
                    this.cancel();
                }

            }
        }).runTaskTimer(this.plugin, 0L, 20L);
    }

    private void openActivationGUI(Player player, ItemStack gem) {
        Inventory activationGUI = Bukkit.createInventory((InventoryHolder)null, 9, String.valueOf(ChatColor.AQUA) + "Activate Gem?");
        ItemStack confirmItem = this.createGuiItem(Material.GREEN_CONCRETE, String.valueOf(ChatColor.GREEN) + "Confirm Activation");
        activationGUI.setItem(3, confirmItem);
        ItemStack cancelItem = this.createGuiItem(Material.RED_CONCRETE, String.valueOf(ChatColor.RED) + "Cancel");
        activationGUI.setItem(5, cancelItem);
        player.setMetadata("ClickedGem", new FixedMetadataValue(this.plugin, gem));
        player.openInventory(activationGUI);
        Logger var10000 = Bukkit.getLogger();
        String var10001 = player.getName();
        var10000.info(var10001 + " opened the activation GUI for gem: " + gem.getItemMeta().getDisplayName());
    }

    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }

        return item;
    }
}