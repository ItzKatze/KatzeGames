package gg.itzkatze.katze.game.seed;

import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class SeedValidatorTest {
    private static final SeedFeature VILLAGE = feature("village_plains");
    private static final SeedFeature DESERT_VILLAGE = feature("village_desert");
    private static final SeedFeature FORTRESS = new SeedFeature(SeedFeature.Kind.STRUCTURE, World.Environment.NETHER, "minecraft:fortress");
    private static final SeedValidationResult.Position POSITION = new SeedValidationResult.Position(12, 60, 30);

    @Test
    void simplifiedRequirementAcceptsAnyVariantAndChecksAtMostOneFeaturePerStep() {
        AtomicInteger calls = new AtomicInteger();
        SeedValidator validator = new SeedValidator(List.of(new SeedRequirement("village", List.of(VILLAGE, DESERT_VILLAGE))), value -> {
            calls.incrementAndGet();
            return value.equals(DESERT_VILLAGE) ? Optional.of(POSITION) : Optional.empty();
        });

        assertNull(validator.step());
        assertEquals(1, calls.get());
        SeedValidationResult result = validator.step();
        assertTrue(result.passed());
        assertEquals(2, calls.get());
        assertEquals(POSITION, result.found().get(DESERT_VILLAGE));
        assertSame(result, validator.step());
        assertEquals(2, calls.get());
    }

    @Test
    void allRequirementsMustPassAndTheFirstFailureStopsValidation() {
        AtomicInteger calls = new AtomicInteger();
        SeedValidator validator = new SeedValidator(List.of(
                new SeedRequirement("village", List.of(VILLAGE)),
                new SeedRequirement("blaze", List.of(FORTRESS)),
                new SeedRequirement("unused", List.of(DESERT_VILLAGE))), value -> {
            calls.incrementAndGet();
            return value.equals(VILLAGE) ? Optional.of(POSITION) : Optional.empty();
        });

        assertNull(validator.step());
        SeedValidationResult result = validator.step();
        assertFalse(result.passed());
        assertEquals("blaze", result.missingRequirement());
        assertEquals(2, calls.get());
        assertSame(result, validator.step());
        assertEquals(2, calls.get());
    }

    @Test
    void sharesSuccessfulAndFailedSearchesBetweenObjectives() {
        AtomicInteger calls = new AtomicInteger();
        SeedValidator validator = new SeedValidator(List.of(
                new SeedRequirement("village", List.of(VILLAGE, DESERT_VILLAGE)),
                new SeedRequirement("villager", List.of(VILLAGE, DESERT_VILLAGE))), value -> {
            calls.incrementAndGet();
            return value.equals(DESERT_VILLAGE) ? Optional.of(POSITION) : Optional.empty();
        });

        assertNull(validator.step());
        assertNull(validator.step());
        assertTrue(validator.step().passed());
        assertEquals(2, calls.get());
    }

    @Test
    void groupedSearchAcceptsTheLastAllowedVariantWithoutIndividualSearches() {
        List<SeedFeature> variants = IntStream.range(0, 18).mapToObj(index -> feature("camp_" + index)).toList();
        AtomicInteger calls = new AtomicInteger();
        SeedValidator validator = SeedValidator.batched(List.of(new SeedRequirement("camp", variants)), alternatives -> {
            calls.incrementAndGet();
            assertEquals(18, alternatives.size());
            return SeedSearchResult.found(alternatives.getLast(), POSITION);
        });

        assertTrue(validator.step().passed());
        assertEquals(1, calls.get());
    }

    @Test
    void anUnsuccessfulQuickProbeDoesNotRejectBeforeTheFullSearch() {
        AtomicInteger calls = new AtomicInteger();
        SeedValidator validator = SeedValidator.batched(List.of(new SeedRequirement("village", List.of(VILLAGE))), alternatives ->
                calls.incrementAndGet() == 1 ? SeedSearchResult.pending() : SeedSearchResult.found(VILLAGE, POSITION));

        assertNull(validator.step());
        assertTrue(validator.step().passed());
        assertEquals(2, calls.get());
    }

    @Test
    void groupedSearchCanRuleOutAllAlternativesInOneCall() {
        AtomicInteger calls = new AtomicInteger();
        SeedValidator validator = SeedValidator.batched(List.of(new SeedRequirement("village", List.of(VILLAGE, DESERT_VILLAGE))), alternatives -> {
            calls.incrementAndGet();
            return new SeedSearchResult(Map.of(), Set.copyOf(alternatives));
        });

        assertFalse(validator.step().passed());
        assertEquals(1, calls.get());
    }

    @Test
    void groupedSearchDoesNotTreatAnUnsearchedAlternativeAsMissing() {
        AtomicInteger calls = new AtomicInteger();
        SeedValidator validator = SeedValidator.batched(List.of(
                new SeedRequirement("first", List.of(VILLAGE, DESERT_VILLAGE)),
                new SeedRequirement("second", List.of(DESERT_VILLAGE, FORTRESS))), alternatives -> {
            calls.incrementAndGet();
            return SeedSearchResult.found(alternatives.getFirst(), POSITION);
        });

        assertNull(validator.step());
        assertTrue(validator.step().passed());
        assertEquals(2, calls.get());
        assertEquals(2, validator.step().found().size());
    }

    @Test
    void rejectsUnexpectedFeaturesFromASearch() {
        SeedValidator validator = SeedValidator.batched(List.of(new SeedRequirement("village", List.of(VILLAGE))),
                alternatives -> SeedSearchResult.found(FORTRESS, POSITION));
        assertThrows(IllegalStateException.class, validator::step);
    }

    @Test
    void dimensionAndFeatureKindArePartOfTheSearchIdentity() {
        SeedFeature netherVillage = new SeedFeature(VILLAGE.kind(), World.Environment.NETHER, VILLAGE.key());
        SeedFeature biomeVillage = new SeedFeature(SeedFeature.Kind.BIOME, VILLAGE.environment(), VILLAGE.key());
        AtomicInteger calls = new AtomicInteger();
        SeedValidator validator = new SeedValidator(List.of(new SeedRequirement("alternatives", List.of(VILLAGE, netherVillage, biomeVillage))), value -> {
            calls.incrementAndGet();
            return value.equals(biomeVillage) ? Optional.of(POSITION) : Optional.empty();
        });

        assertNull(validator.step());
        assertNull(validator.step());
        assertTrue(validator.step().passed());
        assertEquals(3, calls.get());
    }

    @Test
    void aNewCandidateDoesNotReusePreviousCandidateSearches() {
        List<SeedRequirement> requirements = List.of(new SeedRequirement("village", List.of(VILLAGE)));
        assertFalse(new SeedValidator(requirements, value -> Optional.empty()).step().passed());
        assertTrue(new SeedValidator(requirements, value -> Optional.of(POSITION)).step().passed());
    }

    @Test
    void emptyRequirementsPassWithoutAWorldSearch() {
        assertTrue(new SeedValidator(List.of(), value -> { throw new AssertionError("Unexpected search"); }).step().passed());
    }

    @Test
    void invalidRequirementsFailBeforeSearching() {
        assertThrows(IllegalArgumentException.class, () -> new SeedRequirement("village", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SeedRequirement("", List.of(VILLAGE)));
    }

    private static SeedFeature feature(String key) {
        return new SeedFeature(SeedFeature.Kind.STRUCTURE, World.Environment.NORMAL, "minecraft:" + key);
    }
}
