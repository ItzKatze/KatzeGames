package gg.itzkatze.katze.lobby.cosmetics;

import org.bukkit.Material;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

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
        return Component.translatable("katze.cosmetics.category." + name().toLowerCase(java.util.Locale.ROOT));
    }

    public @NotNull Material icon() {
        return icon;
    }
}
