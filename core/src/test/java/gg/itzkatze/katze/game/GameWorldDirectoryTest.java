package gg.itzkatze.katze.game;

import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class GameWorldDirectoryTest {

    @TempDir Path primary;

    @Test
    void resolvesGameDimensionsFromTheLevelDirectoryOnPaper26() throws Exception {
        Path levelDirectory = primary.resolve("world");
        Path overworld = levelDirectory.resolve("dimensions/minecraft/overworld");
        Path game = levelDirectory.resolve("dimensions/katzegames/game");
        Path limbo = levelDirectory.resolve("dimensions/katzegames/limbo");
        Files.createDirectories(overworld.resolve("region"));
        Files.createDirectories(game.resolve("region"));
        Files.createDirectories(limbo);
        Files.writeString(overworld.resolve("region/r.0.0.mca"), "primary terrain");
        Files.writeString(game.resolve("region/r.0.0.mca"), "game terrain");
        Files.writeString(limbo.resolve("spawn.dat"), "spawn island");
        Files.writeString(levelDirectory.resolve("level.dat"), "level metadata");
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class}, (proxy, method, args) -> {
            if (method.getName().equals("getLevelDirectory")) return levelDirectory;
            throw new AssertionError("Unexpected server call: " + method.getName());
        });

        GameWorldDirectory directory = new GameWorldDirectory(server, new NamespacedKey("katzegames", "game"));
        directory.validateWorldPath(game);
        directory.delete();

        assertFalse(Files.exists(game));
        assertEquals("primary terrain", Files.readString(overworld.resolve("region/r.0.0.mca")));
        assertEquals("spawn island", Files.readString(limbo.resolve("spawn.dat")));
        assertEquals("level metadata", Files.readString(levelDirectory.resolve("level.dat")));
        assertFalse(Files.exists(overworld.resolve("dimensions")));
    }

    @Test
    void deletesOnlyTheSelectedGameDimension() throws Exception {
        Path game = primary.resolve("dimensions/katzegames/game");
        Files.createDirectories(game.resolve("region"));
        Files.writeString(game.resolve("region/r.0.0.mca"), "old terrain");
        Files.writeString(game.resolve("uid.dat"), "old identity");
        Path limbo = primary.resolve("dimensions/katzegames/limbo");
        Files.createDirectories(limbo);
        Files.writeString(limbo.resolve("spawn.dat"), "spawn island");
        Files.writeString(primary.resolve("level.dat"), "primary world");

        GameWorldDirectory directory = new GameWorldDirectory(primary, new NamespacedKey("katzegames", "game"));
        directory.validateWorldPath(game);
        directory.delete();
        directory.delete();

        assertFalse(Files.exists(game));
        assertEquals("spawn island", Files.readString(limbo.resolve("spawn.dat")));
        assertEquals("primary world", Files.readString(primary.resolve("level.dat")));
    }

    @Test
    void refusesPrimaryWorldAndOtherDimensionPaths() throws Exception {
        GameWorldDirectory directory = new GameWorldDirectory(primary, new NamespacedKey("katzegames", "game"));
        Files.createDirectories(primary.resolve("dimensions/katzegames/game"));
        assertThrows(IOException.class, () -> directory.validateWorldPath(primary));
        assertThrows(IOException.class, () -> new GameWorldDirectory(primary, NamespacedKey.minecraft("overworld")));
        assertThrows(IOException.class, () -> new GameWorldDirectory(primary, new NamespacedKey("katzegames", "../..")));
    }

    @Test
    void missingDimensionsAreSafeDuringInterruptedResetRecovery() throws Exception {
        GameWorldDirectory directory = new GameWorldDirectory(primary, new NamespacedKey("katzegames", "game_the_end"));
        directory.delete();
        assertTrue(Files.isDirectory(primary));
        assertFalse(Files.exists(primary.resolve("dimensions")));
    }
}
