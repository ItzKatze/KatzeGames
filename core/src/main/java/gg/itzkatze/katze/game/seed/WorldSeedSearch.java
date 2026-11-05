package gg.itzkatze.katze.game.seed;

import gg.itzkatze.katze.game.GameWorldManager;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Location;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.util.BiomeSearchResult;
import org.bukkit.util.StructureSearchResult;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class WorldSeedSearch implements SeedFeatureSearch {
    private final Map<World.Environment, SeedSearchArea> areas = new EnumMap<>(World.Environment.class);
    private final Map<List<SeedFeature>, Integer> biomeStages = new HashMap<>();
    private final Map<List<SeedFeature>, Integer> structureGroupStages = new HashMap<>();
    private final Set<SeedFeature> nearbyStructures = new HashSet<>();
    private int locateCalls;

    public WorldSeedSearch(int radius) {
        Location spawn = GameWorldManager.getWorld().getSpawnLocation();
        areas.put(World.Environment.NORMAL, new SeedSearchArea(spawn.getX(), spawn.getZ(), radius));
        areas.put(World.Environment.NETHER, new SeedSearchArea(spawn.getX() / 8, spawn.getZ() / 8, radius));
        areas.put(World.Environment.THE_END, new SeedSearchArea(100.5, 0.5, radius));
    }

    public SeedSearchArea area(World.Environment environment) {
        SeedSearchArea area = areas.get(environment);
        if (area == null) throw new IllegalArgumentException("Unsupported dimension " + environment);
        return area;
    }

    public int locateCalls() {
        return locateCalls;
    }

    @Override
    public SeedSearchResult find(List<SeedFeature> alternatives) {
        SeedFeature first = alternatives.getFirst();
        List<SeedFeature> group = alternatives.stream().filter(feature -> feature.kind() == first.kind()
                && feature.environment() == first.environment()).toList();
        World world = GameWorldManager.getWorld(first.environment());
        if (world == null) throw new IllegalStateException("Missing dimension " + first.environment());
        SeedSearchArea area = area(first.environment());
        Location origin = new Location(world, area.x(), 64, area.z());
        return first.kind() == SeedFeature.Kind.BIOME ? findBiomes(group, world, origin, area) : findStructures(group, world, origin, area);
    }

    private SeedSearchResult findBiomes(List<SeedFeature> group, World world, Location origin, SeedSearchArea area) {
        Registry<Biome> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
        int stage = biomeStages.getOrDefault(group, 0);
        List<SeedFeature> targets = stage < 2 ? group : List.of(group.getFirst());
        Biome[] biomes = targets.stream().map(feature -> resolve(registry, feature)).toArray(Biome[]::new);
        locateCalls++;
        BiomeSearchResult result = world.locateNearestBiome(origin, area.radius(), stage == 0 ? 64 : 16, stage == 0 ? 64 : 16, biomes);
        if (result != null) {
            String key = registry.getKeyOrThrow(result.getBiome()).toString();
            SeedFeature matched = targets.stream().filter(feature -> feature.key().equals(key)).findFirst().orElseThrow();
            Optional<SeedValidationResult.Position> position = position(result.getLocation(), area);
            if (position.isPresent()) return SeedSearchResult.found(matched, position.get());
        }
        if (stage == 0 || (stage == 1 && result != null && group.size() > 1)) {
            biomeStages.put(group, stage + 1);
            return SeedSearchResult.pending();
        }
        return new SeedSearchResult(Map.of(), Set.copyOf(targets));
    }

    private SeedSearchResult findStructures(List<SeedFeature> group, World world, Location origin, SeedSearchArea area) {
        Registry<Structure> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);
        List<Structure> structures = group.stream().map(feature -> resolve(registry, feature)).toList();
        StructureType type = structures.getFirst().getStructureType();
        int stage = structureGroupStages.getOrDefault(group, 0);
        if (group.size() > 1 && stage < 2 && structures.stream().allMatch(structure -> structure.getStructureType().equals(type))) {
            locateCalls++;
            StructureSearchResult result = world.locateNearestStructure(origin, type, stage == 0 ? Math.min(4, chunkRadius(area)) : chunkRadius(area), false);
            if (result != null) {
                String key = registry.getKeyOrThrow(result.getStructure()).toString();
                SeedFeature matched = group.stream().filter(feature -> feature.key().equals(key)).findFirst().orElse(null);
                Optional<SeedValidationResult.Position> position = position(result.getLocation(), area);
                if (matched != null && position.isPresent()) return SeedSearchResult.found(matched, position.get());
            }
            if (stage == 1 && result == null) return new SeedSearchResult(Map.of(), Set.copyOf(group));
            structureGroupStages.put(group, stage + 1);
            return SeedSearchResult.pending();
        }
        SeedFeature target = group.stream().filter(feature -> !nearbyStructures.contains(feature)).findFirst().orElse(group.getFirst());
        boolean nearby = nearbyStructures.add(target);
        locateCalls++;
        StructureSearchResult result = world.locateNearestStructure(origin, resolve(registry, target), nearby ? Math.min(4, chunkRadius(area)) : chunkRadius(area), false);
        Optional<SeedValidationResult.Position> position = result == null ? Optional.empty() : position(result.getLocation(), area);
        if (position.isPresent()) return SeedSearchResult.found(target, position.get());
        return nearby && chunkRadius(area) > 4 ? SeedSearchResult.pending() : SeedSearchResult.missing(target);
    }

    private static int chunkRadius(SeedSearchArea area) {
        return (area.radius() + 15) / 16;
    }

    private static <T extends Keyed> T resolve(Registry<T> registry, SeedFeature feature) {
        NamespacedKey key = NamespacedKey.fromString(feature.key());
        T value = key == null ? null : registry.get(key);
        if (value == null) throw new IllegalArgumentException("Unknown " + feature.kind() + " " + feature.key());
        return value;
    }

    private static Optional<SeedValidationResult.Position> position(Location found, SeedSearchArea area) {
        if (!area.contains(found.getX(), found.getZ())) return Optional.empty();
        return Optional.of(new SeedValidationResult.Position(found.getX(), found.getY(), found.getZ()));
    }
}
