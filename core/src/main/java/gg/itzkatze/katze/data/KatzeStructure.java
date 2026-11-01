package gg.itzkatze.katze.data;

import lombok.Getter;
import org.bukkit.NamespacedKey;

@Getter
public enum KatzeStructure {
    VILLAGE_PLAINS(NamespacedKey.minecraft("village_plains")),
    VILLAGE_SAVANNA(NamespacedKey.minecraft("village_savanna")),
    VILLAGE_SNOWY(NamespacedKey.minecraft("village_snowy")),
    VILLAGE_TAIGA(NamespacedKey.minecraft("village_taiga")),
    VILLAGE_DESERT(NamespacedKey.minecraft("village_desert")),
    PILLAGER_OUTPOST(NamespacedKey.minecraft("pillager_outpost")),
    RUINED_PORTAL(NamespacedKey.minecraft("ruined_portal")),
    RUINED_PORTAL_MOUNTAIN(NamespacedKey.minecraft("ruined_portal_mountain")),
    RUINED_PORTAL_DESERT(NamespacedKey.minecraft("ruined_portal_desert")),
    RUINED_PORTAL_JUNGLE(NamespacedKey.minecraft("ruined_portal_jungle")),
    RUINED_PORTAL_OCEAN(NamespacedKey.minecraft("ruined_portal_ocean")),
    RUINED_PORTAL_SWAMP(NamespacedKey.minecraft("ruined_portal_swamp")),
    RUINED_PORTAL_NETHER(NamespacedKey.minecraft("ruined_portal_nether")),
    OCEAN_RUIN_COLD(NamespacedKey.minecraft("ocean_ruin_cold")),
    TRIAL_CHAMBERS(NamespacedKey.minecraft("trial_chambers")),
    SHIPWRECK(NamespacedKey.minecraft("shipwreck")),
    SHIPWRECK_BEACHED(NamespacedKey.minecraft("shipwreck_beached")),
    OCEAN_RUIN_WARM(NamespacedKey.minecraft("ocean_ruin_warm")),
    TRAIL_RUINS(NamespacedKey.minecraft("trail_ruins")),
    MANSION(NamespacedKey.minecraft("mansion")),
    JUNGLE_PYRAMID(NamespacedKey.minecraft("jungle_pyramid")),
    MINESHAFT_MESA(NamespacedKey.minecraft("mineshaft_mesa")),
    SWAMP_HUT(NamespacedKey.minecraft("swamp_hut")),
    END_CITY(NamespacedKey.minecraft("end_city")),
    ANCIENT_CITY(NamespacedKey.minecraft("ancient_city")),
    DESERT_PYRAMID(NamespacedKey.minecraft("desert_pyramid")),
    MONUMENT(NamespacedKey.minecraft("monument")),
    IGLOO(NamespacedKey.minecraft("igloo")),
    BASTION_REMNANT(NamespacedKey.minecraft("bastion_remnant")),
    MINESHAFT(NamespacedKey.minecraft("mineshaft")),
    FORTRESS(NamespacedKey.minecraft("fortress")),
    STRONGHOLD(NamespacedKey.minecraft("stronghold")),
    ABANDONED_CAMP_BAMBOO_JUNGLE(NamespacedKey.minecraft("abandoned_camp_bamboo_jungle")),
    ABANDONED_CAMP_BIRCH_FOREST(NamespacedKey.minecraft("abandoned_camp_birch_forest")),
    ABANDONED_CAMP_CHERRY_GROVE(NamespacedKey.minecraft("abandoned_camp_cherry_grove")),
    ABANDONED_CAMP_DAPPLED_FOREST(NamespacedKey.minecraft("abandoned_camp_dappled_forest")),
    ABANDONED_CAMP_FLOWER_FOREST(NamespacedKey.minecraft("abandoned_camp_flower_forest")),
    ABANDONED_CAMP_FOREST(NamespacedKey.minecraft("abandoned_camp_forest")),
    ABANDONED_CAMP_MEADOW(NamespacedKey.minecraft("abandoned_camp_meadow")),
    ABANDONED_CAMP_OLD_GROWTH_BIRCH_FOREST(NamespacedKey.minecraft("abandoned_camp_old_growth_birch_forest")),
    ABANDONED_CAMP_OLD_GROWTH_PINE_TAIGA(NamespacedKey.minecraft("abandoned_camp_old_growth_pine_taiga")),
    ABANDONED_CAMP_OLD_GROWTH_SPRUCE_TAIGA(NamespacedKey.minecraft("abandoned_camp_old_growth_spruce_taiga")),
    ABANDONED_CAMP_PALE_GARDEN(NamespacedKey.minecraft("abandoned_camp_pale_garden")),
    ABANDONED_CAMP_SAVANNA(NamespacedKey.minecraft("abandoned_camp_savanna")),
    ABANDONED_CAMP_SNOWY_TAIGA(NamespacedKey.minecraft("abandoned_camp_snowy_taiga")),
    ABANDONED_CAMP_SPARSE_JUNGLE(NamespacedKey.minecraft("abandoned_camp_sparse_jungle")),
    ABANDONED_CAMP_SWAMP(NamespacedKey.minecraft("abandoned_camp_swamp")),
    ABANDONED_CAMP_TAIGA(NamespacedKey.minecraft("abandoned_camp_taiga")),
    ABANDONED_CAMP_WINDSWEPT_FOREST(NamespacedKey.minecraft("abandoned_camp_windswept_forest")),
    ABANDONED_CAMP_WOODED_BADLANDS(NamespacedKey.minecraft("abandoned_camp_wooded_badlands"));

    public final NamespacedKey key;

    KatzeStructure(NamespacedKey key) {
        this.key = key;
    }

}
