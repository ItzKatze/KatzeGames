package gg.itzkatze.katze.game;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.util.WorldStorage;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PauseManager implements Listener {

    private static final NamespacedKey PAUSED_AI_KEY = new NamespacedKey("katzegames", "paused_ai");

    private boolean pauseApplied = false;
    private static final String PAUSE_FILE = "pause.json";
    private final Map<String, WorldRules> rulesByWorld = new HashMap<>();
    private BukkitTask watcher;

    private record WorldRules(Integer randomTicks, Boolean advanceTime, Boolean advanceWeather) {
        static WorldRules of(World world) {
            return new WorldRules(world.getGameRuleValue(GameRules.RANDOM_TICK_SPEED),
                    world.getGameRuleValue(GameRules.ADVANCE_TIME), world.getGameRuleValue(GameRules.ADVANCE_WEATHER));
        }

        void restore(World world) {
            if (randomTicks != null) world.setGameRule(GameRules.RANDOM_TICK_SPEED, randomTicks);
            if (advanceTime != null) world.setGameRule(GameRules.ADVANCE_TIME, advanceTime);
            if (advanceWeather != null) world.setGameRule(GameRules.ADVANCE_WEATHER, advanceWeather);
        }
    }

    private static final class SavedRules {
        Map<String, WorldRules> worlds = new HashMap<>();
    }
    private final Map<UUID, Location> frozenLocations = new HashMap<>();
    private final Set<UUID> aiDisabledEntities = new HashSet<>();

    public void startStateWatcher() {
        SavedRules saved = WorldStorage.load(PAUSE_FILE, SavedRules.class);
        if (saved != null && saved.worlds != null) {
            rulesByWorld.putAll(saved.worlds);
            restoreWorldRules();
        }
        Bukkit.getWorlds().forEach(world -> world.getEntities().forEach(this::restoreEntityAI));
        watcher = Bukkit.getScheduler().runTaskTimer(KatzeGames.get(), this::updatePauseState, 1L, 5L);
    }

    public void shutdown() {
        release();
        if (watcher != null) {
            watcher.cancel();
            watcher = null;
        }
    }

    public void release() {
        if (pauseApplied) clearPause();
    }

    private void updatePauseState() {
        boolean shouldPause = ChallengeManager.isGameStarted() &&
                KatzeGames.getTimer().getMode() != GameTimer.Mode.DISABLED &&
                !KatzeGames.getTimer().hasHitZero() &&
                !KatzeGames.getTimer().isRunning();
        if (shouldPause && !pauseApplied) {
            applyPause();
        } else if (!shouldPause && pauseApplied) {
            clearPause();
        }
    }

    private void applyPause() {
        pauseApplied = true;
        for (World world : Bukkit.getWorlds()) {
            pauseWorld(world);
        }
    }

    private void pauseWorld(World world) {
        rulesByWorld.putIfAbsent(world.getUID().toString(), WorldRules.of(world));
        SavedRules saved = new SavedRules();
        saved.worlds.putAll(rulesByWorld);
        WorldStorage.save(PAUSE_FILE, saved);
        world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.getEntities().forEach(this::pauseEntity);
    }

    private void pauseEntity(Entity entity) {
        entity.setVelocity(new Vector(0, 0, 0));
        if (entity instanceof LivingEntity living && !(living instanceof Player) && living.hasAI()) {
            living.getPersistentDataContainer().set(PAUSED_AI_KEY, PersistentDataType.BOOLEAN, true);
            living.setAI(false);
            aiDisabledEntities.add(living.getUniqueId());
        }
    }

    private void clearPause() {
        pauseApplied = false;
        restoreWorldRules();
        Bukkit.getWorlds().forEach(world -> world.getEntities().forEach(this::restoreEntityAI));
        frozenLocations.clear();
    }

    private void restoreWorldRules() {
        rulesByWorld.entrySet().removeIf(entry -> {
            World world = Bukkit.getWorld(UUID.fromString(entry.getKey()));
            if (world == null) return false;
            if (entry.getValue() != null) entry.getValue().restore(world);
            return true;
        });
        if (rulesByWorld.isEmpty()) {
            WorldStorage.delete(PAUSE_FILE);
        } else {
            SavedRules saved = new SavedRules();
            saved.worlds.putAll(rulesByWorld);
            WorldStorage.save(PAUSE_FILE, saved);
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (pauseApplied) {
            pauseWorld(event.getWorld());
        } else {
            restoreWorldRules();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(EntitySpawnEvent event) {
        if (pauseApplied) pauseEntity(event.getEntity());
    }

    private void restoreEntityAI(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }
        UUID entityId = livingEntity.getUniqueId();
        boolean pausedByPlugin = aiDisabledEntities.contains(entityId) || livingEntity.getPersistentDataContainer().has(PAUSED_AI_KEY);
        if (!pausedByPlugin) {
            return;
        }
        try {
            livingEntity.setAI(true);
            livingEntity.getPersistentDataContainer().remove(PAUSED_AI_KEY);
            aiDisabledEntities.remove(entityId);
        } catch (Throwable ignored) {
        }
    }

    private boolean isPausedGameplay() {
        return pauseApplied && ChallengeManager.isGameStarted();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            frozenLocations.remove(event.getPlayer().getUniqueId());
            return;
        }
        if (!isPausedGameplay()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        boolean movedPosition = from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();
        if (!movedPosition) return;

        UUID playerId = event.getPlayer().getUniqueId();
        Location freezeAt = frozenLocations.computeIfAbsent(playerId, id -> from.clone());
        event.setTo(freezeAt);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!isPausedGameplay()) return;
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockCook(BlockCookEvent event) {
        if (isPausedGameplay()) event.setCancelled(true);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (pauseApplied) pauseEntity(entity);
            else restoreEntityAI(entity);
        }
    }
}
