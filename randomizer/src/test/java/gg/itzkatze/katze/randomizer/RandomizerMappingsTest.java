package gg.itzkatze.katze.randomizer;

import com.google.gson.Gson;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RandomizerMappingsTest {

    @Test void theSameOriginalItemReusesOneMapping() {
        RandomizerMappings mappings = new RandomizerMappings(List.of(Material.DIAMOND));
        AtomicInteger saves = new AtomicInteger();

        Material minedCoal = mappings.replace(null, Material.COAL, saves::incrementAndGet);
        Material chestCoal = mappings.replace(null, Material.COAL, saves::incrementAndGet);
        Material mobCoal = mappings.replace(null, Material.COAL, saves::incrementAndGet);

        assertEquals(Material.DIAMOND, minedCoal);
        assertEquals(minedCoal, chestCoal);
        assertEquals(minedCoal, mobCoal);
        assertEquals(1, saves.get());
    }

    @Test void teamsAndSoloPlayersHaveIndependentPersistentMappings() {
        RandomizerMappings.SaveData data = new RandomizerMappings.SaveData();
        data.itemMappings.put("COAL", "GOLD_INGOT");
        data.scopedMappings.put("team:1", Map.of("COAL", "DIAMOND"));
        data.scopedMappings.put("team:2", Map.of("COAL", "EMERALD"));
        data.scopedMappings.put("player:solo", Map.of("COAL", "IRON_INGOT"));
        RandomizerMappings original = new RandomizerMappings(List.of());
        original.load(data);
        Gson gson = new Gson();
        RandomizerMappings restored = new RandomizerMappings(List.of());
        restored.load(gson.fromJson(gson.toJson(original.save()), RandomizerMappings.SaveData.class));

        assertEquals(Material.GOLD_INGOT, replace(restored, null, Material.COAL));
        assertEquals(Material.DIAMOND, replace(restored, "team:1", Material.COAL));
        assertEquals(Material.DIAMOND, replace(restored, "team:1", Material.COAL));
        assertEquals(Material.EMERALD, replace(restored, "team:2", Material.COAL));
        assertEquals(Material.IRON_INGOT, replace(restored, "player:solo", Material.COAL));
    }

    @Test void legacySourceMapsAreMergedWithChestMappingsTakingPrecedence() {
        RandomizerMappings.SaveData data = new RandomizerMappings.SaveData();
        data.blockDropMap = Map.of("DIRT", "GOLD_INGOT");
        data.mobDropMap = Map.of("COAL", "IRON_INGOT");
        data.chestLootMap = Map.of("COAL", "DIAMOND");
        data.playerChestLootMaps = Map.of("solo", Map.of("COAL", "EMERALD"));
        RandomizerMappings mappings = new RandomizerMappings(List.of());
        mappings.load(data);

        assertEquals(Material.GOLD_INGOT, replace(mappings, null, Material.DIRT));
        assertEquals(Material.DIAMOND, replace(mappings, null, Material.COAL));
        assertEquals(Material.EMERALD, replace(mappings, "player:solo", Material.COAL));
    }

    @Test void resetClearsEveryMappingScope() {
        RandomizerMappings mappings = new RandomizerMappings(List.of(Material.DIAMOND));
        for (String owner : List.of("team:1", "team:2", "player:solo")) {
            mappings.replace(owner, Material.COAL, () -> {});
        }
        mappings.replace(null, Material.COAL, () -> {});

        mappings.clear();

        assertTrue(mappings.save().itemMappings.isEmpty());
        assertTrue(mappings.save().scopedMappings.isEmpty());
    }

    @Test void itemsNeverMapToThemselvesWhenAlternativesExist() {
        RandomizerMappings mappings = new RandomizerMappings(List.of(Material.COAL, Material.DIAMOND));
        assertEquals(Material.DIAMOND, mappings.replace(null, Material.COAL, () -> {}));
        assertEquals(Material.COAL, mappings.replace(null, Material.DIAMOND, () -> {}));
    }

    private Material replace(RandomizerMappings mappings, String owner, Material source) {
        return mappings.replace(owner, source, () -> fail("An existing mapping was replaced"));
    }
}
