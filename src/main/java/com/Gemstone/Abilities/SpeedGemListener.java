package com.Gemstone.Abilities;

import com.Gemstone.Main;
import com.Gemstone.Listeners.ActiveGemTracker;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class SpeedGemListener implements Listener {
    private final Plugin plugin;

    // 🕒 Single cooldown timer per player
    private final Map<UUID, Long> blinkTimer = new HashMap<>();

    // ⚡ Configuration
    private final long cooldownPerCharge = 60000L; // 60 sec
    private final long cooldownCap = 180000L; // 3 charges max (3 min)

    public SpeedGemListener(Plugin plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {

                if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) continue;

                // ✅ Check if player has Speed Gem active
                ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
                if (activeGem == null || !isSpeedGem(activeGem)) continue;

                UUID uuid = player.getUniqueId();
                BlinkData data = blinkDataMap.getOrDefault(uuid, new BlinkData());
                long now = System.currentTimeMillis();
                long timeLeft = Math.max(0L, data.timerEnd - now);

                // 🔁 Determine current zone
                int newZone = getZoneFromTimeLeft(timeLeft);
                int allowed = getAllowedUsesForZone(newZone);

                // ✅ Reset usage count if zone changed
                if (newZone != data.currentZone) {
                    data.currentZone = newZone;
                    data.usesInCurrentZone = 0;
                }

                int usesLeft = Math.max(0, allowed - data.usesInCurrentZone);
                long seconds = timeLeft / 1000L;

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.YELLOW + "Blink Time: " +
                                ChatColor.GOLD + seconds + "s" +
                                ChatColor.GRAY + " | Uses Left: " +
                                ChatColor.AQUA + usesLeft));

                blinkDataMap.put(uuid, data);
            }
        }, 0L, 20L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());

        applyEffects(player, newItem);
        removeEffects(player, oldItem);
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        ItemStack main = event.getMainHandItem();
        ItemStack off = event.getOffHandItem();

        removeEffects(player, main);
        removeEffects(player, off);
        applyEffects(player, off);
        applyEffects(player, main);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!((Main) plugin).isWorldEnabled(player.getWorld().getName())) return;

        ItemStack item = event.getItem();
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        if (activeGem == null || item == null || !activeGem.isSimilar(item) || !isSpeedGem(item)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        BlinkData data = blinkDataMap.getOrDefault(uuid, new BlinkData());
        if (data.timerEnd < now) {
            data.timerEnd = now; // reset expired timer
            data.usesInCurrentZone = 0;
            data.currentZone = 3; // default to "no-use" until we calculate properly
        }

        long timeLeft = Math.max(0, data.timerEnd - now);
        int currentZone = getZoneFromTimeLeft(timeLeft);

        // If zone has changed, reset usage counter
        if (currentZone != data.currentZone) {
            data.usesInCurrentZone = 0;
            data.currentZone = currentZone;
        }

        int allowedUses = getAllowedUsesForZone(currentZone);
        if (data.usesInCurrentZone >= allowedUses) {
            player.sendMessage(ChatColor.RED + "You cannot use Blink in this time range!");
            return;
        }

        Location blinkTarget = getBlinkTarget(player);
        if (blinkTarget == null) {
            player.sendMessage(ChatColor.RED + "No safe spot to blink!");
            return;
        }

        // ✨ Do the blink
        player.teleport(blinkTarget);
        player.playSound(blinkTarget, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.spawnParticle(Particle.PORTAL, blinkTarget, 30, 0.5, 0.5, 0.5, 0.1);
        player.sendMessage(ChatColor.AQUA + "You blinked!");

        // ⏱️ Add +60s, but cap at now + 180s
        long newEnd = Math.min(data.timerEnd + 60000L, now + 180000L);
        data.timerEnd = newEnd;
        data.usesInCurrentZone++;
        blinkDataMap.put(uuid, data);
    }

    private static class BlinkData {
        long timerEnd;
        int usesInCurrentZone;
        int currentZone;
    }
    private final Map<UUID, BlinkData> blinkDataMap = new HashMap<>();

    private int getZoneFromTimeLeft(long timeLeft) {
        if (timeLeft == 0) return 0;            // Zone 0 (3 uses)
        else if (timeLeft <= 60000L) return 1;  // Zone 1 (2 uses)
        else if (timeLeft <= 120000L) return 2; // Zone 2 (1 use)
        else return 3;                          // Zone 3 (no use)
    }

    private int getAllowedUsesForZone(int zone) {
        return switch (zone) {
            case 0 -> 3;
            case 1 -> 2;
            case 2 -> 1;
            default -> 0;
        };
    }

    private Location getBlinkTarget(Player player) {
        Location start = player.getLocation().add(0, 1, 0); // eye-level
        Vector direction = player.getLocation().getDirection().normalize();

        for (int i = 1; i <= 10; i++) {
            Location step = start.clone().add(direction.clone().multiply(i));
            Block block = step.getBlock();

            if (block.getType().isSolid()) {
                Location prev = start.clone().add(direction.clone().multiply(i - 1));
                return isSafe(prev) ? prev.setDirection(player.getLocation().getDirection()) : null;
            }
        }

        Location end = start.clone().add(direction.multiply(10));
        return isSafe(end) ? end.setDirection(player.getLocation().getDirection()) : null;
    }

    private boolean isSafe(Location loc) {
        Block block = loc.getBlock();
        Block above = loc.clone().add(0, 1, 0).getBlock();
        return block.getType() == Material.AIR && above.getType() == Material.AIR;
    }

    private void applyEffects(Player player, ItemStack heldItem) {
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        if (activeGem != null && activeGem.isSimilar(heldItem) && isSpeedGem(activeGem)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false, true));
        }
    }

    private void removeEffects(Player player, ItemStack heldItem) {
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        if (activeGem != null && activeGem.isSimilar(heldItem) && isSpeedGem(activeGem)) {
            player.removePotionEffect(PotionEffectType.SPEED);
        }
    }

    private boolean isSpeedGem(ItemStack item) {
        return item != null &&
                item.getType() == Material.WOODEN_SWORD &&
                item.hasItemMeta() &&
                ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Speed Gem");
    }
}