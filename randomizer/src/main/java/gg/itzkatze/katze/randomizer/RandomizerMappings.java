package gg.itzkatze.katze.randomizer;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class RandomizerMappings {

    private final List<Material> itemPool;
    private final Random random = new Random();
    private final Map<Material, Material> shared = new EnumMap<>(Material.class);
    private final Map<String, Map<Material, Material>> scoped = new HashMap<>();

    RandomizerMappings(List<Material> itemPool) {
        this.itemPool = List.copyOf(itemPool);
    }

    Material replace(String owner, Material source, Runnable onCreated) {
        Map<Material, Material> mapping = owner == null ? shared
                : scoped.computeIfAbsent(owner, ignored -> new EnumMap<>(Material.class));
        Material existing = mapping.get(source);
        if (existing != null) return existing;
        if (itemPool.isEmpty()) return source;
        int index = random.nextInt(itemPool.size());
        Material chosen = itemPool.get(index);
        if (chosen == source && itemPool.size() > 1) chosen = itemPool.get((index + 1) % itemPool.size());
        mapping.put(source, chosen);
        onCreated.run();
        return chosen;
    }

    void clear() {
        shared.clear();
        scoped.clear();
    }

    SaveData save() {
        SaveData data = new SaveData();
        data.itemMappings = serialize(shared);
        scoped.forEach((key, value) -> data.scopedMappings.put(key, serialize(value)));
        return data;
    }

    void load(SaveData data) {
        clear();
        deserialize(data.blockDropMap, shared);
        deserialize(data.mobDropMap, shared);
        deserialize(data.chestLootMap, shared);
        deserialize(data.itemMappings, shared);
        loadScoped(data.playerBlockDropMaps, "player:");
        loadScoped(data.playerMobDropMaps, "player:");
        loadScoped(data.playerChestLootMaps, "player:");
        loadScoped(data.scopedMappings, "");
    }

    private void loadScoped(Map<String, Map<String, String>> source, String prefix) {
        if (source == null) return;
        source.forEach((key, value) -> deserialize(value,
                scoped.computeIfAbsent(prefix + key, ignored -> new EnumMap<>(Material.class))));
    }

    private Map<String, String> serialize(Map<Material, Material> mapping) {
        Map<String, String> result = new HashMap<>();
        mapping.forEach((source, target) -> result.put(source.name(), target.name()));
        return result;
    }

    private void deserialize(Map<String, String> source, Map<Material, Material> target) {
        if (source == null) return;
        source.forEach((key, value) -> {
            Material original = Material.matchMaterial(key);
            Material replacement = Material.matchMaterial(value);
            if (original != null && replacement != null) target.put(original, replacement);
        });
    }

    public static class SaveData {
        public Map<String, String> itemMappings = new HashMap<>();
        public Map<String, Map<String, String>> scopedMappings = new HashMap<>();
        public Map<String, String> blockDropMap;
        public Map<String, String> mobDropMap;
        public Map<String, String> chestLootMap;
        public Map<String, Map<String, String>> playerBlockDropMaps;
        public Map<String, Map<String, String>> playerMobDropMaps;
        public Map<String, Map<String, String>> playerChestLootMaps;
    }
}
