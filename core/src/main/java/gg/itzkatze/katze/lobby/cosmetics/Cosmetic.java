package gg.itzkatze.katze.lobby.cosmetics;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;

public interface Cosmetic {

    @NotNull String id();

    @NotNull CosmeticCategory category();

    default @NotNull Component displayName() {
        return Component.translatable("katze.cosmetics.cosmetic." + category().name().toLowerCase(java.util.Locale.ROOT) + "." + id());
    }

    @NotNull Rarity rarity();

    @NotNull Material icon();

    default @Nullable Season season() {
        return null;
    }

    default boolean isAvailable(@NotNull LocalDate today) {
        Season season = season();
        return season == null || season.isActive(today);
    }

    default @NotNull Component styledName() {
        return displayName().color(rarity().color());
    }
}
