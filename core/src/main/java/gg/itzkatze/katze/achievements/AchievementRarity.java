package gg.itzkatze.katze.achievements;

import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;

public enum AchievementRarity {
    COMMON(25, KatzeColor.WHITE),
    UNCOMMON(50, KatzeColor.LIME),
    RARE(100, KatzeColor.BLUE),
    MYTHICAL(250, KatzeColor.MAGENTA);

    private final int xp;
    private final TextColor color;

    AchievementRarity(int xp, TextColor color) {
        this.xp = xp;
        this.color = color;
    }

    public int xp() {
        return xp;
    }

    public TextColor color() {
        return color;
    }

    public Component title() {
        return Component.translatable("katze.achievements.rarity." + name().toLowerCase(Locale.ROOT)).color(color);
    }
}
