package gg.itzkatze.katze.bingo.board;

import gg.itzkatze.katze.bingo.BingoSettings;
import gg.itzkatze.katze.objective.ObjectiveFilter;
import gg.itzkatze.katze.game.seed.SeedFeature;
import gg.itzkatze.katze.game.seed.SeedRequirement;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.HexFormat;
import java.util.Properties;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class BingoObjectiveCatalog {
    private final Map<String, BingoObjective> objectives = new LinkedHashMap<>();
    private final Properties icons = new Properties();
    private final Map<String, String> modes = new LinkedHashMap<>();
    private final Map<String, String> names = new LinkedHashMap<>();
    private final Map<String, List<String>> activeTargets = new LinkedHashMap<>();

    public BingoObjectiveCatalog() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/bingo/objectives.tsv")), StandardCharsets.UTF_8))) {
            for (String line : reader.lines().toList()) {
                String[] fields = line.split("\t", -1);
                BingoObjective objective = new BingoObjective(fields[0], BingoObjective.Category.valueOf(fields[1]), Difficulty.valueOf(fields[2]),
                        fields[3], 1, fields[5], fields[6], Integer.parseInt(fields[7]), fields[8], fields[9], fields[10]);
                if (objectives.putIfAbsent(objective.id(), objective) != null) throw new IllegalStateException("Duplicate Bingo objective");
                modes.put(objective.id(), fields.length > 13 ? fields[13] : "ALWAYS");
                if (fields[11].startsWith("@")) names.put(objective.id(), fields[11].substring(1));
            }
        } catch (Exception exception) { throw new IllegalStateException("Unable to load Bingo objectives", exception); }
        try (var stream = Objects.requireNonNull(getClass().getResourceAsStream("/bingo/icons.properties"))) {
            icons.load(stream);
            for (String id : objectives.keySet()) Material.valueOf(Objects.requireNonNull(icons.getProperty(id), "Missing Bingo icon: " + id));
        } catch (Exception exception) { throw new IllegalStateException("Unable to load Bingo icons", exception); }
    }

    public BingoObjective get(String id) {
        BingoObjective objective = objectives.get(id);
        if (objective == null) throw new IllegalArgumentException("Unknown Bingo objective: " + id);
        return objective;
    }

    public List<BingoObjective> all() { return List.copyOf(objectives.values()); }

    public String fingerprint() {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(all().toString().getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    public List<BingoObjective> enabled() {
        return enabled(BingoSettings.filter(), BingoSettings.simplified(), Arrays.stream(BingoObjective.Category.values()).filter(BingoSettings::categoryEnabled).collect(Collectors.toSet()));
    }

    public List<BingoObjective> enabled(ObjectiveFilter filter, boolean simplified, Set<BingoObjective.Category> categories) {
        activeTargets.clear();
        return all().stream().filter(objective -> categories.contains(objective.category())).filter(objective -> {
            String mode = modes.get(objective.id());
            if (mode.equals("LEGACY") || mode.equals("EXACT") && simplified || mode.equals("SIMPLIFIED") && !simplified) return false;
            if (mode.equals("SIMPLIFIED")) {
                String category = objective.category().name().toLowerCase(Locale.ROOT);
                if (filter.excludesGroup(category.equals("mob") ? "mobs" : category.equals("biome") ? "biomes" : "structures", objective.id().substring(("group_" + category + "_").length()))) return false;
            }
            List<String> targets = Arrays.stream(objective.target().split(",")).filter(target -> !excluded(filter, objective.category(), target)).toList();
            if (targets.isEmpty()) return false;
            activeTargets.put(objective.id(), targets);
            return true;
        }).toList();
    }

    private boolean excluded(ObjectiveFilter filter, BingoObjective.Category category, String target) {
        return switch (category) {
            case ITEM -> filter.excludesInventoryItem(Material.valueOf(target));
            case MOB -> filter.isExcluded(EntityType.valueOf(target));
            case BIOME -> filter.excludesBiome(target);
            case STRUCTURE -> filter.excludesStructure(target);
            case ADVANCEMENT -> filter.excludesAdvancement(target);
        };
    }

    public boolean matches(BingoObjective objective, String target) {
        return activeTargets.getOrDefault(objective.id(), List.of(objective.target().split(","))).contains(target);
    }

    public List<String> targets(BingoObjective objective) { return activeTargets.getOrDefault(objective.id(), List.of(objective.target().split(","))); }

    public Component name(String id) {
        return names.containsKey(id) ? Component.translatable("katze.bingo.objective." + id, TextArg.component("target", Component.translatable(names.get(id))))
                : Component.translatable("katze.bingo.objective." + id);
    }

    public Component difficulty(String id) {
        return Component.translatable("katze.bingo.difficulty." + get(id).difficulty().name().toLowerCase(Locale.ROOT))
                .color(switch (get(id).difficulty()) {
                    case EASY -> KatzeColor.LIME;
                    case MEDIUM -> KatzeColor.GOLD;
                    case HARD -> KatzeColor.RED;
                });
    }

    public Component progress(String id, int count) {
        return Component.translatable("katze.bingo.progress", TextArg.number("current", count), TextArg.number("target", get(id).count()));
    }

    public Material icon(String id) {
        get(id);
        return Material.valueOf(icons.getProperty(id));
    }


    public List<SeedRequirement> requirements(List<String> ids) {
        List<SeedRequirement> result = new ArrayList<>();
        for (String id : ids) {
            BingoObjective objective = get(id);
            if (objective.feature().isBlank()) continue;
            String[] feature = objective.feature().split(":", 2);
            SeedFeature.Kind kind = SeedFeature.Kind.valueOf(feature[0]);
            List<SeedFeature> alternatives = List.of(feature[1].split(",")).stream()
                    .map(key -> new SeedFeature(kind, World.Environment.valueOf(objective.dimension()), NamespacedKey.minecraft(key).toString())).toList();
            result.add(new SeedRequirement("bingo/" + id, alternatives));
        }
        return result;
    }
}
