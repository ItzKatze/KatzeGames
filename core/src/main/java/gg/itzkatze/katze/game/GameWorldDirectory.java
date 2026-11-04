package gg.itzkatze.katze.game;

import org.bukkit.NamespacedKey;
import org.bukkit.Server;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

final class GameWorldDirectory {

    private final Path path;

    GameWorldDirectory(Server server, NamespacedKey key) throws IOException {
        this(server.getLevelDirectory(), key);
    }

    GameWorldDirectory(Path levelDirectory, NamespacedKey key) throws IOException {
        Path dimensions = levelDirectory.toRealPath().resolve("dimensions");
        path = dimensions.resolve(key.getNamespace()).resolve(key.getKey()).normalize();
        if (!key.getNamespace().equals("katzegames") || !path.startsWith(dimensions.resolve("katzegames"))
                || path.equals(dimensions.resolve("katzegames"))) {
            throw new IOException("Invalid game world directory: " + key);
        }
        validate();
    }

    void validateWorldPath(Path actual) throws IOException {
        validate();
        if (!actual.toRealPath().equals(path.toRealPath())) {
            throw new IOException("Unexpected game world directory: " + actual);
        }
    }

    private void validate() throws IOException {
        Path existing = path;
        while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) existing = existing.getParent();
        if (!existing.toRealPath().equals(existing)) {
            throw new IOException("Game world directory contains a symbolic link: " + path);
        }
    }

    void delete() throws IOException {
        validate();
        if (!Files.exists(path)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                if (exception != null) throw exception;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
