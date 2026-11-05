package gg.itzkatze.katze.game.seed;

import java.util.Map;
import java.util.Set;

public record SeedSearchResult(Map<SeedFeature, SeedValidationResult.Position> found, Set<SeedFeature> missing) {
    public SeedSearchResult {
        found = Map.copyOf(found);
        missing = Set.copyOf(missing);
        if (missing.stream().anyMatch(found::containsKey)) throw new IllegalArgumentException("Conflicting feature results");
    }

    public static SeedSearchResult pending() {
        return new SeedSearchResult(Map.of(), Set.of());
    }

    public static SeedSearchResult found(SeedFeature feature, SeedValidationResult.Position position) {
        return new SeedSearchResult(Map.of(feature, position), Set.of());
    }

    public static SeedSearchResult missing(SeedFeature feature) {
        return new SeedSearchResult(Map.of(), Set.of(feature));
    }
}
