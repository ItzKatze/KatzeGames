package gg.itzkatze.katze.util;

import gg.itzkatze.katze.KatzeGames;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.StructurePiece;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StructureScanner {

    private static final int MAX_ENTRIES = 2048;
    private static final int CHUNKS_PER_TICK = 4;
    private static final int CACHE_TICKS = 200;
    private static final Map<ChunkKey, Scan> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static final Set<ChunkKey> PENDING = new LinkedHashSet<>();
    private static BukkitTask task;

    private record ChunkKey(UUID world, int x, int z) {}
    private record Scan(int tick, Map<NamespacedKey, List<BoundingBox>> structures) {}

    private StructureScanner() {}

    public static boolean contains(Location location, NamespacedKey structure) {
        World world = location.getWorld();
        if (world == null) return false;
        ChunkKey key = new ChunkKey(world.getUID(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        Scan scan = CACHE.get(key);
        if (scan == null || Bukkit.getCurrentTick() - scan.tick() >= CACHE_TICKS) {
            if (PENDING.size() < MAX_ENTRIES && world.isChunkLoaded(key.x(), key.z())) {
                PENDING.add(key);
                if (task == null) {
                    task = Bukkit.getScheduler().runTaskTimer(KatzeGames.get(), StructureScanner::tick, 1L, 1L);
                }
            }
        }
        return scan != null && scan.structures().getOrDefault(structure, List.of()).stream()
                .anyMatch(box -> box.contains(location.toVector()));
    }

    public static void clear() {
        CACHE.clear();
        PENDING.clear();
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private static void tick() {
        for (int i = 0; i < CHUNKS_PER_TICK && !PENDING.isEmpty(); i++) {
            ChunkKey key = PENDING.iterator().next();
            PENDING.remove(key);
            World world = Bukkit.getWorld(key.world());
            if (world == null || !world.isChunkLoaded(key.x(), key.z())) continue;
            Map<NamespacedKey, List<BoundingBox>> structures = new LinkedHashMap<>();
            for (GeneratedStructure generated : world.getChunkAt(key.x(), key.z()).getStructures()) {
                NamespacedKey structure = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE).getKey(generated.getStructure());
                if (structure == null) continue;
                List<BoundingBox> boxes = structures.computeIfAbsent(structure, ignored -> new ArrayList<>());
                for (StructurePiece piece : generated.getPieces()) {
                    boxes.add(piece.getBoundingBox());
                }
            }
            CACHE.put(key, new Scan(Bukkit.getCurrentTick(), structures));
            if (CACHE.size() > MAX_ENTRIES) {
                CACHE.remove(CACHE.keySet().iterator().next());
            }
        }
        if (PENDING.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }
}
