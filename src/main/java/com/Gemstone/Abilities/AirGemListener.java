package com.Gemstone.Abilities;

import java.util.HashSet;
import java.util.UUID;

import com.Gemstone.Main;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class AirGemListener implements Listener {
    private final Plugin plugin;
    private final HashSet<UUID> windChargeCooldowns = new HashSet();

    public AirGemListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) return;
        enchantMace(player);
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        if (event.getCause() == DamageCause.FALL && isHoldingAirGem(player)) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onWindChargeUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!((com.Gemstone.Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        ItemStack item = event.getItem();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        final UUID playerId = player.getUniqueId();
        if (item != null && item.getType() == Material.WIND_CHARGE) {
            if (!this.isAirGem(offHandItem)) {
                return;
            }

            if (this.windChargeCooldowns.contains(playerId)) {
                return;
            }

            this.windChargeCooldowns.add(playerId);
            (new BukkitRunnable() {
                public void run() {
                    player.getInventory().addItem(new ItemStack[]{new ItemStack(Material.WIND_CHARGE)});
                    AirGemListener.this.windChargeCooldowns.remove(playerId);
                }
            }).runTaskLater(this.plugin, 10L);
        }

    }

    private void enchantMace(Player player) {
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack activeGem = com.Gemstone.Listeners.ActiveGemTracker.getActiveGem(player);

        if (activeGem != null && offHandItem != null && activeGem.isSimilar(offHandItem) && isAirGem(offHandItem)) {
            if (isMace(mainHandItem) && !mainHandItem.containsEnchantment(Enchantment.WIND_BURST)) {
                mainHandItem.addUnsafeEnchantment(Enchantment.WIND_BURST, 3);
            }
        }
    }

    private boolean isHoldingAirGem(Player player) {
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        return mainHandItem != null && this.isAirGem(mainHandItem) || offHandItem != null && this.isAirGem(offHandItem);
    }

    private boolean isMace(ItemStack item) {
        return item != null && item.getType() == Material.MACE;
    }

    private boolean isAirGem(ItemStack item) {
        return item != null && item.getType() == Material.WOODEN_SWORD && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(String.valueOf(ChatColor.WHITE) + "Air Gem");
    }
}
