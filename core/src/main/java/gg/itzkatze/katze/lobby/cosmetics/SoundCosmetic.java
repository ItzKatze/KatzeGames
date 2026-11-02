package gg.itzkatze.katze.lobby.cosmetics;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum SoundCosmetic implements Cosmetic {

    PLING("pling", "block.note_block.pling", Rarity.COMMON, Material.NOTE_BLOCK),
    HARP("harp", "block.note_block.harp", Rarity.COMMON, Material.NOTE_BLOCK),
    CHIME("chime", "block.note_block.chime", Rarity.COMMON, Material.NOTE_BLOCK),
    BELL("bell", "block.note_block.bell", Rarity.COMMON, Material.BELL),
    XYLOPHONE("xylophone", "block.note_block.xylophone", Rarity.COMMON, Material.NOTE_BLOCK),
    BIT("bit", "block.note_block.bit", Rarity.COMMON, Material.NOTE_BLOCK),
    BANJO("banjo", "block.note_block.banjo", Rarity.COMMON, Material.NOTE_BLOCK),
    COW_BELL("cow_bell", "block.note_block.cow_bell", Rarity.COMMON, Material.NOTE_BLOCK),
    IRON_XYLOPHONE("iron_xylophone", "block.note_block.iron_xylophone", Rarity.COMMON, Material.IRON_INGOT),
    PLING_LOW("pling_low", "block.note_block.pling", Rarity.COMMON, Material.NOTE_BLOCK),

    AMETHYST_HIT("amethyst_hit", "block.amethyst_block.hit", Rarity.UNCOMMON, Material.AMETHYST_SHARD),
    EXPERIENCE_ORB("experience_orb", "entity.experience_orb.pickup", Rarity.UNCOMMON, Material.EXPERIENCE_BOTTLE),
    ITEM_PICKUP("item_pickup", "entity.item.pickup", Rarity.UNCOMMON, Material.HOPPER),
    BUTTON_CLICK("button_click", "ui.button.click", Rarity.UNCOMMON, Material.STONE_BUTTON),
    LEVER_CLICK("lever_click", "block.lever.click", Rarity.UNCOMMON, Material.LEVER),
    ARMOR_EQUIP("armor_equip", "item.armor.equip_iron", Rarity.UNCOMMON, Material.IRON_CHESTPLATE),

    FOX("fox", "entity.fox.ambient", Rarity.UNCOMMON, Material.SWEET_BERRIES),
    FROG("frog", "entity.frog.ambient", Rarity.UNCOMMON, Material.FROGSPAWN),

    LEVEL_UP("level_up", "entity.player.levelup", Rarity.UNCOMMON, Material.EXPERIENCE_BOTTLE),
    VILLAGER_YES("villager_yes", "entity.villager.yes", Rarity.UNCOMMON, Material.EMERALD),
    EXPERIENCE_BOTTLE("experience_bottle", "entity.experience_bottle.throw", Rarity.UNCOMMON, Material.EXPERIENCE_BOTTLE),
    NOTE_BLOCK_BASS("note_block_bass", "block.note_block.bass", Rarity.UNCOMMON, Material.NOTE_BLOCK),
    NOTE_BLOCK_GUITAR("note_block_guitar", "block.note_block.guitar", Rarity.UNCOMMON, Material.NOTE_BLOCK),
    NOTE_BLOCK_DIDGERIDOO("note_block_didgeridoo", "block.note_block.didgeridoo", Rarity.UNCOMMON, Material.NOTE_BLOCK),
    NOTE_BLOCK_COW_BELL("note_block_cow_bell", "block.note_block.cow_bell", Rarity.UNCOMMON, Material.NOTE_BLOCK),

    WITCH_CACKLE("witch_cackle", "entity.witch.celebrate", Rarity.RARE, Material.CAULDRON, Season.HALLOWEEN),
    SNOW_GOLEM("snow_golem", "entity.snow_golem.ambient", Rarity.UNCOMMON, Material.CARVED_PUMPKIN, Season.WINTER),
    FIREWORK_TWINKLE("firework_twinkle", "entity.firework_rocket.twinkle", Rarity.UNCOMMON, Material.FIREWORK_ROCKET, Season.WINTER),
    SNOW_BLOCK("snow_block", "block.snow.hit", Rarity.UNCOMMON, Material.SNOW_BLOCK, Season.WINTER),
    CHICKEN_POP("chicken_pop", "entity.chicken.egg", Rarity.UNCOMMON, Material.EGG, Season.EASTER),
    ;

    public static final Sound DEFAULT = Sound.sound(Key.key("entity.experience_orb.pickup"), Sound.Source.PLAYER, 1f, 1.2f);
    private final String id;
    private final Sound sound;
    private final Rarity rarity;
    private final Material icon;
    private final @Nullable Season season;

    SoundCosmetic(String id, String key, Rarity rarity, Material icon) {
        this(id, key, rarity, icon, null);
    }

    SoundCosmetic(String id, String key, Rarity rarity, Material icon, @Nullable Season season) {
        this.id = id;
        this.sound = Sound.sound(Key.key(key), Sound.Source.PLAYER, 1f, 1f);
        this.rarity = rarity;
        this.icon = icon;
        this.season = season;
    }

    @Override
    public @NotNull String id() {
        return id;
    }

    @Override
    public @NotNull CosmeticCategory category() {
        return CosmeticCategory.SOUND;
    }

    @Override
    public @NotNull Rarity rarity() {
        return rarity;
    }

    @Override
    public @NotNull Material icon() {
        return icon;
    }

    @Override
    public @Nullable Season season() {
        return season;
    }

    public @NotNull Sound sound() {
        return sound;
    }
}
