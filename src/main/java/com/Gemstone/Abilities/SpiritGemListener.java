package com.Gemstone.Abilities;

import com.Gemstone.Listeners.ActiveGemTracker;
import com.Gemstone.Main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SpiritGemListener implements Listener {

    private final JavaPlugin plugin;

    private static class SpiritData {
        private final EntityType type;
        private final double health;

        public SpiritData(EntityType type, double health) {
            this.type = type;
            this.health = health;
        }

        public EntityType getType() {
            return type;
        }

        public double getHealth() {
            return health;
        }
    }

    private final Map<UUID, UUID> captureTargets = new HashMap<>();
    private final Map<UUID, List<SpiritData>> storedMobs = new HashMap<>();
    private final Map<UUID, List<Mob>> activeSummons = new HashMap<>();
    private final Map<UUID, List<Long>> rightClickTimestamps = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private static final String SUMMON_METADATA_KEY = "spirit_gem_summon";
    private static final String SUMMONER_METADATA_KEY = "spirit_gem_summoner";
    private static final int MAX_SPIRITS = 5; // The maximum number of spirits a player can store.
    private static final long SUMMON_COOLDOWN = 60 * 1000L; // 60 seconds
    private static final long SUMMON_DURATION_TICKS = 30 * 20L; // 30 seconds
    private static final int SUMMON_CPS_THRESHOLD = 4;

    public SpiritGemListener(JavaPlugin plugin) {
        this.plugin = plugin;

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!isWorldEnabled(player)) continue;

                    UUID uuid = player.getUniqueId();
                    if (cooldowns.containsKey(uuid)) {
                        long lastUsed = cooldowns.get(uuid);
                        long timeElapsed = System.currentTimeMillis() - lastUsed;
                        long cooldownLeft = SUMMON_COOLDOWN - timeElapsed;

                        if (cooldownLeft > 0L) {
                            // Only show the spirit gem cooldown if it's the active gem
                            if (isSpiritGemActiveAndHeld(player)) {
                                sendActionBar(player, ChatColor.RED + "Spirit Summon Cooldown: " + ChatColor.GOLD + cooldownLeft / 1000L + "s");
                            }
                        } else {
                            cooldowns.remove(uuid);
                            // Optionally notify that it's ready, but only if they are holding the gem
                            if (isSpiritGemActiveAndHeld(player)) {
                                sendActionBar(player, ChatColor.GREEN + "Spirit Summon Ready!");
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this.plugin, 0L, 10L);
    }

    @EventHandler
    public void onMarkMob(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof LivingEntity target)) return;

        // UPDATED: Now checks for world enablement and gem activation
        if (!isWorldEnabled(player) || !isSpiritGemActiveAndHeld(player)) return;

        if (target instanceof Player || target.hasMetadata(SUMMON_METADATA_KEY)) {
            sendActionBar(player, ChatColor.RED + "You cannot capture this entity.");
            return;
        }

        if (!canCaptureType(target.getType())) {
            sendActionBar(player, ChatColor.RED + "This type of mob cannot be captured.");
            return;
        }

        captureTargets.put(player.getUniqueId(), target.getUniqueId());
        sendActionBar(player, ChatColor.AQUA + "Marked " + target.getName() + " for capture. Kill it to store its spirit!");
    }

    @EventHandler
    public void onCaptureMob(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        Player killer = deadEntity.getKiller();

        if (killer == null || !isWorldEnabled(killer)) return;

        UUID killerId = killer.getUniqueId();
        if (deadEntity.getUniqueId().equals(captureTargets.get(killerId))) {
            captureTargets.remove(killerId);

            EntityType type = deadEntity.getType();
            if (!canCaptureType(type)) return;

            List<SpiritData> playerSpirits = storedMobs.computeIfAbsent(killerId, k -> new ArrayList<>());
            if (playerSpirits.size() >= MAX_SPIRITS) {
                sendActionBar(killer, ChatColor.RED + "Your Spirit Gem is full! (" + MAX_SPIRITS + "/" + MAX_SPIRITS + ")");
                return;
            }

            AttributeInstance maxHealthAttr = deadEntity.getAttribute(Attribute.MAX_HEALTH);
            double maxHealth = (maxHealthAttr != null) ? maxHealthAttr.getValue() : 20.0;
            playerSpirits.add(new SpiritData(type, maxHealth));

            sendActionBar(killer, ChatColor.GREEN + "Spirit of " + deadEntity.getName() + " captured! (" + playerSpirits.size() + "/" + MAX_SPIRITS + ")");
        }
    }

    @EventHandler
    public void onTrySummon(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.getAction().name().contains("RIGHT_CLICK") || !player.isSneaking()) return;

        // UPDATED: Now checks for world enablement and gem activation
        if (!isWorldEnabled(player) || !isSpiritGemActiveAndHeld(player)) return;

        UUID playerID = player.getUniqueId();
        List<Long> clicks = rightClickTimestamps.computeIfAbsent(playerID, k -> new ArrayList<>());
        long currentTime = System.currentTimeMillis();

        clicks.add(currentTime);
        clicks.removeIf(time -> time < currentTime - 1000L);

        if (clicks.size() >= SUMMON_CPS_THRESHOLD) {
            clicks.clear();
            summonSpirits(player);
        }
    }

    private void summonSpirits(Player player) {
        UUID playerID = player.getUniqueId();

        long timeSinceLastSummon = System.currentTimeMillis() - cooldowns.getOrDefault(playerID, 0L);
        if (timeSinceLastSummon < SUMMON_COOLDOWN) {
            long cooldownLeft = (SUMMON_COOLDOWN - timeSinceLastSummon) / 1000L;
            sendActionBar(player, ChatColor.RED + "On cooldown for " + cooldownLeft + "s.");
            return;
        }

        List<SpiritData> spiritsToSummon = storedMobs.remove(playerID);
        if (spiritsToSummon == null || spiritsToSummon.isEmpty()) {
            sendActionBar(player, ChatColor.GRAY + "You have no spirits to summon.");
            return;
        }

        List<Mob> summonedList = activeSummons.computeIfAbsent(playerID, k -> new ArrayList<>());
        for (SpiritData spirit : spiritsToSummon) {
            Entity spawned = player.getWorld().spawnEntity(player.getLocation().add(0, 1, 0), spirit.getType());
            if (!(spawned instanceof Mob mob)) {
                spawned.remove();
                continue;
            }

            AttributeInstance maxHealthAttr = mob.getAttribute(Attribute.MAX_HEALTH);
            double maxHealth = (maxHealthAttr != null) ? maxHealthAttr.getValue() : 20.0;
            mob.setHealth(Math.min(spirit.getHealth(), maxHealth));

            mob.setCustomName(ChatColor.AQUA + player.getName() + "'s Spirit");
            mob.setCustomNameVisible(true);
            mob.setTarget(null);

            mob.setMetadata(SUMMON_METADATA_KEY, new FixedMetadataValue(plugin, true));
            mob.setMetadata(SUMMONER_METADATA_KEY, new FixedMetadataValue(plugin, player.getUniqueId().toString()));

            summonedList.add(mob);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (mob.isValid() && !mob.isDead()) {
                        List<SpiritData> playerSpirits = storedMobs.computeIfAbsent(playerID, k -> new ArrayList<>());
                        if (playerSpirits.size() < MAX_SPIRITS) {
                            playerSpirits.add(new SpiritData(mob.getType(), mob.getHealth()));
                            sendActionBar(player, ChatColor.GRAY + "A spirit has returned to your gem.");
                        }
                        mob.remove();
                    }
                    activeSummons.getOrDefault(playerID, new ArrayList<>()).remove(mob);
                }
            }.runTaskLater(plugin, SUMMON_DURATION_TICKS);
        }

        if (!summonedList.isEmpty()) {
            cooldowns.put(playerID, System.currentTimeMillis());
            sendActionBar(player, ChatColor.LIGHT_PURPLE + "Summoned " + summonedList.size() + " spirits!");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof LivingEntity victim)) return;

        List<Mob> playerSummons = activeSummons.get(attacker.getUniqueId());
        if (playerSummons != null && !playerSummons.isEmpty()) {
            playerSummons.forEach(mob -> {
                if (mob.isValid()) {
                    mob.setTarget(victim);
                }
            });
        }
    }

    @EventHandler
    public void onSpiritTarget(EntityTargetLivingEntityEvent event) {
        if (!event.getEntity().hasMetadata(SUMMON_METADATA_KEY)) return;

        LivingEntity target = event.getTarget();
        if (target == null) return;

        UUID summonerID = getSummonerUUID(event.getEntity());
        if (summonerID == null) return;

        if (target.getUniqueId().equals(summonerID)) {
            event.setCancelled(true);
            return;
        }

        if (target.hasMetadata(SUMMON_METADATA_KEY)) {
            UUID targetSummonerID = getSummonerUUID(target);
            if (summonerID.equals(targetSummonerID)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSpiritDamage(EntityDamageByEntityEvent event) {
        if (!event.getDamager().hasMetadata(SUMMON_METADATA_KEY)) return;

        UUID summonerID = getSummonerUUID(event.getDamager());
        if (summonerID != null && event.getEntity().getUniqueId().equals(summonerID)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpiritDeath(EntityDeathEvent event) {
        if (event.getEntity().hasMetadata(SUMMON_METADATA_KEY)) {
            event.getDrops().clear();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();
        captureTargets.remove(playerID);
        rightClickTimestamps.remove(playerID);
        cooldowns.remove(playerID);

        List<Mob> summons = activeSummons.remove(playerID);
        if (summons != null) {
            summons.forEach(mob -> {
                if (mob.isValid()) mob.remove();
            });
        }
    }

    private boolean isSpiritGem(ItemStack item) {
        return item != null &&
                item.getType() == Material.WOODEN_SWORD &&
                item.hasItemMeta() &&
                item.getItemMeta().hasDisplayName() &&
                ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Spirit Gem");
    }

    private boolean isSpiritGemActiveAndHeld(Player player) {
        ItemStack activeGem = ActiveGemTracker.getActiveGem(player);
        if (activeGem == null || !isSpiritGem(activeGem)) {
            return false;
        }

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        return activeGem.isSimilar(mainHandItem) || activeGem.isSimilar(offHandItem);
    }

    private boolean canCaptureType(EntityType type) {
        return type.isAlive() && type != EntityType.PLAYER && type != EntityType.ENDER_DRAGON && type != EntityType.WITHER && type != EntityType.WARDEN;
    }

    private boolean isWorldEnabled(Player player) {
        return ((Main) plugin).isWorldEnabled(player.getWorld().getName());
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    private UUID getSummonerUUID(Entity entity) {
        if (!entity.hasMetadata(SUMMONER_METADATA_KEY)) return null;
        try {
            return UUID.fromString(entity.getMetadata(SUMMONER_METADATA_KEY).get(0).asString());
        } catch (Exception e) {
            return null;
        }
    }
}