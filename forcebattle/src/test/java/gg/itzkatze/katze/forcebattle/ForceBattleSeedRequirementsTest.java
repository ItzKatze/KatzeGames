package gg.itzkatze.katze.forcebattle;

import gg.itzkatze.katze.data.KatzeStructure;
import gg.itzkatze.katze.data.StructureSimplified;
import gg.itzkatze.katze.objective.ObjectiveFilter;
import gg.itzkatze.katze.game.seed.SeedFeature;
import gg.itzkatze.katze.game.seed.SeedRequirement;
import gg.itzkatze.katze.game.seed.SeedValidationResult;
import gg.itzkatze.katze.game.seed.SeedValidator;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ForceBattleSeedRequirementsTest {
    @Test
    void simplifiedFallbackNeedsOnlyOneCampVillageShipwreckOrOtherStructureVariant() {
        for (StructureSimplified group : StructureSimplified.values()) {
            ForceBattleSeedRequirements builder = new ForceBattleSeedRequirements(ObjectiveFilter.of(List.of(), new YamlConfiguration()), true);
            List<String> variants = group.getStructures().stream().map(member -> member.getKey().toString()).toList();
            builder.addStructureCoverage(variants);
            List<SeedRequirement> requirements = builder.build();
            SeedRequirement coverage = requirements.stream().filter(value -> value.objective().startsWith("coverage/structure/")).findFirst().orElseThrow();
            assertEquals(variants.size(), coverage.alternatives().size(), group.name());
            assertEquals(1, requirements.stream().filter(value -> value.objective().startsWith("coverage/structure/")).count(), group.name());
            for (String variant : variants) {
                assertTrue(validate(requirements, Set.copyOf(List.of(variant, "minecraft:stronghold", "minecraft:fortress"))), group.name() + ": " + variant);
            }
        }
    }

    @Test
    void exactFallbackStillRequiresEachCampVariant() {
        ForceBattleSeedRequirements builder = builder(new YamlConfiguration());
        List<String> variants = StructureSimplified.ABANDONED_CAMP.getStructures().stream()
                .map(member -> member.getKey().toString()).toList();
        builder.addStructureCoverage(variants);
        assertEquals(variants.size(), builder.build().size());
        assertFalse(validate(builder.build(), Set.of(variants.getFirst())));
        assertTrue(validate(builder.build(), Set.copyOf(variants)));
    }

    @Test
    void simplifiedFallbackRespectsExcludedGroupsAndMembersWithoutReaddingTheirVariants() {
        YamlConfiguration exclusions = new YamlConfiguration();
        exclusions.set("structures", List.of("ABANDONED_CAMP", "village_plains"));
        ForceBattleSeedRequirements builder = new ForceBattleSeedRequirements(ObjectiveFilter.of(List.of(), exclusions), true);
        List<String> available = new ArrayList<>(StructureSimplified.ABANDONED_CAMP.getStructures().stream()
                .map(member -> member.getKey().toString()).toList());
        available.add("minecraft:village_plains");
        available.add("minecraft:village_desert");
        builder.addStructureCoverage(available);
        assertEquals(1, builder.build().size());
        assertTrue(validate(builder.build(), Set.of("minecraft:village_desert")));
        assertFalse(validate(builder.build(), Set.of("minecraft:village_plains")));
    }

    @Test
    void simplifiedFallbackKeepsUngroupedFeaturesAndPreciseItemDependencies() {
        ForceBattleSeedRequirements builder = new ForceBattleSeedRequirements(ObjectiveFilter.of(List.of(), new YamlConfiguration()), true);
        builder.addStructureCoverage(List.of("minecraft:ocean_ruin_warm", "minecraft:ocean_ruin_cold", "minecraft:buried_treasure"));
        builder.add(Material.SNIFFER_EGG);
        assertFalse(validate(builder.build(), Set.of("minecraft:ocean_ruin_cold", "minecraft:buried_treasure")));
        assertFalse(validate(builder.build(), Set.of("minecraft:ocean_ruin_warm")));
        assertTrue(validate(builder.build(), Set.of("minecraft:ocean_ruin_warm", "minecraft:buried_treasure")));
    }

    @Test
    void totemCanComeFromAMansionWithoutAnOutpostOrVillage() {
        assertTrue(validateTotem(Set.of("minecraft:mansion")));
    }

    @Test
    void totemCanComeFromAnOutpostRaidWithAnyVillageVariant() {
        for (KatzeStructure village : StructureSimplified.VILLAGE.getStructures()) {
            assertTrue(validateTotem(Set.of("minecraft:pillager_outpost", village.getKey().toString())), village.name());
        }
    }

    @Test
    void outpostRaidNeedsBothTheOutpostAndAVillage() {
        assertFalse(validateTotem(Set.of("minecraft:pillager_outpost")));
        assertFalse(validateTotem(Set.of("minecraft:village_plains")));
        assertFalse(validateTotem(Set.of()));
    }

    private static boolean validateTotem(Set<String> availableStructures) {
        ForceBattleSeedRequirements builder = builder(new YamlConfiguration());
        builder.add(Material.TOTEM_OF_UNDYING);
        return validate(builder.build(), availableStructures);
    }

    private static boolean validate(List<SeedRequirement> requirements, Set<String> availableStructures) {
        SeedValidator validator = new SeedValidator(requirements, feature -> availableStructures.contains(feature.key())
                ? Optional.of(new SeedValidationResult.Position(0, 64, 0)) : Optional.empty());
        SeedValidationResult result;
        do {
            result = validator.step();
        } while (result == null);
        return result.passed();
    }

    @Test
    void simplifiedVillagesNeedOneAllowedVariant() {
        YamlConfiguration exclusions = new YamlConfiguration();
        exclusions.set("structures", List.of("village_plains", "village_desert"));
        ForceBattleSeedRequirements builder = builder(exclusions);
        builder.add(StructureSimplified.VILLAGE);

        List<SeedRequirement> requirements = builder.build();
        assertEquals(1, requirements.size());
        assertEquals(3, requirements.getFirst().alternatives().size());
        assertTrue(requirements.getFirst().alternatives().stream().noneMatch(feature ->
                feature.key().equals("minecraft:village_plains") || feature.key().equals("minecraft:village_desert")));
    }

    @Test
    void exactStructureRequirementsKeepTheCorrectDimension() {
        ForceBattleSeedRequirements builder = builder(new YamlConfiguration());
        builder.add(KatzeStructure.FORTRESS);
        SeedFeature feature = builder.build().getFirst().alternatives().getFirst();
        assertEquals(World.Environment.NETHER, feature.environment());
        assertEquals("minecraft:fortress", feature.key());
    }

    @Test
    void endObjectivesAlsoRequireAccessThroughAStrongholdAndFortress() {
        ForceBattleSeedRequirements builder = builder(new YamlConfiguration());
        builder.add(KatzeStructure.END_CITY);
        List<SeedRequirement> requirements = builder.build();
        assertEquals(3, requirements.size());
        assertTrue(requirements.stream().flatMap(value -> value.alternatives().stream())
                .anyMatch(feature -> feature.environment() == World.Environment.THE_END && feature.key().equals("minecraft:end_city")));
        assertTrue(requirements.stream().flatMap(value -> value.alternatives().stream())
                .anyMatch(feature -> feature.key().equals("minecraft:stronghold")));
        assertTrue(requirements.stream().flatMap(value -> value.alternatives().stream())
                .anyMatch(feature -> feature.key().equals("minecraft:fortress")));
    }

    @Test
    void blazeAndBlazeRodObjectivesRequireTheFortressEvenIfStructureObjectivesAreDisabled() {
        ForceBattleSeedRequirements builder = builder(new YamlConfiguration());
        builder.add(EntityType.BLAZE);
        builder.add(Material.BLAZE_ROD);
        assertFalse(builder.build().isEmpty());
        assertTrue(builder.build().stream().flatMap(value -> value.alternatives().stream())
                .allMatch(feature -> feature.key().equals("minecraft:fortress") && feature.environment() == World.Environment.NETHER));
    }

    @Test
    void heartOfTheSeaRequiresBuriedTreasureEvenThoughItIsNotAStructureObjective() {
        ForceBattleSeedRequirements builder = builder(new YamlConfiguration());
        builder.add(Material.HEART_OF_THE_SEA);
        SeedFeature feature = builder.build().getFirst().alternatives().getFirst();
        assertEquals("minecraft:buried_treasure", feature.key());
        assertEquals(World.Environment.NORMAL, feature.environment());
    }

    @Test
    void ordinaryResourceObjectivesDoNotRequireUnrelatedStructures() {
        ForceBattleSeedRequirements builder = builder(new YamlConfiguration());
        builder.add(Material.DIAMOND);
        assertTrue(builder.build().isEmpty());
    }

    private static ForceBattleSeedRequirements builder(YamlConfiguration exclusions) {
        return new ForceBattleSeedRequirements(ObjectiveFilter.of(List.of(), exclusions));
    }
}
