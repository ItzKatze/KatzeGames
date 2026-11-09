package gg.itzkatze.katze.lobby.cosmetics;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum CosmeticCategory {

    BLOCK(Material.GRASS_BLOCK),
    SOUND(Material.NOTE_BLOCK),
    MUSIC(Material.JUKEBOX),
    HIGHLIGHT(Material.YELLOW_DYE);

    private final Material icon;

    CosmeticCategory(Material icon) {
        this.icon = icon;
    }

    public @NotNull Component titleComponent() {
        return Component.translatable("katze.cosmetics.category." + name().toLowerCase(Locale.ROOT));
    }

    public @NotNull Material icon() {
        return icon;
    }
}
