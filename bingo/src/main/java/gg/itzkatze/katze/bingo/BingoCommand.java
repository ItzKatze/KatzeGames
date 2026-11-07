package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.bingo.gui.BingoBoardGUI;
import gg.itzkatze.katze.bingo.gui.BingoMatchesGUI;
import gg.itzkatze.katze.bingo.match.BingoMatchManager;
import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.player.KatzePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;
import java.util.UUID;

public final class BingoCommand extends KatzeCommand {
    private final BingoMatchManager matches;
    public BingoCommand(BingoMatchManager matches) {
        super("bingo", List.of(), CallerType.PLAYER, "/bingo [matches|overlay|draw <match-id>]", PermissionDefault.TRUE, "Open your Bingo board");
        this.matches = matches;
    }
    @Override public void executeCommand(CommandCaller caller, String[] args) {
        if (!(caller instanceof KatzePlayer player)) return;
        if (!matches.packReady(player.getUuid())) { caller.sendMessage(Component.translatable("katze.bingo.error.pack_wait")); return; }
        if (args.length > 0 && args[0].equalsIgnoreCase("overlay")) {
            matches.toggleOverlay(player.getUuid());
            caller.sendMessage(Component.translatable(matches.hidden(player.getUuid()) ? "katze.bingo.overlay.hidden" : "katze.bingo.overlay.shown"));
            return;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("draw")) {
            if (!caller.hasPermission(PermissionDefault.OP)) { caller.sendMessage(Component.translatable("katze.command.no_permission")); return; }
            try {
                var match = matches.find(UUID.fromString(args[1]));
                if (match == null) throw new IllegalArgumentException();
                matches.adminDraw(match);
            } catch (IllegalArgumentException exception) { caller.sendMessage(Component.translatable("katze.bingo.error.match")); }
            return;
        }
        if (args.length > 0 && !args[0].equalsIgnoreCase("matches")) { sendUsage(caller); return; }
        var match = matches.viewed(player.getUuid());
        if (match != null && args.length == 0) new BingoBoardGUI(matches, match.id).open(player);
        else new BingoMatchesGUI(matches, 0).open(player);
    }
    @Override public List<String> tabComplete(CommandCaller caller, String[] args) {
        if (args.length == 1) return List.of("matches", "overlay");
        if (args.length == 2 && args[0].equalsIgnoreCase("draw") && caller.hasPermission(PermissionDefault.OP))
            return matches.matches().stream().map(match -> match.id.toString()).toList();
        return List.of();
    }
}
