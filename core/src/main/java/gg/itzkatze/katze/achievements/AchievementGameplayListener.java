package gg.itzkatze.katze.achievements;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.event.PlayerExcludeEvent;
import gg.itzkatze.katze.event.TimerPauseEvent;
import gg.itzkatze.katze.event.TimerStartEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AchievementGameplayListener implements Listener {
    private static final NamespacedKey PLACED_COAL = new NamespacedKey("katzegames", "placed_deepslate_coal");
    private final AchievementService service;
    private final AchievementSampler sampler;
    private final Set<UUID> inventoryChecks = new HashSet<>();
    private final Set<UUID> teleportedBoats = new HashSet<>();

    public AchievementGameplayListener(AchievementService service, AchievementSampler sampler) {
        this.service = service;
        this.sampler = sampler;
    }

    private void inspectLater(Player player) {
        if (!inventoryChecks.add(player.getUniqueId())) return;
        Bukkit.getScheduler().runTask(KatzeGames.get(), () -> {
            inventoryChecks.remove(player.getUniqueId());
            if (player.isOnline()) AchievementInventory.inspect(player, service);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventory(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) inspectLater(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) inspectLater(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) inspectLater(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getAction() != InventoryAction.NOTHING && event.getRecipe().getResult().getType() == Material.ENDER_EYE
                && event.getWhoClicked() instanceof Player player) service.record(player, Achievement.CRAFT_ENDER_EYE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (event.getOwner() instanceof Player player) service.record(player, Achievement.TAME_ANIMAL);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.isBedSpawn()) service.record(event.getPlayer(), Achievement.BED_RESPAWN);
        sampler.invalidate(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHorn(PlayerStatisticIncrementEvent event) {
        if (event.getStatistic() == Statistic.USE_ITEM && event.getMaterial() == Material.GOAT_HORN && event.getNewValue() > event.getPreviousValue()) {
            service.record(event.getPlayer(), Achievement.USE_GOAT_HORN);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreakTool(PlayerItemBreakEvent event) {
        if (AchievementInventory.tool(event.getBrokenItem().getType())) service.record(event.getPlayer(), Achievement.BREAK_TOOL);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArrow(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow) || !(arrow.getShooter() instanceof Player player)) return;
        String mob = event.getEntity().getType().name();
        PotionEffectType needed = switch (mob) {
            case "STRAY", "PARCHED" -> PotionEffectType.WEAKNESS;
            case "BOGGED" -> PotionEffectType.POISON;
            default -> null;
        };
        PotionType potion = arrow.getBasePotionType();
        if (needed != null && (arrow.hasCustomEffect(needed) || potion != null && potion.getPotionEffects().stream().anyMatch(effect -> effect.getType().equals(needed)))) {
            service.record(player, Achievement.RETURN_TO_SENDER);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!service.forceBattleEligible(player)) return;
        Material material = event.getItem().getType();
        if (material.isEdible()) {
            service.participant(player).food(AchievementInventory.vegan(material));
            if (Set.of(Material.MUSHROOM_STEW, Material.RABBIT_STEW, Material.SUSPICIOUS_STEW).contains(material)) {
                service.distinct(player, Achievement.THREE_STEWS, material.name());
            }
        } else if (material == Material.MILK_BUCKET || material == Material.HONEY_BOTTLE) {
            service.participant(player).food(false);
        }
        if (material == Material.ENCHANTED_GOLDEN_APPLE) service.record(player, Achievement.ENCHANTED_GOLDEN_APPLE);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!service.forceBattleEligible(player)) return;
        service.add(player, Achievement.ROUND_DEATHS, 1);
        if (player.getLastDamageCause() != null) service.distinct(player, Achievement.FIVE_DEATH_CAUSES, player.getLastDamageCause().getCause().name());
        service.snapshot(player, Achievement.MINING_FATIGUE, 0);
        sampler.invalidate(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null && event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent damage) killer = owner(damage.getDamager());
        if (killer != null) service.distinct(killer, Achievement.MOB_TYPES, event.getEntityType().getKey().toString());
    }

    private Player owner(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) return owner(shooter);
        if (entity instanceof Tameable tameable) {
            AnimalTamer owner = tameable.getOwner();
            if (owner != null) return Bukkit.getPlayer(owner.getUniqueId());
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        NamespacedKey key = event.getAdvancement().getKey();
        if (key.getNamespace().equals("minecraft") && !key.getKey().startsWith("recipes/")
                && event.getAdvancement().getDisplay() != null && !event.getAdvancement().getDisplay().isHidden()) {
            service.distinct(event.getPlayer(), Achievement.ADVANCEMENTS, key.toString());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaid(RaidTriggerEvent event) { service.record(event.getPlayer(), Achievement.START_RAID); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() == Material.DEEPSLATE_COAL_ORE) markPlacedCoal(block);
        if (block.getType() == Material.LIGHTNING_ROD && service.forceBattleEligible(event.getPlayer())) {
            service.round().rods().put(position(block), event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMine(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.DEEPSLATE_COAL_ORE && !placedCoal(block)) service.record(event.getPlayer(), Achievement.DEEPSLATE_COAL);
        forgetRod(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLightning(LightningStrikeEvent event) {
        if (event.getCause() != LightningStrikeEvent.Cause.WEATHER || service.round() == null) return;
        Block strike = event.getLightning().getLocation().getBlock();
        for (Block block : List.of(strike, strike.getRelative(0, -1, 0))) {
            if (block.getType() != Material.LIGHTNING_ROD) continue;
            UUID id = service.round().rods().get(position(block));
            if (id != null) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) service.record(player, Achievement.LIGHTNING_ROD);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) { event.blockList().forEach(this::forgetRod); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(BlockExplodeEvent event) { event.blockList().forEach(this::forgetRod); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        event.getBlocks().forEach(block -> {
            forgetRod(block);
            if (block.getType() == Material.DEEPSLATE_COAL_ORE) markPlacedCoal(block.getRelative(event.getDirection()));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent event) {
        event.getBlocks().forEach(block -> {
            forgetRod(block);
            if (block.getType() == Material.DEEPSLATE_COAL_ORE) markPlacedCoal(block.getRelative(event.getDirection()));
        });
    }

    private void forgetRod(Block block) {
        if (service.round() != null) service.round().rods().remove(position(block));
    }

    private static String position(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private static String localPosition(Block block) { return ";" + block.getX() + "," + block.getY() + "," + block.getZ() + ";"; }

    private static boolean placedCoal(Block block) {
        String positions = block.getChunk().getPersistentDataContainer().getOrDefault(PLACED_COAL, PersistentDataType.STRING, "");
        return positions.contains(localPosition(block));
    }

    private static void markPlacedCoal(Block block) {
        if (placedCoal(block)) return;
        String positions = block.getChunk().getPersistentDataContainer().getOrDefault(PLACED_COAL, PersistentDataType.STRING, "");
        block.getChunk().getPersistentDataContainer().set(PLACED_COAL, PersistentDataType.STRING, positions + localPosition(block));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        sampler.invalidate(event.getPlayer().getUniqueId());
        inspectLater(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        sampler.invalidate(event.getPlayer().getUniqueId());
        service.jumpEnded(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) { sampler.invalidate(event.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorld(PlayerChangedWorldEvent event) {
        if (event.getPlayer().getWorld().getEnvironment() == World.Environment.NETHER) service.record(event.getPlayer(), Achievement.ENTER_NETHER);
        inspectLater(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock() || !service.forceBattleEligible(event.getPlayer())) return;
        String biome = event.getTo().getBlock().getBiome().getKey().toString();
        service.distinct(event.getPlayer(), Achievement.BIOMES, biome);
        if (biome.equals("minecraft:mushroom_fields")) service.record(event.getPlayer(), Achievement.MUSHROOM_FIELDS);
        if (event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) service.record(event.getPlayer(), Achievement.ENTER_NETHER);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBoatMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat boat) || teleportedBoats.contains(boat.getUniqueId()) || boat.getPassengers().isEmpty()
                || !(boat.getPassengers().getFirst() instanceof Player player)
                || !event.getFrom().getWorld().equals(event.getTo().getWorld())) return;
        sampler.boatMoved(player, event.getFrom().distance(event.getTo()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBoatTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof Boat)) return;
        UUID id = event.getEntity().getUniqueId();
        teleportedBoats.add(id);
        Bukkit.getScheduler().runTask(KatzeGames.get(), () -> teleportedBoats.remove(id));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExclude(PlayerExcludeEvent event) {
        if (event.isNewExcluded()) service.disqualify(event.getPlayer().getUuid());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMode(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() != GameMode.SURVIVAL) service.disqualify(event.getPlayer().getUniqueId());
        sampler.invalidate(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPause(TimerPauseEvent event) { sampler.invalidateAll(); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResume(TimerStartEvent event) { sampler.invalidateAll(); }
}
