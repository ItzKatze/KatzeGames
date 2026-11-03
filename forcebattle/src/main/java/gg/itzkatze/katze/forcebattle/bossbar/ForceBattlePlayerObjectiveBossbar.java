package gg.itzkatze.katze.forcebattle.bossbar;

import gg.itzkatze.katze.bossbar.BossbarContent;
import gg.itzkatze.katze.bossbar.BossbarKind;
import gg.itzkatze.katze.forcebattle.ForceBattleObjective;
import gg.itzkatze.katze.configuration.GlobalSetting;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.forcebattle.ForceBattlePlayer;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ForceBattlePlayerObjectiveBossbar implements BossbarContent {

    @Override
    @NotNull public BossbarKind getKind() {
        return BossbarKind.RUNNING;
    }

    @Override
    @Nullable public BossBar updateBossbar(@NotNull KatzePlayer katzePlayer, @Nullable BossBar current) {
        ForceBattlePlayer player = (ForceBattlePlayer) katzePlayer;
        ForceBattleObjective objective = player.getObjective();
        if (objective == null) return null;

        return BossbarContent.update(current,
                getTitle(player),
                1,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS
        );
    }

    private @NotNull Component getTitle(KatzePlayer katzePlayer) {
        ForceBattlePlayer player = (ForceBattlePlayer) katzePlayer;
        ForceBattleObjective playerObjective = player.getObjective();

        if (playerObjective == null) return Component.empty();

        boolean isPlayerInTeam = katzePlayer.isInTeam();
        int points = isPlayerInTeam ? katzePlayer.getTeam().getPoints() : katzePlayer.getPoints();

        if (SettingsManager.isEnabled(GlobalSetting.HIDE_POINTS)) {
            return playerObjective.getBattleType().displayName().color(KatzeColor.LIGHT_GRAY)
                    .append(Component.text(": ", KatzeColor.GRAY))
                    .append(playerObjective.displayName().color(KatzeColor.BLUE));
        }

        return playerObjective.getBattleType().displayName().color(KatzeColor.LIGHT_GRAY)
                .append(Component.text(": ", KatzeColor.GRAY))
                .append(playerObjective.displayName().color(KatzeColor.BLUE))
                .append(Component.text(" | ", KatzeColor.GRAY))
                .append(Component.translatable(isPlayerInTeam ? "katze.forcebattle.team_points" : "katze.forcebattle.points").color(KatzeColor.LIGHT_GRAY))
                .append(Component.text(": ", KatzeColor.GRAY))
                .append(Component.text(points, KatzeColor.BLUE));
    }
}


