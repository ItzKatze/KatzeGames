package gg.itzkatze.katze.bossbar;

import gg.itzkatze.katze.player.KatzePlayer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BossbarContent {

    @NotNull BossbarKind getKind();

    default @Nullable BossBar getBossbar(@NotNull KatzePlayer player) {
        return updateBossbar(player, null);
    }

    @Nullable BossBar updateBossbar(@NotNull KatzePlayer player, @Nullable BossBar current);

    static BossBar update(@Nullable BossBar current, Component name, float progress, BossBar.Color color, BossBar.Overlay overlay) {
        if (current == null) return BossBar.bossBar(name, progress, color, overlay);
        if (!current.name().equals(name)) current.name(name);
        if (current.progress() != progress) current.progress(progress);
        if (current.color() != color) current.color(color);
        if (current.overlay() != overlay) current.overlay(overlay);
        return current;
    }

    default int getPriority() {
        return 0;
    }
}
