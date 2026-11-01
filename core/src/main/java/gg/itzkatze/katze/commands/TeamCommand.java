package gg.itzkatze.katze.commands;

import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.team.KatzeTeam;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.StringUtility;
import gg.itzkatze.katze.util.TabCompletions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TeamCommand extends KatzeCommand {

    public TeamCommand() {
        super(
                "team",
                List.of(),
                CallerType.PLAYER,
                "/team <create|join|leave|list|accept|reject> <id>",
                PermissionDefault.TRUE,
                "Command to manage teams: create, join, etc."
        );
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        if (args.length < 1) {
            sendUsage(caller);
            return;
        }

        @NotNull Optional<KatzePlayer> battlePlayerOpt = KatzePlayer.adapt(caller.getName());
        if (battlePlayerOpt.isEmpty()) {
            return;
        }
        KatzePlayer katzePlayer = battlePlayerOpt.get();

        if (!args[0].equalsIgnoreCase("list") && ChallengeManager.isGameStarted()) {
            caller.sendMessage(Component.translatable("katze.team.game_started").color(KatzeColor.RED));
            return;
        }
        if ((args[0].equals("create") || args[0].equals("join")) && katzePlayer.isSpectator()) {
            caller.sendMessage(Component.translatable("katze.team.spectator_cannot_join").color(KatzeColor.RED));
            return;
        }

        switch (args[0]) {
            case "create" -> {
                katzePlayer.leaveTeam();
                KatzeTeam team = KatzeTeam.create(List.of(katzePlayer));
                caller.sendMessage(Component.translatable("katze.team.created", gg.itzkatze.katze.text.TextArg.number("id", team.getId())));
            }
            case "join" -> {
                if (args.length < 2) {
                    caller.sendMessage(Component.translatable("katze.team.invalid_id").color(KatzeColor.RED));
                    return;
                }
                int teamId;
                try {
                    teamId = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    caller.sendMessage(Component.translatable("katze.team.invalid_id").color(KatzeColor.RED));
                    return;
                }

                Optional<KatzeTeam> teamOpt = KatzeTeam.ofId(teamId);
                teamOpt.ifPresentOrElse(
                        team -> team.sendJoinRequest(katzePlayer),
                        () -> caller.sendMessage(Component.translatable("katze.team.invalid_id").color(KatzeColor.RED))
                );
            }
            case "leave" -> {
                katzePlayer.leaveTeam();
            }
            case "accept" -> {
                KatzeTeam team = katzePlayer.getTeam();
                if (team == null) {
                    caller.sendMessage(Component.translatable("katze.team.not_in_team").color(KatzeColor.LIGHT_GRAY));
                    return;
                }
                if (team.getLastJoinRequest() != null) {
                    KatzePlayer requester = team.getLastJoinRequest().katzePlayer();
                    if (requester.isSpectator()) {
                        caller.sendMessage(Component.translatable("katze.team.requester_spectating", gg.itzkatze.katze.text.TextArg.text("name", requester.getName())).color(KatzeColor.RED));
                        team.finishLastJoinRequest(false);
                        return;
                    }
                    team.finishLastJoinRequest(true);
                }
            }
            case "reject" -> {
                KatzeTeam team = katzePlayer.getTeam();
                if (team == null) {
                    caller.sendMessage(Component.translatable("katze.team.not_in_team").color(KatzeColor.LIGHT_GRAY));
                    return;
                }
                if (team.getLastJoinRequest() != null) {
                    team.finishLastJoinRequest(false);
                }
            }
            case "list" -> {
                Component teamList = Component.translatable("katze.team.list.title").color(KatzeColor.BLUE).decorate(TextDecoration.BOLD);
                for (KatzeTeam team : KatzeTeam.teams) {
                    teamList = teamList.append(Component.text("\n"))
                            .append(Component.text(team.getId() + ":", KatzeColor.YELLOW));
                    List<KatzePlayer> teamPlayers = team.getPlayers();
                    for (int i = 0; i < teamPlayers.size(); i++) {
                        KatzePlayer teamPlayer = teamPlayers.get(i);
                        if (teamPlayer == null || teamPlayer.isOffline()) {
                            continue;
                        }
                        teamList = teamList.append(Component.text(" " + teamPlayer.getName(), KatzeColor.BLUE));
                        if (i < teamPlayers.size() - 1) {
                            teamList = teamList.append(Component.text(",", KatzeColor.BLUE));
                        }
                    }
                }

                caller.sendMessage(teamList);
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String @NotNull [] args) {
        if (args.length == 1) {
            return StringUtility.copyPartialMatches(args[0], List.of("create", "list", "join", "leave", "accept", "reject"), new ArrayList<>());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return StringUtility.copyPartialMatches(args[1], TabCompletions.getTeamIdTabCompletions(), new ArrayList<>());
        }
        return List.of();
    }

}
