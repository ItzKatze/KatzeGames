package gg.itzkatze.katze.forcebattle;

import gg.itzkatze.katze.configuration.GlobalSetting;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.configuration.states.PlayerTeamBothState;
import gg.itzkatze.katze.data.BiomeSimplified;
import gg.itzkatze.katze.data.DamageTypeSimplified;
import gg.itzkatze.katze.data.KatzeAdvancement;
import gg.itzkatze.katze.data.KatzeDamageType;
import gg.itzkatze.katze.data.KatzeStructure;
import gg.itzkatze.katze.data.MobsSimplified;
import gg.itzkatze.katze.data.StructureSimplified;
import gg.itzkatze.katze.forcebattle.exclusion.ObjectiveFilter;
import gg.itzkatze.katze.game.seed.SeedFeature;
import gg.itzkatze.katze.game.seed.SeedRequirement;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ForceBattleSeedRequirements {
    private final Set<SeedRequirement> requirements = new LinkedHashSet<>();
    private final ObjectiveFilter filter;
    private final boolean simplified;
    private boolean fullCoverage;

    ForceBattleSeedRequirements(ObjectiveFilter filter) {
        this(filter, false);
    }

    ForceBattleSeedRequirements(ObjectiveFilter filter, boolean simplified) {
        this.filter = filter;
        this.simplified = simplified;
    }

    static List<SeedRequirement> current() {
        boolean simplified = SettingsManager.isEnabled(GlobalSetting.SIMPLIFIED_OBJECTIVES);
        ForceBattleSeedRequirements builder = new ForceBattleSeedRequirements(ObjectiveFilter.current(), simplified);
        boolean teamObjectives = SettingsManager.isEnabled(GlobalSetting.EXTRA_TEAM_OBJECTIVE);
        for (ForceBattleType type : ForceBattleType.values()) {
            if (type.getSettingState() == PlayerTeamBothState.OFF) continue;
            if (type.getSettingState() == PlayerTeamBothState.TEAM && !teamObjectives) continue;
            for (ForceBattleObjectiveManager.ObjectiveSeed candidate : ForceBattleObjectiveManager.getInstance()
                    .getCandidates(type, null, builder.filter, simplified)) {
                builder.add(candidate.getValue());
            }
        }
        return builder.build();
    }

    List<SeedRequirement> build() {
        if (fullCoverage) addCoverage();
        return List.copyOf(requirements);
    }

    void add(Object value) {
        switch (value) {
            case Biome biome -> require("biome/" + biome.getKey(), List.of(biome(biome)));
            case BiomeSimplified group -> require("biome/" + group.name(), group.getBiomes().stream()
                    .filter(member -> !filter.isExcluded(member)).map(ForceBattleSeedRequirements::biome).toList());
            case KatzeStructure structure -> require("structure/" + structure.name(), List.of(structure(structure)));
            case StructureSimplified group -> require("structure/" + group.name(), group.getStructures().stream()
                    .filter(member -> !filter.isExcluded(member)).map(ForceBattleSeedRequirements::structure).toList());
            case EntityType mob -> addDependencies(mob.name(), mobDependencies(mob.name()));
            case MobsSimplified group -> {
                List<List<SeedFeature>> alternatives = group.getMobs().stream().filter(member -> !filter.isExcluded(member))
                        .map(member -> mobDependencies(member.name())).toList();
                if (alternatives.stream().anyMatch(list -> list != null && list.isEmpty())) break;
                List<SeedFeature> route = alternatives.stream().filter(list -> list != null).findFirst().orElse(null);
                addDependencies("mob/" + group.name(), route);
            }
            case Material material -> addItemDependencies(material);
            case KatzeAdvancement advancement -> addDependencies(advancement.name(), advancementDependencies(advancement));
            case KatzeDamageType death -> addDependencies(death.name(), deathDependencies(death));
            case DamageTypeSimplified group -> {
                List<List<SeedFeature>> alternatives = group.getDamageTypes().stream().filter(member -> !filter.isExcluded(member))
                        .map(ForceBattleSeedRequirements::deathDependencies).toList();
                List<SeedFeature> route = alternatives.stream().filter(list -> list != null)
                        .min((left, right) -> Integer.compare(left.size(), right.size())).orElse(null);
                addDependencies("death/" + group.name(), route);
            }
            case Integer ignored -> { }
            default -> throw new IllegalArgumentException("No seed dependency resolver for " + value);
        }
    }

    private void addItemDependencies(Material material) {
        if (material != Material.TOTEM_OF_UNDYING) {
            addDependencies(material.name(), itemDependencies(material.name()));
            return;
        }
        SeedFeature mansion = structure(KatzeStructure.MANSION);
        require("TOTEM_OF_UNDYING/evoker_source", List.of(mansion, structure(KatzeStructure.PILLAGER_OUTPOST)));
        List<SeedFeature> villageOrMansion = new ArrayList<>(List.of(mansion));
        villageOrMansion.addAll(StructureSimplified.VILLAGE.getStructures().stream()
                .map(ForceBattleSeedRequirements::structure).toList());
        require("TOTEM_OF_UNDYING/raid_village", villageOrMansion);
    }

    private void addDependencies(String objective, List<SeedFeature> dependencies) {
        if (dependencies == null) {
            fullCoverage = true;
            return;
        }
        for (SeedFeature feature : dependencies) require(objective + "/" + feature.key(), List.of(feature));
        if (dependencies.stream().anyMatch(feature -> feature.environment() == World.Environment.THE_END)) {
            require("access/end/stronghold", List.of(structure(KatzeStructure.STRONGHOLD)));
            require("access/end/fortress", List.of(structure(KatzeStructure.FORTRESS)));
        }
    }

    private void require(String name, List<SeedFeature> alternatives) {
        requirements.add(new SeedRequirement(name, alternatives));
        if (alternatives.stream().allMatch(feature -> feature.environment() == World.Environment.THE_END)) {
            requirements.add(new SeedRequirement("access/end/stronghold", List.of(structure(KatzeStructure.STRONGHOLD))));
            requirements.add(new SeedRequirement("access/end/fortress", List.of(structure(KatzeStructure.FORTRESS))));
        }
    }

    private void addCoverage() {
        addBiomeCoverage(RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).stream().toList());
        var structures = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);
        addStructureCoverage(structures.stream().map(structures::getKeyOrThrow).map(Object::toString).toList());
    }

    private void addBiomeCoverage(List<Biome> available) {
        Set<Biome> grouped = new LinkedHashSet<>();
        if (simplified) {
            for (BiomeSimplified group : BiomeSimplified.values()) {
                grouped.addAll(group.getBiomes());
                if (filter.isExcluded(group)) continue;
                List<SeedFeature> alternatives = group.getBiomes().stream().filter(available::contains)
                        .filter(member -> !filter.isExcluded(member)).map(ForceBattleSeedRequirements::biome).toList();
                if (!alternatives.isEmpty()) require("coverage/biome/" + group.name(), alternatives);
            }
        }
        available.stream().filter(value -> !grouped.contains(value) && !filter.isExcluded(value))
                .filter(value -> !value.getKey().getKey().equals("the_void"))
                .sorted((left, right) -> left.getKey().toString().compareTo(right.getKey().toString()))
                .forEach(value -> require("coverage/biome/" + value.getKey(), List.of(biome(value))));
    }

    void addStructureCoverage(List<String> available) {
        Set<String> grouped = new LinkedHashSet<>();
        if (simplified) {
            for (StructureSimplified group : StructureSimplified.values()) {
                grouped.addAll(group.getStructures().stream().map(member -> member.getKey().toString()).toList());
                if (filter.isExcluded(group)) continue;
                List<SeedFeature> alternatives = group.getStructures().stream().filter(member -> !filter.isExcluded(member))
                        .map(ForceBattleSeedRequirements::structure).filter(feature -> available.contains(feature.key())).toList();
                if (!alternatives.isEmpty()) require("coverage/structure/" + group.name(), alternatives);
            }
        }
        available.stream().filter(key -> !grouped.contains(key))
                .filter(key -> Arrays.stream(KatzeStructure.values()).noneMatch(member ->
                        member.getKey().toString().equals(key) && filter.isExcluded(member))).sorted()
                .forEach(key -> require("coverage/structure/" + key, List.of(structure(key))));
    }

    private static List<SeedFeature> mobDependencies(String name) {
        return switch (name) {
            case "BLAZE", "WITHER_SKELETON", "WITHER" -> structures(KatzeStructure.FORTRESS);
            case "SHULKER" -> structures(KatzeStructure.END_CITY);
            case "ENDER_DRAGON" -> biomes(Biome.THE_END);
            case "ELDER_GUARDIAN", "GUARDIAN" -> structures(KatzeStructure.MONUMENT);
            case "WARDEN" -> biomes(Biome.DEEP_DARK);
            case "BREEZE" -> structures(KatzeStructure.TRIAL_CHAMBERS);
            case "PIGLIN_BRUTE" -> structures(KatzeStructure.BASTION_REMNANT);
            case "MOOSHROOM" -> biomes(Biome.MUSHROOM_FIELDS);
            case "PANDA", "PARROT", "OCELOT" -> biomes(Biome.JUNGLE);
            case "AXOLOTL" -> biomes(Biome.LUSH_CAVES);
            case "SULFUR_CUBE" -> biomes(Biome.SULFUR_CAVES);
            case "SNIFFER" -> structures(KatzeStructure.OCEAN_RUIN_WARM);
            case "HOGLIN", "ZOGLIN" -> biomes(Biome.CRIMSON_FOREST);
            case "PIGLIN", "ZOMBIFIED_PIGLIN", "GHAST", "HAPPY_GHAST", "STRIDER", "MAGMA_CUBE" -> biomes(Biome.NETHER_WASTES);
            case "GOAT" -> biomes(Biome.JAGGED_PEAKS);
            case "FROG", "TADPOLE", "BOGGED" -> biomes(Biome.SWAMP);
            case "ARMADILLO" -> biomes(Biome.SAVANNA);
            case "CAMEL", "HUSK", "PARCHED", "CAMEL_HUSK" -> biomes(Biome.DESERT);
            case "POLAR_BEAR", "STRAY" -> biomes(Biome.SNOWY_PLAINS);
            case "FOX", "WOLF" -> biomes(Biome.TAIGA);
            case "TURTLE" -> biomes(Biome.BEACH);
            case "BEE" -> biomes(Biome.MEADOW);
            case "EVOKER", "VINDICATOR", "VEX", "ALLAY" -> structures(KatzeStructure.MANSION);
            case "PILLAGER", "RAVAGER" -> structures(KatzeStructure.PILLAGER_OUTPOST, KatzeStructure.VILLAGE_PLAINS);
            case "VILLAGER", "CAT" -> structures(KatzeStructure.VILLAGE_PLAINS);
            case "CAVE_SPIDER" -> structures(KatzeStructure.MINESHAFT);
            case "SILVERFISH" -> structures(KatzeStructure.STRONGHOLD);
            case "CHICKEN", "COW", "DONKEY", "HORSE", "MULE", "PIG", "SHEEP", "RABBIT", "LLAMA" -> biomes(Biome.PLAINS);
            case "DROWNED", "COD", "SALMON", "SQUID", "GLOW_SQUID", "DOLPHIN", "PUFFERFISH", "TROPICAL_FISH",
                    "NAUTILUS", "ZOMBIE_NAUTILUS" -> biomes(Biome.OCEAN);
            case "BAT", "TRADER_LLAMA", "WANDERING_TRADER", "COPPER_GOLEM", "IRON_GOLEM", "SNOW_GOLEM", "SLIME",
                    "ENDERMAN", "ENDERMITE", "SKELETON", "SKELETON_HORSE", "SPIDER", "CREEPER", "ZOMBIE",
                    "ZOMBIE_VILLAGER", "WITCH", "PHANTOM", "ZOMBIE_HORSE" -> List.of();
            default -> null;
        };
    }

    private static List<SeedFeature> itemDependencies(String name) {
        if (name.contains("NETHERITE") || name.equals("ANCIENT_DEBRIS")) return structures(KatzeStructure.BASTION_REMNANT);
        if (name.startsWith("BLAZE_") || name.equals("NETHER_WART") || name.equals("WITHER_SKELETON_SKULL")
                || name.equals("NETHER_STAR") || name.equals("BEACON")) return structures(KatzeStructure.FORTRESS);
        if (name.startsWith("SHULKER_") || name.equals("ELYTRA")) return structures(KatzeStructure.END_CITY);
        if (name.startsWith("CHORUS_") || name.startsWith("PURPUR_") || name.equals("END_ROD")) return biomes(Biome.END_HIGHLANDS);
        if (name.equals("DRAGON_BREATH") || name.equals("DRAGON_EGG")) return biomes(Biome.THE_END);
        if (name.startsWith("BREEZE_") || name.equals("HEAVY_CORE") || name.equals("MACE")) return structures(KatzeStructure.TRIAL_CHAMBERS);
        if (name.startsWith("ECHO_") || name.equals("RECOVERY_COMPASS") || name.equals("DISC_FRAGMENT_5")) return structures(KatzeStructure.ANCIENT_CITY);
        if (name.contains("SNIFFER") || name.startsWith("TORCHFLOWER") || name.startsWith("PITCHER_")) return structures(KatzeStructure.OCEAN_RUIN_WARM);
        if (name.contains("SPONGE")) return structures(KatzeStructure.MONUMENT);
        if (name.equals("HEART_OF_THE_SEA")) return List.of(structure("minecraft:buried_treasure"));
        if (name.equals("CONDUIT")) return List.of(structure("minecraft:buried_treasure"), structure(KatzeStructure.MONUMENT));
        if (name.startsWith("SCULK")) return biomes(Biome.DEEP_DARK);
        if (name.contains("MANGROVE")) return biomes(Biome.MANGROVE_SWAMP);
        if (name.contains("CHERRY")) return biomes(Biome.CHERRY_GROVE);
        if (name.contains("PALE_OAK") || name.contains("RESIN") || name.equals("CREAKING_HEART")) return biomes(Biome.PALE_GARDEN);
        if (name.contains("BAMBOO")) return biomes(Biome.BAMBOO_JUNGLE);
        if (name.contains("JUNGLE") || name.equals("COCOA_BEANS")) return biomes(Biome.JUNGLE);
        if (name.contains("ACACIA")) return biomes(Biome.SAVANNA);
        if (name.contains("SPRUCE") || name.equals("SWEET_BERRIES")) return biomes(Biome.TAIGA);
        if (name.contains("DARK_OAK")) return biomes(Biome.DARK_FOREST);
        if (name.contains("BIRCH")) return biomes(Biome.BIRCH_FOREST);
        if (name.contains("CRIMSON")) return biomes(Biome.CRIMSON_FOREST);
        if (name.contains("WARPED")) return biomes(Biome.WARPED_FOREST);
        if (name.contains("PRISMARINE") || name.equals("SEA_LANTERN")) return structures(KatzeStructure.MONUMENT);
        if (name.equals("DIAMOND") || name.equals("IRON_INGOT") || name.equals("GOLD_INGOT") || name.equals("COAL")
                || name.equals("COBBLESTONE") || name.equals("STONE") || name.equals("DIRT") || name.equals("STICK")) return List.of();
        return null;
    }

    private static List<SeedFeature> advancementDependencies(KatzeAdvancement advancement) {
        return switch (advancement) {
            case EYE_SPY, THE_END -> structures(KatzeStructure.STRONGHOLD, KatzeStructure.FORTRESS);
            case A_TERRIBLE_FORTRESS, INTO_FIRE, SPOOKY_SCARY_SKELETON, WITHERING_HEIGHTS,
                    LOCAL_BREWERY, BRING_HOME_THE_BEACON, BEACONATOR -> structures(KatzeStructure.FORTRESS);
            case THOSE_WERE_THE_DAYS, WAR_PIGS, COVER_ME_IN_DEBRIS, SERIOUS_DEDICATION -> structures(KatzeStructure.BASTION_REMNANT);
            case THE_CITY_AT_THE_END_OF_THE_GAME, SKY_IS_THE_LIMIT, GREAT_VIEW_FROM_UP_HERE -> structures(KatzeStructure.END_CITY);
            case FREE_THE_END, REMOTE_GATEWAY, THE_END_AGAIN, YOU_NEED_A_MINT, IS_IT_A_PLANE -> biomes(Biome.THE_END);
            case SMELLS_INTERESTING, LITTLE_SNIFFS, PLANTING_THE_PAST -> structures(KatzeStructure.OCEAN_RUIN_WARM);
            case SNEAK_100, IT_SPREADS -> biomes(Biome.DEEP_DARK);
            case SOUND_OF_MUSIC -> biomes(Biome.MEADOW);
            case UH_OH -> biomes(Biome.SULFUR_CAVES);
            case STONE_AGE, GETTING_AN_UPGRADE, ACQUIRE_HARDWARE, SUIT_UP, HOT_STUFF, ISNT_IT_IRON_PICK,
                    NOT_TODAY_THANK_YOU, ICE_BUCKET_CHALLENGE, DIAMONDS, WE_NEED_TO_GO_DEEPER,
                    COVER_ME_WITH_DIAMONDS, ENCHANTER -> List.of();
            default -> null;
        };
    }

    private static List<SeedFeature> deathDependencies(KatzeDamageType death) {
        return switch (death.name()) {
            case "DRAGON_BREATH" -> biomes(Biome.THE_END);
            case "SONIC_BOOM" -> biomes(Biome.DEEP_DARK);
            case "WIND_CHARGE" -> structures(KatzeStructure.TRIAL_CHAMBERS);
            case "WITHER" -> structures(KatzeStructure.FORTRESS);
            case "SULFUR_CUBE_HOT" -> biomes(Biome.SULFUR_CAVES);
            case "SWEET_BERRY_BUSH" -> biomes(Biome.TAIGA);
            case "STING" -> biomes(Biome.MEADOW);
            case "FALLING_STALACTITE", "STALAGMITE" -> biomes(Biome.DRIPSTONE_CAVES);
            case "FLY_INTO_WALL" -> structures(KatzeStructure.END_CITY);
            case "CACTUS" -> biomes(Biome.DESERT);
            case "FREEZE" -> biomes(Biome.SNOWY_SLOPES);
            case "SPIT" -> biomes(Biome.SAVANNA);
            case "TRIDENT" -> biomes(Biome.OCEAN);
            case "FIREBALL" -> biomes(Biome.NETHER_WASTES);
            default -> List.of();
        };
    }

    private static List<SeedFeature> biomes(Biome... values) {
        return Arrays.stream(values).map(ForceBattleSeedRequirements::biome).toList();
    }

    private static List<SeedFeature> structures(KatzeStructure... values) {
        return Arrays.stream(values).map(ForceBattleSeedRequirements::structure).toList();
    }

    private static SeedFeature biome(Biome biome) {
        if (!biome.getKey().getNamespace().equals("minecraft")) {
            throw new IllegalArgumentException("Custom biome needs an explicit seed dimension mapping: " + biome.getKey());
        }
        String name = biome.getKey().getKey();
        World.Environment environment = switch (name) {
            case "nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas" -> World.Environment.NETHER;
            case "the_end", "small_end_islands", "end_midlands", "end_highlands", "end_barrens" -> World.Environment.THE_END;
            case "the_void" -> throw new IllegalArgumentException("The Void cannot be a survival objective");
            default -> World.Environment.NORMAL;
        };
        return new SeedFeature(SeedFeature.Kind.BIOME, environment, biome.getKey().toString());
    }

    private static SeedFeature structure(KatzeStructure structure) {
        return structure(structure.getKey().toString());
    }

    private static SeedFeature structure(String key) {
        if (!key.startsWith("minecraft:")) {
            throw new IllegalArgumentException("Custom structure needs an explicit seed dimension mapping: " + key);
        }
        World.Environment environment = switch (key) {
            case "minecraft:fortress", "minecraft:bastion_remnant", "minecraft:ruined_portal_nether", "minecraft:nether_fossil" -> World.Environment.NETHER;
            case "minecraft:end_city" -> World.Environment.THE_END;
            default -> World.Environment.NORMAL;
        };
        return new SeedFeature(SeedFeature.Kind.STRUCTURE, environment, key);
    }
}
