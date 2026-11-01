package gg.itzkatze.katze.lobby.cosmetics;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum BlockCosmetic implements Cosmetic {

    BRICKS("bricks", Material.BRICKS, Rarity.COMMON),
    PALE_OAK_PLANKS("pale_oak_planks", Material.PALE_OAK_PLANKS, Rarity.COMMON),
    BAMBOO_PLANKS("bamboo_planks", Material.BAMBOO_PLANKS, Rarity.COMMON),
    PURPUR_BLOCK("purpur_block", Material.PURPUR_BLOCK, Rarity.COMMON),
    QUARTZ_BLOCK("quartz_block", Material.QUARTZ_BLOCK, Rarity.COMMON),
    TUFF_BRICKS("tuff_bricks", Material.TUFF_BRICKS, Rarity.UNCOMMON),
    MAGMA_BLOCK("magma_block", Material.MAGMA_BLOCK, Rarity.UNCOMMON),
    SEA_LANTERN("sea_lantern", Material.SEA_LANTERN, Rarity.UNCOMMON),
    SHROOMLIGHT("shroomlight", Material.SHROOMLIGHT, Rarity.RARE),
    WAXED_BLOCK_OF_COPPER("waxed_block_of_copper", Material.WAXED_COPPER_BLOCK, Rarity.RARE),
    WAXED_CUT_COPPER("waxed_cut_copper", Material.WAXED_CUT_COPPER, Rarity.RARE),
    RED_MUSHROOM_BLOCK("red_mushroom_block", Material.RED_MUSHROOM_BLOCK, Rarity.RARE),
    BROWN_MUSHROOM_BLOCK("brown_mushroom_block", Material.BROWN_MUSHROOM_BLOCK, Rarity.RARE),
    AMETHYST_BLOCK("amethyst_block", Material.AMETHYST_BLOCK, Rarity.RARE),
    EMERALD_ORE("emerald_ore", Material.EMERALD_ORE, Rarity.RARE),
    REDSTONE_ORE("redstone_ore", Material.REDSTONE_ORE, Rarity.RARE),
    GOLD_ORE("gold_ore", Material.GOLD_ORE, Rarity.RARE),
    DIAMOND_ORE("diamond_ore", Material.DIAMOND_ORE, Rarity.RARE),
    BLOCK_OF_RAW_GOLD("block_of_raw_gold", Material.RAW_GOLD_BLOCK, Rarity.RARE),
    BLOCK_OF_RESIN("block_of_resin", Material.RESIN_BLOCK, Rarity.RARE),
    GILDED_BLACKSTONE("gilded_blackstone", Material.GILDED_BLACKSTONE, Rarity.RARE),
    NETHERITE_BLOCK("netherite_block", Material.NETHERITE_BLOCK, Rarity.EPIC),
    HONEYCOMB_BLOCK("honeycomb_block", Material.HONEYCOMB_BLOCK, Rarity.EPIC),
    VERDANT_FROGLIGHT("verdant_froglight", Material.VERDANT_FROGLIGHT, Rarity.LEGENDARY),
    OCHRE_FROGLIGHT("ochre_froglight", Material.OCHRE_FROGLIGHT, Rarity.LEGENDARY),
    PEARLESCENT_FROGLIGHT("pearlescent_froglight", Material.PEARLESCENT_FROGLIGHT, Rarity.LEGENDARY),
    BEACON("beacon", Material.BEACON, Rarity.MYTHIC),

    JACK_O_LANTERN("jack_o_lantern", Material.JACK_O_LANTERN, Rarity.RARE, Season.HALLOWEEN),
    FRESH_SNOW("fresh_snow", Material.SNOW_BLOCK, Rarity.RARE, Season.CHRISTMAS),
    MELON("melon", Material.MELON, Rarity.RARE, Season.SUMMER),
    CHERRY_PLANKS("cherry_planks", Material.CHERRY_PLANKS, Rarity.COMMON, Season.SPRING),
    TUBE_CORAL_BLOCK("tube_coral_block", Material.TUBE_CORAL_BLOCK, Rarity.RARE, Season.SUMMER),
    BRAIN_CORAL_BLOCK("brain_coral_block", Material.BRAIN_CORAL_BLOCK, Rarity.RARE, Season.SUMMER),
    BUBBLE_CORAL_BLOCK("bubble_coral_block", Material.BUBBLE_CORAL_BLOCK, Rarity.RARE, Season.SUMMER),
    FIRE_CORAL_BLOCK("fire_coral_block", Material.FIRE_CORAL_BLOCK, Rarity.RARE, Season.SUMMER),
    HORN_CORAL_BLOCK("horn_coral_block", Material.HORN_CORAL_BLOCK, Rarity.RARE, Season.SUMMER),
    ;
    private final String id;
    private final Material material;
    private final Rarity rarity;
    private final @Nullable Season season;

    BlockCosmetic(String id, Material material, Rarity rarity) {
        this(id, material, rarity, null);
    }

    BlockCosmetic(String id, Material material, Rarity rarity, @Nullable Season season) {
        this.id = id;
        this.material = material;
        this.rarity = rarity;
        this.season = season;
    }

    @Override
    public @NotNull String id() {
        return id;
    }

    @Override
    public @NotNull CosmeticCategory category() {
        return CosmeticCategory.BLOCK;
    }

    @Override
    public @NotNull Rarity rarity() {
        return rarity;
    }

    @Override
    public @NotNull Material icon() {
        return material;
    }

    @Override
    public @Nullable Season season() {
        return season;
    }

    public @NotNull Material material() {
        return material;
    }
}
