package gg.itzkatze.katze.lobby.jumpandrun;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class BlockHighlights {

    private final Plugin plugin;
    private final Map<UUID, BlockDisplay> displays = new HashMap<>();

    BlockHighlights(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    void show(@NotNull Player player, @NotNull Cell target, @NotNull Color color) {
        remove(player.getUniqueId());
        World world = player.getWorld();
        Location location = new Location(world, target.x(), target.y(), target.z());
        BlockDisplay display = world.spawn(location, BlockDisplay.class, entity -> {
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
            entity.setBlock(world.getBlockAt(target.x(), target.y(), target.z()).getBlockData());
            entity.setTransformation(new Transformation(new Vector3f(-0.003f), new Quaternionf(),
                    new Vector3f(1.006f), new Quaternionf()));
            entity.setGlowing(true);
            entity.setGlowColorOverride(color);
            entity.getPersistentDataContainer().set(PlateDisplay.TAG, PersistentDataType.STRING, "target");
        });
        displays.put(player.getUniqueId(), display);
        player.showEntity(plugin, display);
    }

    void remove(@NotNull UUID player) {
        BlockDisplay display = displays.remove(player);
        if (display != null) display.remove();
    }
}
