package gg.itzkatze.katze.forcebattle.tasks;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.forcebattle.ForceBattleObjective;
import gg.itzkatze.katze.configuration.GlobalSetting;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.configuration.states.PlayerTeamBothState;
import gg.itzkatze.katze.forcebattle.ForceBattlePlayer;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.forcebattle.ForceBattleType;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.Toast;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

public class ItemTask implements ForceTask {

    @Override
    public void runTick() {
        if (ForceBattleType.FORCE_ITEM.getSettingState() == PlayerTeamBothState.OFF) return;
        if (!KatzeGames.getTimer().isRunning()) return;

        for (KatzePlayer player : KatzePlayer.getOnlinePlayers()) {
            ForceBattlePlayer katzePlayer = (ForceBattlePlayer) player;
            if (!katzePlayer.isParticipating()) continue;

            checkPlayerObjective(katzePlayer);
            checkTeamObjective(katzePlayer);
        }
    }

    private void checkPlayerObjective(ForceBattlePlayer player) {
        ForceBattleObjective objective = player.getObjective();
        if (objective == null) return;
        if (objective.getBattleType() != ForceBattleType.FORCE_ITEM) return;

        Material material = objective.getObjectiveValue(Material.class);
        if (material != null && hasRequiredItem(player, material)) {
            completeObjective(player, objective, material);
            player.updateObjective(true, false);
        }
    }

    private void checkTeamObjective(ForceBattlePlayer player) {
        if (!player.isInTeam()) return;
        ForceBattleObjective teamObjective = (ForceBattleObjective) player.getTeam().getObjective();
        if (teamObjective == null) return;
        if (teamObjective.getBattleType() != ForceBattleType.FORCE_ITEM) return;

        Material material = teamObjective.getObjectiveValue(Material.class);
        if (material != null && hasRequiredItem(player, material)) {
            completeObjective(player, teamObjective, material);
            player.getTeam().updateObjective(player, true, false);
        }
    }

    private boolean hasRequiredItem(ForceBattlePlayer player, Material material) {
        return player.getInventory().contains(material);
    }

    private void completeObjective(ForceBattlePlayer player, ForceBattleObjective objective, Material material) {
        Component formattedObjective = objective.displayName();
        Material toastIcon = material != null ? material : Material.BOOK;
        KatzeGames plugin = KatzeGames.get();

        if (player.isInTeam() && SettingsManager.isEnabled(GlobalSetting.EXTRA_TEAM_OBJECTIVE)) {
            player.getTeam().getPlayers().stream()
                    .filter(teammate -> teammate != player)
                    .filter(teammate -> !teammate.isOffline())
                    .forEach(teammate -> {
                        teammate.sendMessage(Component.translatable("katze.forcebattle.task.teammate_finished", gg.itzkatze.katze.text.TextArg.text("player", player.getName()), gg.itzkatze.katze.text.TextArg.component("objective", formattedObjective)));
                        Toast.display(teammate.getPlayer(), toastIcon, formattedObjective.color(KatzeColor.BLUE), Toast.Style.GOAL, plugin);
                    });
        }

        player.sendMessage(Component.translatable("katze.forcebattle.task.personal_finished", gg.itzkatze.katze.text.TextArg.component("objective", formattedObjective)));
        Toast.display(player.getPlayer(), toastIcon, formattedObjective.color(KatzeColor.BLUE), Toast.Style.GOAL, plugin);
    }
}
