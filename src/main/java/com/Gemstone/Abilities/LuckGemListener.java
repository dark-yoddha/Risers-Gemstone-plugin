package com.Gemstone.Abilities;

import com.Gemstone.Main;
import com.Gemstone.Listeners.ActiveGemTracker;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class LuckGemListener implements Listener {
    private final Plugin plugin;
    private final Random random = new Random();

    public LuckGemListener(Plugin plugin) {
        this.plugin = plugin;
    }

    private void applyLuckEffects(Player player, ItemStack heldItem) {
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        if (isLuckGem(heldItem) && activeGem != null && activeGem.isSimilar(heldItem)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, Integer.MAX_VALUE, 1, true, false, true));
        }
    }

    private void removeLuckEffects(Player player, ItemStack heldItem) {
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        if (isLuckGem(heldItem) && activeGem != null && activeGem.isSimilar(heldItem)) {
            player.removePotionEffect(PotionEffectType.LUCK);
        }
    }

    private void enchantHeldItem(Player player) {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);

        if (activeGem != null && isLuckGem(offHand) && activeGem.isSimilar(offHand)) {
            if (isSword(mainHand) && !mainHand.containsEnchantment(Enchantment.LOOTING)) {
                mainHand.addUnsafeEnchantment(Enchantment.LOOTING, 5);
            } else if (isPickaxe(mainHand) && !mainHand.containsEnchantment(Enchantment.FORTUNE)) {
                mainHand.addUnsafeEnchantment(Enchantment.FORTUNE, 5);
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());

        applyLuckEffects(player, newItem);
        removeLuckEffects(player, oldItem);
        enchantHeldItem(player);
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        removeLuckEffects(player, main);
        removeLuckEffects(player, off);

        applyLuckEffects(player, off);
        applyLuckEffects(player, main);
        enchantHeldItem(player);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        boolean holdingLuckGem = (isLuckGem(main) && main.isSimilar(activeGem)) ||
                (isLuckGem(off) && off.isSimilar(activeGem));

        if (!holdingLuckGem) return;

        // 33% chance for double damage
        if (random.nextDouble() < 0.33) {
            double originalDamage = event.getDamage();
            event.setDamage(originalDamage * 2);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.GREEN + "Lucky strike! You dealt double damage!"));
        }
    }

    private boolean isLuckGem(ItemStack item) {
        return item != null &&
                item.getType() == Material.WOODEN_SWORD &&
                item.hasItemMeta() &&
                ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Luck Gem");
    }

    private boolean isPickaxe(ItemStack item) {
        return item != null && switch (item.getType()) {
            case STONE_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE,
                 DIAMOND_PICKAXE, NETHERITE_PICKAXE -> true;
            default -> false;
        };
    }

    private boolean isSword(ItemStack item) {
        return item != null && switch (item.getType()) {
            case STONE_SWORD, IRON_SWORD, GOLDEN_SWORD,
                 DIAMOND_SWORD, NETHERITE_SWORD -> true;
            default -> false;
        };
    }
}