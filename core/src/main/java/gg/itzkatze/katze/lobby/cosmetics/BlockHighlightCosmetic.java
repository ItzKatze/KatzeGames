package gg.itzkatze.katze.lobby.cosmetics;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum BlockHighlightCosmetic implements Cosmetic {

    WHITE(DyeColor.WHITE),
    ORANGE(DyeColor.ORANGE),
    MAGENTA(DyeColor.MAGENTA),
    LIGHT_BLUE(DyeColor.LIGHT_BLUE),
    YELLOW(DyeColor.YELLOW),
    LIME(DyeColor.LIME),
    PINK(DyeColor.PINK),
    GRAY(DyeColor.GRAY),
    LIGHT_GRAY(DyeColor.LIGHT_GRAY),
    CYAN(DyeColor.CYAN),
    PURPLE(DyeColor.PURPLE),
    BLUE(DyeColor.BLUE),
    BROWN(DyeColor.BROWN),
    GREEN(DyeColor.GREEN),
    RED(DyeColor.RED),
    BLACK(DyeColor.BLACK);

    private final String id;
    private final Material icon;
    private final Color color;

    BlockHighlightCosmetic(DyeColor dye) {
        id = "highlight_" + dye.name().toLowerCase(Locale.ROOT);
        icon = Material.valueOf(dye.name() + "_DYE");
        color = dye.getColor();
    }

    @Override
    public @NotNull String id() {
        return id;
    }

    @Override
    public @NotNull CosmeticCategory category() {
        return CosmeticCategory.HIGHLIGHT;
    }

    @Override
    public @NotNull Rarity rarity() {
        return Rarity.COMMON;
    }

    @Override
    public @NotNull Material icon() {
        return icon;
    }

    public @NotNull Color color() {
        return color;
    }
}
