package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.bingo.board.BingoBoardGenerator;
import gg.itzkatze.katze.bingo.board.BingoObjective;
import gg.itzkatze.katze.bingo.board.BingoObjectiveCatalog;
import gg.itzkatze.katze.data.KatzeAdvancement;
import gg.itzkatze.katze.data.KatzeStructure;
import gg.itzkatze.katze.data.MobsSimplified;
import gg.itzkatze.katze.data.StructureSimplified;
import gg.itzkatze.katze.objective.ObjectiveExclusion;
import gg.itzkatze.katze.objective.ObjectiveFilter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BingoObjectiveCatalogTest {
    @Test void completeCatalogIncludesAllInventoryItemsStructuresAndAdvancementsWithOneItemPerObjective() throws Exception {
        BingoObjectiveCatalog catalog = new BingoObjectiveCatalog();
        Set<String> items = targets(catalog, BingoObjective.Category.ITEM);
        try (var registry = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/bingo/item-registry.txt"), StandardCharsets.UTF_8))) {
            var registeredItems = registry.lines().toList();
            assertFalse(registeredItems.isEmpty());
            assertEquals(registeredItems.size(), items.size());
            for (String item : registeredItems) assertTrue(items.contains(item.toUpperCase(Locale.ROOT)), item);
        }
        Set<String> structures = targets(catalog, BingoObjective.Category.STRUCTURE);
        for (KatzeStructure structure : KatzeStructure.values()) assertTrue(structures.contains(structure.getKey().getKey()), structure.name());
        Set<String> advancements = targets(catalog, BingoObjective.Category.ADVANCEMENT);
        for (KatzeAdvancement advancement : KatzeAdvancement.values()) assertTrue(advancements.contains(advancement.getKey().getKey()), advancement.name());
        assertTrue(targets(catalog, BingoObjective.Category.BIOME).size() >= 67);
        assertTrue(targets(catalog, BingoObjective.Category.MOB).size() >= 80);
        assertTrue(catalog.all().stream().allMatch(objective -> objective.count() == 1));
    }

    @Test void exclusionGroupsAndConfigListsFilterBothExactAndSimplifiedObjectives() {
        BingoObjectiveCatalog catalog = new BingoObjectiveCatalog();
        YamlConfiguration config = new YamlConfiguration();
        config.set("mobs", List.of("COW"));
        config.set("biomes", List.of("minecraft:plains"));
        config.set("structures", List.of("village_plains"));
        config.set("items", List.of("minecraft:oak_log"));
        ObjectiveFilter filter = ObjectiveFilter.of(List.of(ObjectiveExclusion.UNOBTAINABLE, ObjectiveExclusion.ORES, ObjectiveExclusion.END), config);
        var exact = catalog.enabled(filter, false, EnumSet.allOf(BingoObjective.Category.class));
        assertFalse(exact.stream().anyMatch(objective -> objective.category() == BingoObjective.Category.ITEM && (objective.target().equals("OAK_LOG") || objective.target().endsWith("_ORE") || objective.target().equals("BARRIER") || objective.target().equals("ELYTRA"))));
        assertFalse(exact.stream().anyMatch(objective -> objective.category() == BingoObjective.Category.MOB && objective.target().equals("COW")));
        assertFalse(exact.stream().anyMatch(objective -> objective.id().startsWith("group_")));
        var grouped = catalog.enabled(filter, true, EnumSet.allOf(BingoObjective.Category.class));
        BingoObjective animals = grouped.stream().filter(objective -> objective.id().equals("group_mob_animal")).findFirst().orElseThrow();
        assertFalse(catalog.matches(animals, "COW"));
        assertTrue(catalog.matches(animals, "PIG"));
        BingoObjective villages = grouped.stream().filter(objective -> objective.id().equals("group_structure_village")).findFirst().orElseThrow();
        assertFalse(catalog.matches(villages, "village_plains"));
        assertTrue(catalog.matches(villages, "village_desert"));
        assertFalse(grouped.stream().anyMatch(objective -> objective.id().equals("group_biome_end")));
        config.set("mobs", List.of("ANIMAL"));
        assertFalse(catalog.enabled(ObjectiveFilter.of(List.of(ObjectiveExclusion.UNOBTAINABLE), config), true, EnumSet.allOf(BingoObjective.Category.class)).stream().anyMatch(objective -> objective.id().equals("group_mob_animal")));
    }

    @Test void simplifiedGroupsHaveTheSameMembersAsForceBattle() {
        BingoObjectiveCatalog catalog = new BingoObjectiveCatalog();
        for (MobsSimplified group : MobsSimplified.values()) {
            Set<String> expected = group.getMobs().stream().map(Enum::name).collect(Collectors.toSet());
            assertEquals(expected, Set.copyOf(List.of(catalog.get("group_mob_" + group.name().toLowerCase(Locale.ROOT)).target().split(","))));
        }
        for (StructureSimplified group : StructureSimplified.values()) {
            Set<String> expected = group.getStructures().stream().map(value -> value.getKey().getKey()).collect(Collectors.toSet());
            assertEquals(expected, Set.copyOf(List.of(catalog.get("group_structure_" + group.name().toLowerCase(Locale.ROOT)).target().split(","))));
        }
    }

    @Test void boardsWorkWithDefaultFiltersAndWithOnlyItemObjectives() {
        BingoObjectiveCatalog catalog = new BingoObjectiveCatalog();
        ObjectiveFilter filter = ObjectiveFilter.of(List.of(ObjectiveExclusion.values()), null);
        for (boolean simplified : new boolean[]{true, false}) {
            var pool = catalog.enabled(filter, simplified, EnumSet.allOf(BingoObjective.Category.class));
            for (int seed = 0; seed < 30; seed++) assertEquals(25, new BingoBoardGenerator().generate(pool, seed).size());
        }
        var items = catalog.enabled(filter, true, EnumSet.of(BingoObjective.Category.ITEM));
        assertEquals(25, new BingoBoardGenerator().generate(items, 123).size());
    }

    private Set<String> targets(BingoObjectiveCatalog catalog, BingoObjective.Category category) {
        return catalog.all().stream().filter(objective -> objective.category() == category).flatMap(objective -> Arrays.stream(objective.target().split(","))).collect(Collectors.toSet());
    }
}
