package gg.itzkatze.katze.game.seed;

import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class SeedRequirementOptimizerTest {
    private static final List<SeedFeature> FEATURES = IntStream.range(0, 6).mapToObj(index ->
            new SeedFeature(SeedFeature.Kind.STRUCTURE, World.Environment.NORMAL, "minecraft:feature_" + index)).toList();

    @Test
    void removesRepeatedDependenciesWithDifferentObjectiveNamesOrAlternativeOrder() {
        List<SeedRequirement> optimized = SeedRequirementOptimizer.optimize(List.of(
                requirement("village", 0, 1), requirement("villager", 1, 0), requirement("cat", 0, 1)));
        assertEquals(1, optimized.size());
    }

    @Test
    void anExactDependencyAlsoSatisfiesBroaderSimplifiedGroups() {
        List<SeedRequirement> optimized = SeedRequirementOptimizer.optimize(List.of(
                requirement("village", 0, 1, 2), requirement("raid", 0, 1), requirement("specific_resource", 0)));
        assertEquals(List.of(requirement("specific_resource", 0)), optimized);
    }

    @Test
    void overlappingGroupsRemainSeparateWhenNeitherImpliesTheOther() {
        assertEquals(2, SeedRequirementOptimizer.optimize(List.of(requirement("one", 0, 1), requirement("two", 1, 2))).size());
    }

    @Test
    void optimizationPreservesAcceptanceForEveryCombinationOfAvailableFeatures() {
        List<SeedRequirement> requirements = new ArrayList<>();
        for (int mask = 1; mask < 64; mask++) {
            int selected = mask;
            if (Integer.bitCount(selected) >= 3) {
                requirements.add(new SeedRequirement("group_" + mask, IntStream.range(0, 6)
                        .filter(index -> (selected & (1 << index)) != 0).mapToObj(FEATURES::get).toList()));
            }
        }
        requirements.add(requirement("specific", 0, 1));
        List<SeedRequirement> optimized = SeedRequirementOptimizer.optimize(requirements);
        assertTrue(optimized.size() < requirements.size());
        for (int mask = 0; mask < 64; mask++) {
            int selected = mask;
            Set<SeedFeature> available = IntStream.range(0, 6).filter(index -> (selected & (1 << index)) != 0)
                    .mapToObj(FEATURES::get).collect(Collectors.toSet());
            assertEquals(accepts(requirements, available), accepts(optimized, available), "Available features: " + mask);
        }
    }

    private static boolean accepts(List<SeedRequirement> requirements, Set<SeedFeature> available) {
        return requirements.stream().allMatch(requirement -> requirement.alternatives().stream().anyMatch(available::contains));
    }

    private static SeedRequirement requirement(String name, int... indices) {
        return new SeedRequirement(name, IntStream.of(indices).mapToObj(FEATURES::get).toList());
    }
}
