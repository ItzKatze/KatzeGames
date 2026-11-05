package gg.itzkatze.katze.game.seed;

import java.util.List;

public record SeedRequirement(String objective, List<SeedFeature> alternatives) {
    public SeedRequirement {
        if (objective == null || objective.isBlank()) throw new IllegalArgumentException("Missing objective name");
        alternatives = alternatives.stream().distinct().toList();
        if (alternatives.isEmpty()) throw new IllegalArgumentException("No feature alternatives for " + objective);
    }
}
