package gg.itzkatze.katze.game;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public final class VoidGenerator extends ChunkGenerator {

    @Override
    public @NotNull BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo world) {
        return new LimboBiomes();
    }

    @Override
    public int getBaseHeight(@NotNull WorldInfo world, @NotNull Random random, int x, int z, @NotNull HeightMap heightMap) {
        return world.getMinHeight();
    }

    @Override
    public @NotNull Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        return new Location(world, 0.5, 190, 0.5);
    }
}
