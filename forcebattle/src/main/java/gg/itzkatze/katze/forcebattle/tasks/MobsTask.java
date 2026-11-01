package gg.itzkatze.katze.forcebattle.tasks;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.configuration.GlobalSetting;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.configuration.states.PlayerTeamBothState;
import gg.itzkatze.katze.data.MobsSimplified;
import gg.itzkatze.katze.forcebattle.ForceBattleObjective;
import gg.itzkatze.katze.forcebattle.ForceBattlePlayer;
import gg.itzkatze.katze.forcebattle.ForceBattleType;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.Toast;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MobsTask {

    public static void onDamage(EntityDamageByEntityEvent event) {
        if (ForceBattleType.FORCE_MOB.getSettingState() == PlayerTeamBothState.OFF) return;

        Player damager;

        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (event.getDamager() instanceof Fireball || event.getDamager() instanceof Arrow) {
            // Not Fireball#getShooter(): Paper caches it and a player hitting the fireball back does not update the cache
            if (event.getDamageSource().getCausingEntity() instanceof Player player) {
                damager = player;
            } else return;
        } else if (event.getDamager() instanceof Player player) {
            damager = player;
        } else return;

        if (!(KatzePlayer.adapt(damager) instanceof ForceBattlePlayer katzePlayer)) return;
        if (!katzePlayer.isParticipating()) return;

        if (target.getHealth() - event.getDamage() > 0) return;

        checkPlayerObjective(katzePlayer, target.getType());
        checkTeamObjective(katzePlayer, target.getType());
    }

    private static void checkPlayerObjective(ForceBattlePlayer player, EntityType target) {
        ForceBattleObjective objective = player.getObjective();
        if (objective == null) return;
        if (objective.getBattleType() != ForceBattleType.FORCE_MOB) return;

        if (isMobMatch(objective.getObjectiveValue(), target)) {
            completeObjective(player, objective);
            player.updateObjective(true, false);
        }
    }

    private static void checkTeamObjective(ForceBattlePlayer player, EntityType target) {
        if (!player.isInTeam()) return;
        ForceBattleObjective teamObjective = (ForceBattleObjective) player.getTeam().getObjective();
        if (teamObjective == null) return;
        if (teamObjective.getBattleType() != ForceBattleType.FORCE_MOB) return;

        if (isMobMatch(teamObjective.getObjectiveValue(), target)) {
            completeObjective(player, teamObjective);
            player.getTeam().updateObjective(player, true, false);
        }
    }

    private static boolean isMobMatch(Object objectiveValue, EntityType target) {
        if (objectiveValue instanceof MobsSimplified simplified) {
            return simplified.getMobs().contains(target);
        }

        if (objectiveValue instanceof EntityType entityType) {
            return entityType == target;
        }

        return false;
    }

    private static void completeObjective(ForceBattlePlayer player, ForceBattleObjective objective) {
        Component formattedObjective = objective.displayName();
        Material toastIcon = Material.DIAMOND_SWORD;
        KatzeGames plugin = KatzeGames.get();

        if (player.isInTeam() && SettingsManager.isEnabled(GlobalSetting.EXTRA_TEAM_OBJECTIVE)) {
            player.getTeam().getPlayers().stream()
                    .filter(teammate -> teammate != player)
                    .forEach(teammate -> {
                        teammate.sendMessage(Component.translatable("katze.forcebattle.task.teammate_finished", gg.itzkatze.katze.text.TextArg.text("player", player.getName()), gg.itzkatze.katze.text.TextArg.component("objective", formattedObjective)));
                        Player teammatePlayer = teammate.getPlayer();
                        if (teammatePlayer != null) {
                            Toast.display(teammatePlayer, toastIcon, formattedObjective.color(KatzeColor.BLUE), Toast.Style.GOAL, plugin);
                        }
                    });
        }

        player.sendMessage(Component.translatable("katze.forcebattle.task.personal_finished", gg.itzkatze.katze.text.TextArg.component("objective", formattedObjective)));
        Player playerObj = player.getPlayer();
        if (playerObj != null) {
            Toast.display(playerObj, toastIcon, formattedObjective.color(KatzeColor.BLUE), Toast.Style.GOAL, plugin);
        }
    }
}
