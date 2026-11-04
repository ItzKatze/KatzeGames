package gg.itzkatze.katze.game;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LimboBiomes extends BiomeProvider implements Listener {

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo world, int x, int y, int z) {
        return Biome.PLAINS;
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo world) {
        return List.of(Biome.PLAINS);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk() && GameWorldManager.isLimbo(event.getWorld())) {
            apply(event.getChunk());
        }
    }

    static void apply(Chunk chunk) {
        World world = chunk.getWorld();
        for (int x = 0; x < 16; x += 4) {
            for (int z = 0; z < 16; z += 4) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y += 4) {
                    if (chunk.getBlock(x, y, z).getBiome() != Biome.PLAINS) {
                        chunk.getBlock(x, y, z).setBiome(Biome.PLAINS);
                    }
                }
            }
        }
    }
}
