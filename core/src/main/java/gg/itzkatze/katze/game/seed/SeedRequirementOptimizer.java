package gg.itzkatze.katze.game.seed;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class SeedRequirementOptimizer {
    private SeedRequirementOptimizer() {}

    public static List<SeedRequirement> optimize(List<SeedRequirement> requirements) {
        List<SeedRequirement> optimized = new ArrayList<>();
        List<Set<SeedFeature>> retained = new ArrayList<>();
        for (SeedRequirement requirement : requirements.stream()
                .sorted(Comparator.comparingInt(value -> value.alternatives().size())).toList()) {
            Set<SeedFeature> alternatives = Set.copyOf(requirement.alternatives());
            if (retained.stream().anyMatch(alternatives::containsAll)) continue;
            optimized.add(requirement);
            retained.add(alternatives);
        }
        return List.copyOf(optimized);
    }
}
