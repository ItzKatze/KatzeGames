package gg.itzkatze.katze.lobby.jumpandrun;

import gg.itzkatze.katze.lobby.cosmetics.BlockCosmetic;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

record RunLook(@NotNull Material block, @Nullable RunColor color, @NotNull Component startMessage) {

    static @NotNull RunLook of(@NotNull RunColor color) {
        Component message = Component.translatable("katze.cosmetics.run_start_color",
                TextArg.component("color", Component.translatable("katze.cosmetics.run_color." + color.name().toLowerCase(Locale.ROOT)).color(color.textColor())));
        return new RunLook(color.block(), color, message);
    }

    static @NotNull RunLook of(@NotNull BlockCosmetic cosmetic) {
        Component message = Component.translatable("katze.cosmetics.run_start_cosmetic",
                TextArg.component("cosmetic", cosmetic.styledName())).color(KatzeColor.YELLOW);
        return new RunLook(cosmetic.material(), null, message);
    }
}
