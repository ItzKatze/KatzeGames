package gg.itzkatze.katze.forcebattle;

import gg.itzkatze.katze.configuration.GlobalSetting;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.scoreboard.ScoreboardContent;
import gg.itzkatze.katze.team.KatzeTeam;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ForceBattleScoreboardContent implements ScoreboardContent {

    private static final int MAX_STRING_LENGTH = 30;

    @Override
    @NotNull public Component getTitle() {
        return Component.translatable("katze.forcebattle.scoreboard.title");
    }

    @Override
    @NotNull public Component[] getLines(@NotNull KatzePlayer katzePlayer) {
        ForceBattlePlayer player = (ForceBattlePlayer) katzePlayer;
        List<Component> lines = new ArrayList<>();

        ForceBattleObjective selfObjective = player.getObjective();
        if (selfObjective != null) {
            lines.add(Component.translatable("katze.forcebattle.scoreboard.your_objective").color(KatzeColor.YELLOW));
            lines.add(truncate(objectiveText(selfObjective), KatzeColor.WHITE));
        } else {
            lines.add(Component.translatable("katze.forcebattle.scoreboard.no_objective").color(KatzeColor.LIGHT_GRAY));
        }

        lines.add(Component.text(" "));

        if (player.isInTeam()) {
            KatzeTeam team = player.getTeam();

            ForceBattleObjective teamObjective = (ForceBattleObjective) team.getObjective();
            if (teamObjective != null && SettingsManager.isEnabled(GlobalSetting.EXTRA_TEAM_OBJECTIVE)) {
                lines.add(Component.translatable("katze.forcebattle.scoreboard.team_objective").color(KatzeColor.YELLOW));
                lines.add(truncate(objectiveText(teamObjective), KatzeColor.WHITE));
                lines.add(Component.text("  "));
            }

            if (team.getPlayers().size() > 1) {
                lines.add(Component.translatable("katze.forcebattle.scoreboard.teammates").color(KatzeColor.YELLOW));

                for (KatzePlayer teammate : team.getPlayers()) {
                    if (teammate.equals(player)) continue;
                    ForceBattlePlayer teammatePlayer = (ForceBattlePlayer) teammate;

                    lines.add(Component.text("* ").color(KatzeColor.LIGHT_GRAY)
                            .append(Component.text(teammate.getName()).color(KatzeColor.LIGHT_BLUE)));

                    ForceBattleObjective teammateObjective = teammatePlayer.getObjective();
                    lines.add(teammateObjective == null
                            ? Component.translatable("katze.forcebattle.scoreboard.none").color(KatzeColor.WHITE)
                            : Component.text("  ").append(truncate(objectiveText(teammateObjective), KatzeColor.WHITE)));
                }
            }
        }

        return lines.toArray(new Component[0]);
    }

    private Component truncate(Component value, TextColor color) {
        String plain = PlainTextComponentSerializer.plainText().serialize(value);
        if (plain.length() <= MAX_STRING_LENGTH) {
            return value.color(color);
        }
        return Component.text(plain.substring(0, 21) + "...").color(color);
    }

    private Component objectiveText(ForceBattleObjective objective) {
        return objective.displayIcon().append(Component.text(" ")).append(objective.displayName());
    }

}

