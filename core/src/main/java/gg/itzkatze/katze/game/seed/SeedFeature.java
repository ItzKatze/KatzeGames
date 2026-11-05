package gg.itzkatze.katze.game.seed;

import org.bukkit.World;

import java.util.Objects;

public record SeedFeature(Kind kind, World.Environment environment, String key) {
    public enum Kind { BIOME, STRUCTURE }

    public SeedFeature {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(environment);
        Objects.requireNonNull(key);
        if (key.isBlank()) throw new IllegalArgumentException("Empty feature key");
    }
}
