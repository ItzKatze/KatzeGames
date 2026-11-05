package gg.itzkatze.katze.game.seed;

import java.util.Map;

public record SeedValidationResult(boolean passed, String missingRequirement, Map<SeedFeature, Position> found) {
    public record Position(double x, double y, double z) {}

    public SeedValidationResult {
        found = Map.copyOf(found);
    }
}
