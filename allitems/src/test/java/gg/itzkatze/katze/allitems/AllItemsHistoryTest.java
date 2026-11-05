package gg.itzkatze.katze.allitems;

import gg.itzkatze.katze.allitems.data.AllItemsData;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AllItemsHistoryTest {
    @Test
    void archiveUsesTheFullCollectionRatherThanTheLastTwoHundredFinds() throws Exception {
        AllItemsData data = new AllItemsData();
        UUID player = UUID.randomUUID();
        Set<Material> items = Set.copyOf(Arrays.asList(Material.values()).subList(0, 250));
        playerItems(data).put(player, items);

        List<AllItemsObjective> snapshot = data.getRecordedDiscoveries(player);
        data.clearAllProgress();

        assertEquals(250, snapshot.size());
        assertTrue(snapshot.stream().allMatch(objective -> objective.type() == AllItemsObjectiveType.ITEM));
        assertTrue(data.getRecordedDiscoveries(player).isEmpty());
    }

    @Test
    void personalContributionsRemainSeparateFromTheSharedCollection() throws Exception {
        AllItemsData data = new AllItemsData();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        playerItems(data).put(first, Set.of(Material.STONE));
        playerItems(data).put(second, Set.of(Material.DIAMOND));
        sharedItems(data).put(Material.STONE, new AllItemsData.FoundEntry("First", first, LocalDateTime.now()));
        sharedItems(data).put(Material.DIAMOND, new AllItemsData.FoundEntry("Second", second, LocalDateTime.now()));

        assertEquals(List.of("STONE"), data.getRecordedDiscoveries(first).stream().map(AllItemsObjective::key).toList());
        assertEquals(List.of("DIAMOND"), data.getRecordedDiscoveries(second).stream().map(AllItemsObjective::key).toList());
        assertEquals(2, data.getRecordedDiscoveries(null).size());
        assertTrue(data.getRecordedDiscoveries(UUID.randomUUID()).isEmpty());
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Set<Material>> playerItems(AllItemsData data) throws Exception {
        var field = AllItemsData.class.getDeclaredField("playerFoundItems");
        field.setAccessible(true);
        return (Map<UUID, Set<Material>>) field.get(data);
    }

    @SuppressWarnings("unchecked")
    private Map<Material, AllItemsData.FoundEntry> sharedItems(AllItemsData data) throws Exception {
        var field = AllItemsData.class.getDeclaredField("teamFoundItems");
        field.setAccessible(true);
        return (Map<Material, AllItemsData.FoundEntry>) field.get(data);
    }
}
