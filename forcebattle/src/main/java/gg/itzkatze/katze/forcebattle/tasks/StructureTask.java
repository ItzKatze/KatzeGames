package gg.itzkatze.katze.forcebattle.tasks;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.configuration.GlobalSetting;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.configuration.states.PlayerTeamBothState;
import gg.itzkatze.katze.data.KatzeStructure;
import gg.itzkatze.katze.data.StructureSimplified;
import gg.itzkatze.katze.forcebattle.ForceBattleObjective;
import gg.itzkatze.katze.forcebattle.ForceBattlePlayer;
import gg.itzkatze.katze.forcebattle.ForceBattleType;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.Toast;
import gg.itzkatze.katze.util.StructureScanner;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;


public class StructureTask implements ForceTask {

    public static void clearCache() {
        StructureScanner.clear();
    }

    @Override
    public void runTick() {
        if (ForceBattleType.FORCE_STRUCTURE.getSettingState() == PlayerTeamBothState.OFF) return;
        if (!KatzeGames.getTimer().isRunning()) return;

        Registry<Structure> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);

        for (KatzePlayer player : KatzePlayer.getOnlinePlayers()) {
            ForceBattlePlayer katzePlayer = (ForceBattlePlayer) player;
            if (!katzePlayer.isParticipating()) continue;

            checkPlayerObjective(katzePlayer, registry);
            checkTeamObjective(katzePlayer, registry);
        }
    }

    private void checkPlayerObjective(ForceBattlePlayer player, Registry<Structure> registry) {
        ForceBattleObjective objective = player.getObjective();

        if (objective == null) return;
        if (objective.getBattleType() != ForceBattleType.FORCE_STRUCTURE) return;

        if (isInsideStructure(player, objective.getObjectiveValue(), registry)) {
            completeObjective(player, objective);
            player.updateObjective(true, false);
        }
    }

    private void checkTeamObjective(ForceBattlePlayer player, Registry<Structure> registry) {
        if (!player.isInTeam()) return;
        ForceBattleObjective teamObjective = (ForceBattleObjective) player.getTeam().getObjective();

        if (teamObjective == null) return;
        if (teamObjective.getBattleType() != ForceBattleType.FORCE_STRUCTURE) return;

        if (isInsideStructure(player, teamObjective.getObjectiveValue(), registry)) {
            completeObjective(player, teamObjective);
            player.getTeam().updateObjective(player, true, false);
        }
    }

    private boolean isInsideStructure(ForceBattlePlayer player, Object objectiveValue, Registry<Structure> registry) {
        if (objectiveValue instanceof StructureSimplified simplified) {
            for (KatzeStructure structure : simplified.getStructures()) {
                Structure bukkitStructure = registry.get(structure.getKey());

                if (bukkitStructure != null && checkPlayerLocationInStructure(player, structure.getKey(), bukkitStructure)) {
                    return true;
                }
            }

            return false;
        }

        if (objectiveValue instanceof KatzeStructure structure) {
            Structure bukkitStructure = registry.get(structure.getKey());
            return bukkitStructure != null && checkPlayerLocationInStructure(player, structure.getKey(), bukkitStructure);
        }

        return false;
    }

    private boolean checkPlayerLocationInStructure(ForceBattlePlayer fbPlayer, NamespacedKey structureKey, Structure bukkitStructure) {
        Player player = fbPlayer.getPlayer();
        return player != null && StructureScanner.contains(player.getLocation(), structureKey);
    }
    private void completeObjective(KatzePlayer player, ForceBattleObjective objective) {
        Component formattedObjective = objective.displayName();
        Material toastIcon = Material.STRUCTURE_BLOCK;
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
