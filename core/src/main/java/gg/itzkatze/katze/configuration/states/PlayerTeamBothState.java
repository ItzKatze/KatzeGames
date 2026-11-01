package gg.itzkatze.katze.configuration.states;

import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public enum PlayerTeamBothState implements SettingStateType {
    OFF(Component.translatable("katze.settings.state.off").color(KatzeColor.RED)),
    PLAYER(Component.translatable("katze.settings.state.player").color(KatzeColor.LIME)),
    TEAM(Component.translatable("katze.settings.state.team").color(KatzeColor.LIGHT_BLUE)),
    BOTH(Component.translatable("katze.settings.state.both").color(KatzeColor.GOLD));

    private final Component defaultName;

    PlayerTeamBothState(Component defaultName) {
        this.defaultName = defaultName;
    }

    @Override
    @NotNull public PlayerTeamBothState next() {
        return switch (this) {
            case OFF -> PLAYER;
            case PLAYER -> TEAM;
            case TEAM -> BOTH;
            case BOTH -> OFF;
        };
    }

    @Override
    @NotNull public Component getDisplayName() {
        return defaultName;
    }
}
