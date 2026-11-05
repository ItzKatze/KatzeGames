package gg.itzkatze.katze.game.seed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class SeedValidator {
    private final List<SeedRequirement> requirements;
    private final SeedFeatureSearch search;
    private final Map<SeedFeature, Optional<SeedValidationResult.Position>> cache = new HashMap<>();
    private int requirementIndex;
    private SeedValidationResult result;

    public SeedValidator(List<SeedRequirement> requirements,
                         Function<SeedFeature, Optional<SeedValidationResult.Position>> search) {
        this(requirements, (SeedFeatureSearch) alternatives -> {
            SeedFeature feature = alternatives.getFirst();
            return search.apply(feature).map(position -> SeedSearchResult.found(feature, position))
                    .orElseGet(() -> SeedSearchResult.missing(feature));
        });
    }

    private SeedValidator(List<SeedRequirement> requirements, SeedFeatureSearch search) {
        this.requirements = List.copyOf(requirements);
        this.search = search;
    }

    public static SeedValidator batched(List<SeedRequirement> requirements, SeedFeatureSearch search) {
        return new SeedValidator(SeedRequirementOptimizer.optimize(requirements), search);
    }

    public SeedValidationResult step() {
        if (result != null) return result;
        if (requirementIndex == requirements.size()) return finish(true, null);
        SeedRequirement requirement = requirements.get(requirementIndex);
        if (isSatisfied(requirement)) return advance();
        List<SeedFeature> remaining = requirement.alternatives().stream().filter(feature -> !cache.containsKey(feature)).toList();
        if (remaining.isEmpty()) return finish(false, requirement.objective());
        SeedSearchResult searched = search.find(remaining);
        if (!remaining.containsAll(searched.found().keySet()) || !remaining.containsAll(searched.missing())) {
            throw new IllegalStateException("Search returned unrequested features");
        }
        searched.found().forEach((feature, position) -> cache.put(feature, Optional.of(position)));
        searched.missing().forEach(feature -> cache.put(feature, Optional.empty()));
        if (isSatisfied(requirement)) return advance();
        if (requirement.alternatives().stream().allMatch(cache::containsKey)) return finish(false, requirement.objective());
        return null;
    }

    private boolean isSatisfied(SeedRequirement requirement) {
        return requirement.alternatives().stream().anyMatch(feature -> cache.getOrDefault(feature, Optional.empty()).isPresent());
    }

    private SeedValidationResult advance() {
        requirementIndex++;
        return requirementIndex == requirements.size() ? finish(true, null) : null;
    }

    private SeedValidationResult finish(boolean passed, String missing) {
        Map<SeedFeature, SeedValidationResult.Position> found = new HashMap<>();
        cache.forEach((feature, position) -> position.ifPresent(value -> found.put(feature, value)));
        result = new SeedValidationResult(passed, missing, found);
        return result;
    }
}
