import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

final class BuildObjectives {
    private final Path module;
    private final MinecraftAssets assets;
    private final ZipFile archive;
    private final Map<String, String[]> entries = new LinkedHashMap<>();
    private final Set<String> targets = new HashSet<>();
    private final Set<String> items = new HashSet<>();
    private final JsonObject language;

    private BuildObjectives(Path module, ZipFile archive) throws IOException {
        this.module = module;
        this.archive = archive;
        assets = new MinecraftAssets(archive);
        language = assets.json("assets/minecraft/lang/en_us.json");
    }

    static void generate(Path module, ZipFile archive, Path exports) throws IOException {
        new BuildObjectives(module, archive).generate(exports);
    }

    private void generate(Path exports) throws IOException {
        var manifest = JsonParser.parseString(Files.readString(exports.resolve("manifest.json"))).getAsJsonObject();
        manifest.getAsJsonArray("items").forEach(item -> items.add(item.getAsString().substring("minecraft:".length())));
        Files.write(module.resolve("src/main/resources/bingo/item-registry.txt"), items.stream().sorted().toList(), StandardCharsets.UTF_8);
        Path base = module.resolve("src/main/resources/bingo/objectives-base.tsv");
        boolean hasBase = Files.isRegularFile(base);
        for (String line : Files.readAllLines(hasBase ? base : module.resolve("src/main/resources/bingo/objectives.tsv"), StandardCharsets.UTF_8)) {
            String[] row = Arrays.copyOf(line.split("\t", -1), 14);
            if (!hasBase && row[11].startsWith("@")) continue;
            row[4] = "1";
            row[7] = effort(row[2]);
            if (row[13] == null) row[13] = row[3].contains(",") ? "LEGACY" : mode(row[1]);
            entries.put(row[0], row);
            if (!row[13].equals("SIMPLIFIED") && !row[13].equals("LEGACY")) targets.add(row[1] + ":" + row[3]);
        }
        for (String item : items.stream().sorted().toList()) {
            String translation = language.has("block.minecraft." + item) ? "block.minecraft." + item : "item.minecraft." + item;
            add("ITEM", item.toUpperCase(Locale.ROOT), item, itemDifficulty(item), translation, "ALWAYS", "NORMAL", "");
        }
        for (String egg : items.stream().filter(item -> item.endsWith("_spawn_egg")).sorted().toList()) {
            String mob = egg.substring(0, egg.length() - "_spawn_egg".length());
            add("MOB", mob.toUpperCase(Locale.ROOT), egg, mobDifficulty(mob), "entity.minecraft." + mob, "EXACT", dimension(mob), "");
        }
        for (String biome : resources("worldgen/biome")) {
            String dimension = dimension(biome);
            String icon = biomeIcon(biome);
            add("BIOME", biome, icon, biomeDifficulty(biome), "biome.minecraft." + biome, "EXACT", dimension, "BIOME:" + biome);
        }
        for (String structure : resources("worldgen/structure")) {
            String icon = structureIcon(structure);
            add("STRUCTURE", structure, icon, structureDifficulty(structure), "katze.objective.structure_detail." + structure, "EXACT", dimension(structure), "STRUCTURE:" + structure);
        }
        for (String advancement : resources("advancement")) {
            JsonObject definition = assets.advancement(advancement);
            if (!definition.has("display")) continue;
            JsonObject display = definition.getAsJsonObject("display");
            String icon = display.getAsJsonObject("icon").get("id").getAsString().replace("minecraft:", "");
            String tier = display.has("frame") && display.get("frame").getAsString().equals("challenge") || advancement.startsWith("end/") ? "HARD" : "MEDIUM";
            if (advancement.contains("root") || Set.of("story/mine_stone", "story/upgrade_tools", "story/smelt_iron", "husbandry/plant_seed").contains(advancement)) tier = "EASY";
            if (advancement.startsWith("nether/") || advancement.startsWith("end/")) tier = "HARD";
            String title = display.getAsJsonObject("title").get("translate").getAsString();
            add("ADVANCEMENT", advancement, icon, tier, title, "ALWAYS", advancement.startsWith("end/") ? "THE_END" : advancement.startsWith("nether/") ? "NETHER" : "NORMAL", "");
        }
        groups("MobsSimplified", "MOB", "EntityType");
        groups("BiomeSimplified", "BIOME", "Biome");
        groups("StructureSimplified", "STRUCTURE", "KatzeStructure");
        Files.write(module.resolve("src/main/resources/bingo/objectives.tsv"), entries.values().stream().map(row -> String.join("\t", row)).toList(), StandardCharsets.UTF_8);
        System.out.println("Bingo catalog: " + entries.size() + " objectives (including exact and simplified variants)");
    }

    private void groups(String source, String category, String memberType) throws IOException {
        Path file = module.getParent().resolve("core/src/main/java/gg/itzkatze/katze/data/" + source + ".java");
        var pattern = Pattern.compile("([A-Z_]+)\\(\"[^\"]+\", new GUIMaterial\\(Material\\.([A-Z_]+)\\), List\\.of\\((.*?)\\)\\)", Pattern.DOTALL);
        var matcher = pattern.matcher(Files.readString(file));
        int found = 0;
        while (matcher.find()) {
            found++;
            String name = matcher.group(1).toLowerCase(Locale.ROOT);
            List<String> members = new ArrayList<>();
            var member = Pattern.compile(memberType + "\\.([A-Z_]+)").matcher(matcher.group(3));
            while (member.find()) members.add(category.equals("MOB") ? member.group(1) : member.group(1).toLowerCase(Locale.ROOT));
            String icon = matcher.group(2).toLowerCase(Locale.ROOT);
            if (category.equals("MOB")) icon = members.stream().map(value -> value.toLowerCase(Locale.ROOT) + "_spawn_egg").filter(items::contains).findFirst().orElseThrow();
            String tier = category.equals("BIOME") ? biomeDifficulty(name) : category.equals("STRUCTURE") ? structureDifficulty(name) : mobDifficulty(name);
            String dimension = dimension(name);
            String feature = category.equals("MOB") ? "" : category + ":" + String.join(",", members);
            add(category, String.join(",", members), icon, tier, "katze.objective." + category.toLowerCase(Locale.ROOT) + "." + name, "SIMPLIFIED", dimension, feature, "group_" + category.toLowerCase(Locale.ROOT) + "_" + name);
        }
        if (found == 0) throw new IOException("No simplified groups found in " + file);
    }

    private List<String> resources(String directory) {
        String prefix = "data/minecraft/" + directory + "/";
        return archive.stream().map(entry -> entry.getName()).filter(name -> name.startsWith(prefix) && name.endsWith(".json"))
                .map(name -> name.substring(prefix.length(), name.length() - 5)).sorted().toList();
    }

    private void add(String category, String target, String icon, String tier, String translation, String mode, String dimension, String feature) {
        add(category, target, icon, tier, translation, mode, dimension, feature, category.toLowerCase(Locale.ROOT) + "_" + target.toLowerCase(Locale.ROOT).replace('/', '_'));
    }

    private void add(String category, String target, String icon, String tier, String translation, String mode, String dimension, String feature, String id) {
        if (!mode.equals("SIMPLIFIED") && !targets.add(category + ":" + target)) return;
        if (!items.contains(icon)) throw new IllegalStateException("Missing native item for " + id + ": " + icon);
        entries.put(id, new String[]{id, category, tier, target, "1", icon.toUpperCase(Locale.ROOT), "", effort(tier), "", feature, dimension, "@" + translation, "@" + translation, mode});
    }

    private String biomeIcon(String biome) {
        String name = biome.toLowerCase(Locale.ROOT);
        if (name.contains("ocean") || name.contains("river")) return "water_bucket";
        if (name.contains("desert") || name.contains("beach")) return "sand";
        if (name.contains("badlands")) return "red_sand";
        if (name.contains("end")) return "end_stone";
        if (name.contains("mushroom")) return "red_mushroom";
        if (name.contains("dark")) return "sculk";
        if (name.contains("cave")) return "moss_block";
        for (String tree : List.of("birch", "cherry", "jungle", "mangrove", "spruce", "pale", "crimson", "warped")) {
            if (name.contains(tree)) return tree.equals("pale") ? "pale_oak_sapling" : tree.equals("mangrove") ? "mangrove_propagule" : items.contains(tree + "_sapling") ? tree + "_sapling" : tree + "_fungus";
        }
        if (name.contains("snow") || name.contains("frozen") || name.contains("ice")) return "snow_block";
        if (name.contains("nether")) return "netherrack";
        if (name.contains("soul")) return "soul_sand";
        if (name.contains("basalt")) return "basalt";
        return "grass_block";
    }

    private String structureIcon(String name) {
        if (name.startsWith("village")) return "bell";
        if (name.startsWith("abandoned_camp")) return "campfire";
        if (name.startsWith("ruined_portal")) return "crying_obsidian";
        if (name.startsWith("shipwreck")) return "oak_boat";
        if (name.startsWith("mineshaft")) return "rail";
        if (name.startsWith("ocean_ruin")) return "mossy_stone_bricks";
        return switch (name) {
            case "fortress" -> "nether_bricks";
            case "bastion_remnant" -> "gilded_blackstone";
            case "desert_pyramid" -> "chiseled_sandstone";
            case "jungle_pyramid" -> "mossy_cobblestone";
            case "ancient_city" -> "sculk_shrieker";
            case "end_city" -> "end_stone_bricks";
            case "stronghold" -> "ender_eye";
            case "monument" -> "prismarine";
            case "igloo" -> "snow_block";
            case "swamp_hut" -> "cauldron";
            case "trial_chambers" -> "trial_key";
            case "mansion", "pillager_outpost" -> "dark_oak_planks";
            case "trail_ruins" -> "gravel";
            default -> "bricks";
        };
    }

    private static String itemDifficulty(String name) {
        if (Pattern.compile("netherite|diamond|nether_star|beacon|elytra|dragon|shulker|chorus|purpur|end_stone|end_rod|ender_eye|heavy_core|mace|trident|totem|enchanting|anvil|brewing|blaze|wither|ghast|ancient_debris|echo_shard|conduit|sponge|music_disc|smithing_template|pottery_sherd|heart_of_the_sea|sea_lantern|prismarine|froglight").matcher(name).find()) return "HARD";
        if (Pattern.compile("^nether_|^crimson_|^warped_|^blackstone|^gilded_|^gilded|^soul_|^magma|^quartz|^basalt").matcher(name).find()) return "HARD";
        if (Pattern.compile("^oak_|^birch_|^spruce_|^jungle_|^acacia_|^dark_oak_|^mangrove_|^cherry_|^pale_oak_|^poplar_|^bamboo_|dirt|sand|gravel|cobblestone|^stone|^granite|^diorite|^andesite|^stick$|^bowl$|^torch$|^coal$|^charcoal$|^apple$|^wheat|^bread$|^seeds|^wool|_wool$|^crafting_table$|^furnace$|^chest$|^wooden_|^leather$|^feather$|^egg$|^sugar_cane$|^paper$|^sugar$|^poppy$|^dandelion$|^raw_|^porkchop$|^beef$|^chicken$|^mutton$|^kelp$|^clay|^cactus$").matcher(name).find()) return "EASY";
        return "MEDIUM";
    }

    private static String mobDifficulty(String name) {
        if (Pattern.compile("dragon|wither|warden|elder_guardian|evoker|ravager|shulker|breeze|piglin_brute|sniffer|mythical|infernal").matcher(name).find()) return "HARD";
        if (Pattern.compile("^(cow|pig|sheep|chicken|zombie|skeleton|spider|creeper|squid|cod|salmon|animal|arthropod|undead|skeletal|cubic)$").matcher(name).find()) return "EASY";
        return "MEDIUM";
    }

    private static String biomeDifficulty(String name) {
        if (!dimension(name).equals("NORMAL") || Pattern.compile("mushroom|badlands|cherry|deep_dark|ice_spikes|sulfur").matcher(name).find()) return "HARD";
        if (Pattern.compile("^(plains|forest|oak_forest|birch_forest|taiga|river|beach|ocean|desert|savanna|swamp|dappled_forest)$").matcher(name).find()) return "EASY";
        return "MEDIUM";
    }

    private static String structureDifficulty(String name) {
        return name.startsWith("village") || name.startsWith("shipwreck") || name.startsWith("ruined_portal") || name.startsWith("abandoned_camp") || name.startsWith("mineshaft") || name.startsWith("ocean_ruin") ? "MEDIUM" : "HARD";
    }

    private static String dimension(String name) {
        if (Set.of("the_end", "end", "end_barrens", "end_highlands", "end_midlands", "small_end_islands", "end_city", "ender_dragon", "shulker").contains(name)) return "THE_END";
        return Pattern.compile("nether|crimson|warped|soul_sand|basalt|fortress|bastion|^(blaze|ghast|hoglin|piglin|piglin_brute|strider|magma_cube|zombified_piglin|infernal)$").matcher(name).find() ? "NETHER" : "NORMAL";
    }

    private static String effort(String tier) { return tier.equals("EASY") ? "3" : tier.equals("MEDIUM") ? "5" : "9"; }
    private static String mode(String category) { return category.equals("ITEM") || category.equals("ADVANCEMENT") ? "ALWAYS" : "EXACT"; }
}
