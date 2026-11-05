package gg.itzkatze.katze.commands;

import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.results.SavedResultsGUI;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

public final class ResultsCommand extends KatzeCommand {
    public ResultsCommand() {
        super("results", List.of(), CallerType.PLAYER, "/results", PermissionDefault.TRUE,
                "Browse saved rounds you played in");
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        if (args.length != 0) {
            sendUsage(caller);
            return;
        }
        if (caller instanceof KatzePlayer player) new SavedResultsGUI().open(player);
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        return List.of();
    }
}
