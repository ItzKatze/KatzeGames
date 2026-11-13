package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.game.GameWorldManager;
import gg.itzkatze.katze.game.VoidGenerator;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.structure.Structure;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

final class MobArmyArena {
    static final int FLOOR = 80;
    private final Map<Integer, World> worlds = new HashMap<>();
    private int sideCount;
    private SchematicTemplate template;
    private ArenaLayout layout;
    private List<Part> parts;
    private final Map<String, BlockData> states = new HashMap<>();
    private final Map<Integer, List<Location>> surfaces = new HashMap<>();
    private final Deque<BuildStep> builds = new ArrayDeque<>();
    private BukkitTask builder;

    SchematicTemplate template() {
        if (template == null) {
            try (InputStream input = getClass().getResourceAsStream("/arena/village.schem")) {
                template = SchematicTemplate.read(input);
                layout = new ArenaLayout(template.width(), template.height(), template.length(), FLOOR, template.roofLayer());
            } catch (IOException exception) { throw new IllegalStateException("Could not load MobArmy arena/village.schem", exception); }
        }
        return template;
    }

    ArenaLayout layout() { template(); return layout; }
    int roofHeight() { return layout().roof(); }
    boolean ready(int index) { return surfaces.containsKey(index); }

    static NamespacedKey worldKey(int index) {
        if (index < 0) throw new IllegalArgumentException("Negative arena index");
        return new NamespacedKey("katzegames", "arena_mobarmy_" + index);
    }

    World world(int index) {
        sideCount = Math.max(sideCount, index + 1);
        return worlds.computeIfAbsent(index, key -> {
            World world = Objects.requireNonNull(WorldCreator.ofKey(worldKey(index)).generator(new VoidGenerator())
                    .generateStructures(false).createWorld(), "Could not create MobArmy world");
            if (layout().maxHeight() >= world.getMaxHeight()) throw new IllegalStateException("MobArmy arena exceeds the world height");
            world.setDifficulty(Difficulty.NORMAL);
            world.setGameRule(GameRules.SPAWN_MOBS, false);
            world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
            world.setGameRule(GameRules.ADVANCE_TIME, false);
            world.setGameRule(GameRules.ADVANCE_WEATHER, false);
            world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
            applyEnvironment(world);
            return world;
        });
    }

    static void applyEnvironment(World world) {
        if (world.getTime() != 6000) world.setTime(6000);
        if (world.hasStorm()) world.setStorm(false);
        if (world.isThundering()) world.setThundering(false);
        world.setClearWeatherDuration(Integer.MAX_VALUE);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
    }

    void maintainEnvironment() { worlds.values().forEach(MobArmyArena::applyEnvironment); }
    boolean owns(World candidate) { return worldIndex(candidate.getKey(), Integer.MAX_VALUE) >= 0; }

    static int worldIndex(NamespacedKey key, int count) {
        String prefix = "arena_mobarmy_";
        if (!key.getNamespace().equals("katzegames") || !key.getKey().startsWith(prefix)) return -1;
        try {
            int index = Integer.parseInt(key.getKey().substring(prefix.length()));
            return index >= 0 && index < count ? index : -1;
        } catch (NumberFormatException exception) { return -1; }
    }

    private List<Part> parts() {
        if (parts == null) {
            try {
                List<Part> loaded = new ArrayList<>();
                for (var part : template().parts()) {
                    Structure structure = Bukkit.getStructureManager().loadStructure(new ByteArrayInputStream(part.nbt()));
                    if (structure.getPalettes().isEmpty() || structure.getPalettes().getFirst().getBlocks().stream()
                            .noneMatch(block -> !block.getType().isAir())) {
                        throw new IllegalStateException("The custom arena decoded as air; its structure block-state format does not match its data version");
                    }
                    loaded.add(new Part(part.x(), part.z(), structure));
                }
                parts = List.copyOf(loaded);
            } catch (IOException exception) { throw new IllegalStateException("Could not prepare MobArmy arena structures", exception); }
        }
        return parts;
    }

    void prepare(int count, Runnable onReady, Consumer<RuntimeException> onFailure) {
        AtomicInteger remaining = new AtomicInteger(count);
        for (int index = 0; index < count; index++) enqueue(index, () -> { if (remaining.decrementAndGet() == 0) onReady.run(); }, onFailure);
    }

    void rebuild(int index, MobArmyMatch.Side side, Runnable onReady, Consumer<RuntimeException> onFailure) {
        clearEntities(index);
        side.blocks.clear();
        surfaces.remove(index);
        enqueue(index, onReady, onFailure);
    }

    private void enqueue(int index, Runnable onReady, Consumer<RuntimeException> onFailure) {
        pinChunks(index);
        for (Part part : parts()) builds.addLast(new BuildStep(() -> prime(index, part), onFailure));
        for (Part part : parts()) builds.addLast(new BuildStep(() -> paste(index, part), onFailure));
        builds.addLast(new BuildStep(() -> { finish(index); onReady.run(); }, onFailure));
        if (builder == null) builder = Bukkit.getScheduler().runTaskTimer(KatzeGames.get(), this::build, 1, 1);
    }

    private void build() {
        for (int i = 0; i < 2 && !builds.isEmpty(); i++) {
            BuildStep step = builds.removeFirst();
            try { step.action.run(); }
            catch (RuntimeException exception) { cancelBuilds(); step.failure.accept(exception); return; }
        }
        if (builds.isEmpty()) cancelBuilds();
    }

    void cancelBuilds() {
        builds.clear();
        if (builder != null) { builder.cancel(); builder = null; }
    }

    void generate(int index) {
        pinChunks(index);
        for (Part part : parts()) prime(index, part);
        for (Part part : parts()) paste(index, part);
        finish(index);
    }

    private void prime(int index, Part part) {
        int width = Math.min(16, layout().width() - part.x);
        int length = Math.min(16, layout().length() - part.z);
        for (int y = 0; y < template().height(); y++) for (int z = 0; z < length; z++) for (int x = 0; x < width; x++) {
            BlockData data = states.computeIfAbsent(template().block(part.x + x, y, part.z + z), Bukkit::createBlockData);
            Block block = world(index).getBlockAt(part.x + x, FLOOR + y, part.z + z);
            if (!block.getBlockData().matches(data)) block.setBlockData(data, false);
        }
    }

    private void paste(int index, Part part) {
        part.structure.place(new Location(world(index), part.x, FLOOR, part.z), false,
                StructureRotation.NONE, Mirror.NONE, 0, 1, new Random(0));
    }

    private void pinChunks(int index) {
        for (int x = 0; x <= Math.floorDiv(layout().width(), 16); x++) {
            for (int z = -1; z <= Math.floorDiv(layout().length(), 16); z++) world(index).getChunkAt(x, z).addPluginChunkTicket(KatzeGames.get());
        }
    }

    private void finish(int index) {
        ArenaLayout bounds = layout();
        List<Location> positions = new ArrayList<>();
        for (int x = 1; x < bounds.width() - 1; x++) for (int z = 1; z < bounds.length() - 1; z++) {
            for (int y = bounds.roof() - 3; y > FLOOR; y--) {
                Location position = new Location(world(index), x + 0.5, y, z + 0.5);
                if (valid(position)) { positions.add(position); break; }
            }
        }
        if (positions.isEmpty()) throw new IllegalStateException("The custom MobArmy arena has no safe spawn surfaces");
        double centerX = bounds.width() / 2.0;
        double centerZ = bounds.length() / 2.0;
        positions.sort(Comparator.comparingDouble(position -> Math.pow(position.getX() - centerX, 2) + Math.pow(position.getZ() - centerZ, 2)));
        surfaces.put(index, List.copyOf(positions));
    }

    private boolean valid(Location position) {
        Block floor = position.clone().subtract(0, 1, 0).getBlock();
        Material type = floor.getType();
        double floorHeight = floor.getBoundingBox().getHeight();
        return type.isSolid() && type != Material.BARRIER && type != Material.MAGMA_BLOCK && type != Material.CACTUS
                && !Tag.LEAVES.isTagged(type) && floorHeight >= 0.875 && floorHeight <= 1
                && position.getBlock().getType() != Material.FIRE && position.getBlock().getType() != Material.SOUL_FIRE
                && position.getBlock().getType() != Material.SWEET_BERRY_BUSH && position.getBlock().getType() != Material.POWDER_SNOW
                && position.getBlock().isPassable() && !position.getBlock().isLiquid()
                && position.clone().add(0, 1, 0).getBlock().isPassable() && !position.clone().add(0, 1, 0).getBlock().isLiquid();
    }

    Location entrance(int index) {
        return surfaces.getOrDefault(index, List.of()).stream().filter(this::valid).findFirst()
                .orElseThrow(() -> new IllegalStateException("The custom arena has no unobstructed player spawn")).clone();
    }

    Location roof(int index) { return new Location(world(index), layout().width() / 2.0, roofHeight() + 1, layout().length() / 2.0); }

    boolean inside(int index, Location position, boolean waiting) {
        return position.getWorld().getKey().equals(worldKey(index)) && layout().contains(position.getX(), position.getY(), position.getZ(), waiting);
    }

    boolean insideMob(int index, Location position) {
        return position.getWorld().getKey().equals(worldKey(index)) && layout().containsMob(position.getX(), position.getY(), position.getZ());
    }

    int index(Location position, int sides) { return worldIndex(position.getWorld().getKey(), sides); }

    Location spawn(int index, LivingEntity entity) {
        List<Location> positions = surfaces.getOrDefault(index, List.of());
        for (int attempt = 0; attempt < 100 && !positions.isEmpty(); attempt++) {
            Location position = positions.get(ThreadLocalRandom.current().nextInt(positions.size()));
            if (valid(position) && fits(position, entity)) return position.clone();
        }
        return positions.stream().filter(position -> valid(position) && fits(position, entity)).findFirst()
                .orElseThrow(() -> new IllegalStateException("The custom arena has no clear spawn for " + entity.getType())).clone();
    }

    private boolean fits(Location position, LivingEntity entity) {
        double radius = entity.getWidth() / 2;
        int minX = (int) Math.floor(position.getX() - radius);
        int maxX = (int) Math.floor(Math.nextDown(position.getX() + radius));
        int minZ = (int) Math.floor(position.getZ() - radius);
        int maxZ = (int) Math.floor(Math.nextDown(position.getZ() + radius));
        int maxY = (int) Math.floor(Math.nextDown(position.getY() + entity.getHeight()));
        if (position.getY() + entity.getHeight() >= roofHeight()) return false;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) for (int y = position.getBlockY(); y <= maxY; y++) {
            Block block = position.getWorld().getBlockAt(x, y, z);
            if (!block.isPassable() || block.isLiquid() || block.getType() == Material.FIRE || block.getType() == Material.SOUL_FIRE
                    || block.getType() == Material.SWEET_BERRY_BUSH || block.getType() == Material.POWDER_SNOW) return false;
        }
        return true;
    }

    void remember(MobArmyMatch.Side side, Block block, boolean placed) {
        if (side.blocks.stream().noneMatch(change -> change.x() == block.getX() && change.y() == block.getY() && change.z() == block.getZ())) {
            side.blocks.add(new MobArmyMatch.BlockChange(block.getX(), block.getY(), block.getZ(), block.getBlockData().getAsString(), placed));
        }
    }

    boolean placed(MobArmyMatch.Side side, Block block) {
        return side.blocks.stream().anyMatch(change -> change.placed() && change.x() == block.getX() && change.y() == block.getY() && change.z() == block.getZ());
    }

    void clearEntities(int index) {
        for (Entity entity : new ArrayList<>(world(index).getEntities())) {
            if (!(entity instanceof Player)) entity.remove();
        }
    }

    void cleanup() {
        cancelBuilds();
        surfaces.clear();
        List<NamespacedKey> keys = new ArrayList<>();
        for (int index = 0; index < sideCount; index++) keys.add(worldKey(index));
        for (World loaded : Bukkit.getWorlds()) if (owns(loaded) && !keys.contains(loaded.getKey())) keys.add(loaded.getKey());
        for (NamespacedKey key : keys) {
            World existing = Bukkit.getWorld(key);
            if (existing != null) {
                var destination = GameWorldManager.getWorld();
                if (destination == null) destination = GameWorldManager.getLimbo();
                for (Player player : new ArrayList<>(existing.getPlayers())) player.teleport(destination.getSpawnLocation());
                existing.removePluginChunkTickets(KatzeGames.get());
            }
            GameWorldManager.deleteChallengeWorld(key);
        }
        worlds.clear();
        sideCount = 0;
    }

    private record Part(int x, int z, Structure structure) {}
    private record BuildStep(Runnable action, Consumer<RuntimeException> failure) {}
}
