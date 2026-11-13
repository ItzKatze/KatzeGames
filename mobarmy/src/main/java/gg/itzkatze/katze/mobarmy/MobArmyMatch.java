package gg.itzkatze.katze.mobarmy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MobArmyMatch {

    int version = 1;
    String minecraftVersion;
    String arenaFingerprint;
    int arenaWorldVersion;
    UUID id = UUID.randomUUID();
    long startedAt = System.currentTimeMillis();
    long endedAt;
    boolean archived;
    Phase phase = Phase.COLLECTION;
    List<Side> sides = new ArrayList<>();

    public static int penaltySeconds(int deaths) {
        if (deaths < 1) throw new IllegalArgumentException("Death count must be positive");
        return deaths * 30;
    }

    public Side side(UUID player) {
        return sides.stream().filter(side -> side.members.containsKey(player)).findFirst().orElse(null);
    }

    public List<Side> opponents(Side side) {
        return sides.stream().filter(other -> other != side).toList();
    }

    public Side findSide(String name) {
        return sides.stream().filter(side -> side.members.values().stream().anyMatch(member -> member.equalsIgnoreCase(name))
                || side.id.equals(name)).findFirst().orElse(null);
    }

    public boolean allocate(Side side, String type, int wave, boolean add) {
        if (phase != Phase.CONFIGURATION || !sides.contains(side) || side.finished || wave < 0 || wave > 2) return false;
        CapturedMob mob = side.mobs.stream().filter(entry -> entry.type.equals(type)
                && (add ? entry.wave == -1 : entry.wave == wave)).findFirst().orElse(null);
        if (mob == null) return false;
        mob.wave = add ? wave : -1;
        side.ready = false;
        return true;
    }

    public boolean toggleReady(Side side) {
        if (phase != Phase.CONFIGURATION || !sides.contains(side) || side.finished) return false;
        side.ready = !side.ready;
        return sides.stream().allMatch(entry -> entry.ready);
    }

    public List<Side> winners() {
        long best = sides.stream().filter(side -> side.finished && !side.forfeited)
                .mapToLong(side -> side.elapsedTicks).min().orElse(-1);
        return sides.stream().filter(side -> side.finished && !side.forfeited && side.elapsedTicks == best).toList();
    }

    void prepareWave(Side side) {
        if (phase != Phase.COMBAT || side.finished || side.preparing) throw new IllegalStateException("Not fighting");
        side.queue = new ArrayList<>(opponents(side).get(side.opponent).mobs.stream().filter(mob -> mob.wave == side.wave).toList());
        side.spawnTicks = 0;
    }

    Progress advance(Side side) {
        if (phase != Phase.COMBAT || side.finished || side.preparing || !side.queue.isEmpty() || !side.live.isEmpty()) {
            throw new IllegalStateException("Wave is not complete");
        }
        if (side.wave < 2) {
            side.wave++;
            return Progress.WAVE;
        }
        if (side.opponent + 1 < sides.size() - 1) {
            side.opponent++;
            side.wave = 0;
            side.deaths.clear();
            side.roofTicks.clear();
            return Progress.OPPONENT;
        }
        side.finished = true;
        return Progress.FINISHED;
    }

    CapturedMob nextSpawn(Side side) {
        if (phase != Phase.COMBAT || side.finished || side.preparing || side.queue.isEmpty()) return null;
        if (++side.spawnTicks < 4) return null;
        side.spawnTicks = 0;
        return side.queue.removeFirst();
    }

    void tickClock(Side side, boolean running) {
        if (phase != Phase.COMBAT || side.finished || side.preparing || !running) return;
        side.elapsedTicks++;
        side.roofTicks.replaceAll((uuid, ticks) -> ticks - 1);
    }

    public void validate() {
        if (version != 1 || id == null || phase == null || sides == null || sides.size() < 2) {
            throw new IllegalStateException("Invalid MobArmy match");
        }
        var members = new HashSet<UUID>();
        var ids = new HashSet<String>();
        for (Side side : sides) {
            if (side.id == null || !ids.add(side.id) || side.members == null || side.members.isEmpty()
                    || side.mobs == null || side.deaths == null || side.roofTicks == null || side.live == null
                    || side.queue == null || side.baselines == null || side.players == null || side.pendingBaseline == null
                    || side.backpack == null || side.backpackBaseline == null || side.blocks == null || side.elapsedTicks < 0
                    || side.opponent < 0 || side.opponent >= sides.size() - 1 || side.wave < 0 || side.wave > 2
                    || side.spawnTicks < 0 || side.spawnTicks > 3) throw new IllegalStateException("Invalid MobArmy side");
            for (UUID member : side.members.keySet()) {
                if (member == null || !members.add(member)) throw new IllegalStateException("Duplicate MobArmy participant");
            }
            List<CapturedMob> captured = new ArrayList<>(side.mobs);
            captured.addAll(side.queue);
            for (CapturedMob mob : captured) {
                if (mob == null || mob.data == null || mob.data.isEmpty() || mob.variant == null || mob.type == null
                        || mob.equipment == null || mob.attributes == null || mob.wave < -1 || mob.wave > 2) {
                    throw new IllegalStateException("Invalid captured mob");
                }
            }
            if (!side.players.keySet().containsAll(side.members.keySet())) throw new IllegalStateException("Missing participant snapshot");
            side.players.values().forEach(PlayerLoadout::validate);
            side.baselines.values().forEach(PlayerLoadout::validate);
            for (var entry : side.live.entrySet()) {
                var mob = entry.getValue();
                if (entry.getKey() == null || mob == null || mob.data() == null || mob.data().isEmpty()
                        || !Double.isFinite(mob.x()) || !Double.isFinite(mob.y()) || !Double.isFinite(mob.z())) {
                    throw new IllegalStateException("Invalid live mob snapshot");
                }
            }
            if (phase == Phase.COMBAT && !side.forfeited && !side.baselines.keySet().containsAll(side.members.keySet())) {
                throw new IllegalStateException("Missing combat loadout");
            }
        }
    }

    public enum Phase {
        COLLECTION,
        CONFIGURATION,
        COMBAT,
        RESULTS
    }

    enum Progress {
        WAVE,
        OPPONENT,
        FINISHED
    }

    public static final class Side {

        String id;
        Map<UUID, String> members = new LinkedHashMap<>();
        List<CapturedMob> mobs = new ArrayList<>();
        boolean ready;
        boolean finished;
        boolean forfeited;
        boolean preparing;
        int opponent;
        int wave;
        long elapsedTicks;
        int spawnTicks;
        List<CapturedMob> queue = new ArrayList<>();
        Map<UUID, LiveMob> live = new LinkedHashMap<>();
        Map<UUID, Integer> deaths = new HashMap<>();
        Map<UUID, Long> roofTicks = new HashMap<>();
        Map<UUID, PlayerLoadout> baselines = new HashMap<>();
        Map<UUID, PlayerLoadout> players = new HashMap<>();
        List<UUID> pendingBaseline = new ArrayList<>();
        List<String> backpack = new ArrayList<>();
        List<String> backpackBaseline = new ArrayList<>();
        List<BlockChange> blocks = new ArrayList<>();

        public String name() {return String.join(", ", members.values());}

        public String commandName() {return members.values().iterator().next();}

        public String detailsCommand() {return "/waves details " + commandName();}

        public Map<String, List<CapturedMob>> mobsByType() {
            Map<String, List<CapturedMob>> result = new LinkedHashMap<>();
            mobs.stream().sorted(Comparator.comparing(mob -> mob.type)).forEach(mob ->
                    result.computeIfAbsent(mob.type, key -> new ArrayList<>()).add(mob));
            return result;
        }

    }

    public static final class CapturedMob {

        String type;
        String data;
        String variant;
        List<String> equipment = new ArrayList<>();
        List<String> attributes = new ArrayList<>();
        int wave = -1;

    }

    public record LiveMob(String data, double x, double y, double z, float yaw, float pitch) {}

    public record BlockChange(int x, int y, int z, String original, boolean placed, String current, List<String> contents) {

        public BlockChange(int x, int y, int z, String original, boolean placed) {this(x, y, z, original, placed, null, null);}

    }

}
