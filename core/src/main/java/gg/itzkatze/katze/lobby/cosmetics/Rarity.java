package gg.itzkatze.katze.lobby.cosmetics;

import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum Rarity {

    COMMON(KatzeColor.WHITE),
    UNCOMMON(KatzeColor.LIME),
    RARE(KatzeColor.BLUE),
    EPIC(KatzeColor.PURPLE),
    LEGENDARY(KatzeColor.GOLD),
    MYTHIC(KatzeColor.MAGENTA),
    ;

    private final TextColor color;

    Rarity(TextColor color) {
        this.color = color;
    }

    public @NotNull TextColor color() {
        return color;
    }

    public @NotNull Component displayNameComponent() {
        return Component.translatable("katze.cosmetics.rarity." + name().toLowerCase(Locale.ROOT));
    }
}
