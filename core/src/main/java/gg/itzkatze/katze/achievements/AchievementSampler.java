package gg.itzkatze.katze.achievements;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.StructureScanner;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class AchievementSampler {
    private static final Statistic[] WALKING = {Statistic.WALK_ONE_CM, Statistic.SPRINT_ONE_CM, Statistic.CROUCH_ONE_CM,
            Statistic.WALK_ON_WATER_ONE_CM, Statistic.WALK_UNDER_WATER_ONE_CM};
    private final AchievementService service;
    private final Map<UUID, Long> walking = new HashMap<>();
    private final Map<UUID, Integer> walkingRemainders = new HashMap<>();
    private final Map<UUID, Double> boatRemainders = new HashMap<>();
    private BukkitTask task;
    private UUID roundId;

    public AchievementSampler(AchievementService service) { this.service = service; }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(KatzeGames.get(), this::tick, 20, 20);
    }

    public void stop() {
        if (task != null) task.cancel();
        walking.clear();
    }

    public void invalidate(UUID player) {
        walking.remove(player);
    }

    public void invalidateAll() {
        walking.clear();
    }

    private void tick() {
        checkRound();
        service.retryPending();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (service.round() != null && !service.round().finished() && !KatzePlayer.adapt(player).isParticipating()) service.disqualify(player.getUniqueId());
            if (!service.eligible(player) || player.isDead()) {
                invalidate(player.getUniqueId());
                continue;
            }
            AchievementInventory.inspect(player, service);
            walk(player);
            if (player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) service.add(player, Achievement.MINING_FATIGUE, 1);
            String biome = player.getLocation().getBlock().getBiome().getKey().toString();
            service.distinct(player, Achievement.BIOMES, biome);
            if (biome.equals("minecraft:mushroom_fields")) service.record(player, Achievement.MUSHROOM_FIELDS);
            if (player.getWorld().getEnvironment() == World.Environment.NETHER) service.record(player, Achievement.ENTER_NETHER);
            structures(player);
        }
    }

    private void checkRound() {
        if (!Objects.equals(roundId, service.roundId())) {
            walking.clear();
            walkingRemainders.clear();
            boatRemainders.clear();
            roundId = service.roundId();
        }
    }

    private void walk(Player player) {
        long now = 0;
        for (Statistic statistic : WALKING) now += player.getStatistic(statistic);
        UUID id = player.getUniqueId();
        Long before = walking.put(id, now);
        if (before == null || now < before) return;
        long delta = now - before + walkingRemainders.getOrDefault(id, 0);
        walkingRemainders.put(id, (int) (delta % 100));
        service.add(player, Achievement.WALK_10000, (int) Math.min(Integer.MAX_VALUE, delta / 100));
    }

    public void boatMoved(Player player, double distance) {
        checkRound();
        if (!service.eligible(player) || distance <= 0 || distance > 16) return;
        UUID id = player.getUniqueId();
        distance += boatRemainders.getOrDefault(id, 0.0);
        boatRemainders.put(id, distance % 1);
        service.add(player, Achievement.BOAT_DISTANCE, (int) distance);
    }

    private void structures(Player player) {
        Location location = player.getLocation();
        if (StructureScanner.contains(location, NamespacedKey.minecraft("trial_chambers"))) service.record(player, Achievement.TRIAL_CHAMBER);
        if (StructureScanner.contains(location, NamespacedKey.minecraft("stronghold"))) service.record(player, Achievement.STRONGHOLD);
        if (player.getWorld().getEnvironment() != World.Environment.NETHER) return;
        for (String structure : new String[]{"ruined_portal_nether", "fortress", "bastion_remnant"}) {
            if (StructureScanner.contains(location, NamespacedKey.minecraft(structure))) service.distinct(player, Achievement.NETHER_STRUCTURES, structure);
        }
    }
}
