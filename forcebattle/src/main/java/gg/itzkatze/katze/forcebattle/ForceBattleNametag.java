package gg.itzkatze.katze.forcebattle;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.configuration.GlobalSetting;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.team.KatzeTeam;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

final class ForceBattleNametag {

    private ForceBattleNametag() {
    }

    static @NotNull Component suffix(@NotNull ForceBattlePlayer player) {
        Component suffix = Component.text(" ");

        if (player.isSpectator()) {
            return suffix.append(Component.translatable("katze.forcebattle.state.spectator").color(KatzeColor.LIGHT_GRAY).decorate(TextDecoration.ITALIC))
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        }
        if (player.isExcluded()) {
            suffix = suffix.append(Component.translatable("katze.forcebattle.state.excluded").color(KatzeColor.LIGHT_GRAY)
                    .decorate(TextDecoration.ITALIC));
        } else if (!KatzeGames.getTimer().isRunning()) {
            suffix = suffix.append(Component.translatable("katze.forcebattle.state.waiting").color(KatzeColor.LIGHT_GRAY)
                    .decorate(TextDecoration.ITALIC));
        }

        ForceBattleObjective objective = player.getObjective();
        if (objective == null && player.isInTeam()) {
            objective = (ForceBattleObjective) player.getTeam().getObjective();
        }

        if (objective != null) {
            boolean hideObjectives = SettingsManager.isEnabled(GlobalSetting.HIDE_OBJECTIVES);
            boolean hidePoints = SettingsManager.isEnabled(GlobalSetting.HIDE_POINTS);

            if (!hideObjectives) {
                suffix = suffix.append(Component.text(" ")).append(objectiveText(objective));
            }

            if (!hideObjectives && !hidePoints) {
                suffix = suffix.append(Component.text(" |", KatzeColor.GRAY));
            }

            if (!hidePoints) {
                suffix = suffix.append(Component.text(" "))
                        .append(Component.translatable("katze.forcebattle.points").color(KatzeColor.LIGHT_GRAY))
                        .append(Component.text(" » ", KatzeColor.GRAY))
                        .append(Component.text(player.getPoints(), KatzeColor.BLUE));
            }
        }

        KatzeTeam teamData = player.getTeam();
        if (teamData != null) {
            suffix = suffix.append(Component.text(" | ", KatzeColor.GRAY))
                    .append(Component.translatable("katze.forcebattle.team_label").color(KatzeColor.LIGHT_GRAY))
                    .append(Component.text(" » ", KatzeColor.GRAY))
                    .append(Component.text(teamData.getId(), KatzeColor.BLUE));
        }

        return suffix.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    static @NotNull Component objectiveText(@NotNull ForceBattleObjective objective) {
        return objective.getBattleType().displayName().color(KatzeColor.LIGHT_GRAY)
                .append(Component.text(" » ", KatzeColor.GRAY))
                .append(objective.displayName().color(KatzeColor.BLUE));
    }
}


