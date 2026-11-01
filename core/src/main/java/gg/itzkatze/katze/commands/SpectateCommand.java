package gg.itzkatze.katze.commands;

import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.chat.TeamChatManager;
import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.spectator.SpectatorManager;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

public class SpectateCommand extends KatzeCommand {

    public SpectateCommand() {
        super(
                "spectate",
                List.of(),
                CallerType.PLAYER,
                "/spectate",
                PermissionDefault.TRUE,
                "Spectate the next battle instead of playing in it"
        );
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        if (!(caller instanceof KatzePlayer player)) return;

        if (ChallengeManager.isGameStarted()) {
            caller.sendMessage(Component.translatable("katze.game.spectate.cannot_switch").color(KatzeColor.RED));
            return;
        }
        if (player.isSpectator()) {
            SpectatorManager.remove(player.getUuid());
            caller.sendMessage(Component.translatable("katze.game.spectate.play_next").color(KatzeColor.LIME));
            return;
        }
        SpectatorManager.choose(player.getUuid());
        player.leaveTeam();
        TeamChatManager.setTeamChatEnabled(player, false);
        caller.sendMessage(Component.translatable("katze.game.spectate.spectate_next").color(KatzeColor.LIME));
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        return List.of();
    }
}
