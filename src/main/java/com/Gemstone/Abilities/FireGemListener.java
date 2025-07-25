package com.Gemstone.Abilities;

import com.Gemstone.Main;
import com.Gemstone.Listeners.ActiveGemTracker;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
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

public class FireGemListener implements Listener {
    private final Plugin plugin;

    public FireGemListener(Plugin plugin) {
        this.plugin = plugin;

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) continue;

                if (!isUsingFireGem(player)) continue;

                if (player.getFireTicks() > 0) {
                    applyFireEffects(player);
                } else {
                    removeFireEffects(player);
                }
            }
        }, 0L, 20L); // Every 1 second
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());
        applyPassiveFireResistance(player, newItem);
        removePassiveFireResistance(player, oldItem);
        this.enchantHeldItem(player);
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        removePassiveFireResistance(player, mainHand);
        removePassiveFireResistance(player, offHand);
        applyPassiveFireResistance(player, offHand);
        applyPassiveFireResistance(player, mainHand);
        this.enchantHeldItem(player);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!((Main) plugin).isWorldEnabled(attacker.getWorld().getName())) return;

        if (!isUsingFireGem(attacker)) return;

        boolean isInWater = attacker.isInWater();
        boolean isRainingOrThundering = isPlayerInRainOrThunder(attacker);

        if (isInWater || isRainingOrThundering) {
            double originalDamage = event.getDamage();
            event.setDamage(originalDamage / 1.5);
            attacker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.RED + "Water/Storm Weakness: Damage halved!"));
        } else if (attacker.getWorld().getEnvironment() == Environment.NETHER) {
            double originalDamage = event.getDamage();
            event.setDamage(originalDamage * 1.5);
            attacker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.RED + "Nether Power: Your damage was doubled!"));
        }
    }

    private void enchantHeldItem(Player player) {
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack activeGem = com.Gemstone.Listeners.ActiveGemTracker.getActiveGem(player);

        if (activeGem != null && offHandItem != null && activeGem.isSimilar(offHandItem) && isFireGem(offHandItem)) {
            if (isSword(mainHandItem) && !mainHandItem.containsEnchantment(Enchantment.FIRE_ASPECT)) {
                mainHandItem.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
            }
        }
    }

    private boolean isPlayerInRainOrThunder(Player player) {
        World world = player.getWorld();
        if (!world.hasStorm()) return false;

        // Check biome to avoid no-rain biomes
        org.bukkit.block.Biome biome = world.getBiome(player.getLocation());
        return player.getLocation().getBlock().getType() == Material.AIR &&
                biome != org.bukkit.block.Biome.DESERT &&
                biome != org.bukkit.block.Biome.SAVANNA &&
                biome != org.bukkit.block.Biome.SAVANNA_PLATEAU;
    }

    private boolean isFireGem(ItemStack item) {
        return item != null &&
                item.getType() == Material.WOODEN_SWORD &&
                item.hasItemMeta() &&
                ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Fire Gem");
    }

    private boolean isUsingFireGem(Player player) {
        if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) return false;

        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        return activeGem != null &&
                ((activeGem.isSimilar(main) && isFireGem(main)) || (activeGem.isSimilar(off) && isFireGem(off)));
    }

    private void applyFireEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, true, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, true, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, true, false, true));
    }

    private void removeFireEffects(Player player) {
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.STRENGTH);
    }

    private void applyPassiveFireResistance(Player player, ItemStack item) {
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        if (activeGem != null && item != null && activeGem.isSimilar(item) && isFireGem(item)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false, true));
        }
    }

    private void removePassiveFireResistance(Player player, ItemStack item) {
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        if (activeGem != null && item != null && activeGem.isSimilar(item) && isFireGem(item)) {
            player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        }
    }

    private boolean isSword(ItemStack item) {
        return item != null && switch (item.getType()) {
            case STONE_SWORD, IRON_SWORD, GOLDEN_SWORD,
                 DIAMOND_SWORD, NETHERITE_SWORD -> true;
            default -> false;
        };
    }
}