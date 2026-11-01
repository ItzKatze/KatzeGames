package gg.itzkatze.katze.commands;

import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.lobby.Lobby;
import gg.itzkatze.katze.lobby.LobbyIsland;
import gg.itzkatze.katze.lobby.LobbyManager;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.StringUtility;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SetSpawnCommand extends KatzeCommand {

    public SetSpawnCommand() {
        super(
                "setspawn",
                List.of(),
                CallerType.NONE,
                "/setspawn [island]",
                PermissionDefault.OP,
                "Command to switch the lobby to another island"
        );
    }

    @Override
    public void executeCommand(@NotNull CommandCaller caller, String[] args) {
        if (args.length > 1) {
            sendUsage(caller);
            return;
        }
        Lobby lobby = LobbyManager.getActiveLobby();
        if (lobby == null) {
            fail(caller, Component.translatable("katze.command.setspawn.no_lobby"));
            return;
        }

        LobbyIsland target = args.length == 0 ? LobbyManager.pickOtherIsland(ThreadLocalRandom.current()) : findIsland(args[0]);
        if (target == null) {
            fail(caller, args.length == 0
                    ? Component.translatable("katze.command.setspawn.no_other_island")
                    : Component.translatable("katze.command.setspawn.unknown_island",
                            gg.itzkatze.katze.text.TextArg.text("island", args[0]),
                            gg.itzkatze.katze.text.TextArg.text("islands", String.join(", ", islandIds()))));
            return;
        }
        if (target.getId().equals(lobby.getIsland().getId())) {
            fail(caller, Component.translatable("katze.command.setspawn.already_active", gg.itzkatze.katze.text.TextArg.text("island", target.getId())));
            return;
        }
        if (!LobbyManager.switchIsland(target)) {
            fail(caller, Component.translatable("katze.command.setspawn.load_failed", gg.itzkatze.katze.text.TextArg.text("island", target.getId())));
            return;
        }
        caller.sendMessage(Component.translatable("katze.command.setspawn.success", gg.itzkatze.katze.text.TextArg.text("island", target.getId())).color(KatzeColor.LIME));
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String @NotNull [] args) {
        Lobby lobby = LobbyManager.getActiveLobby();
        if (lobby == null || args.length != 1) {
            return List.of();
        }
        String current = lobby.getIsland().getId();
        List<String> others = islandIds().stream().filter(id -> !id.equals(current)).toList();
        return StringUtility.copyPartialMatches(args[0], others, new ArrayList<>());
    }

    private static @Nullable LobbyIsland findIsland(@NotNull String id) {
        return LobbyManager.getIslands().stream().filter(island -> island.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    private static @NotNull List<String> islandIds() {
        return LobbyManager.getIslands().stream().map(LobbyIsland::getId).toList();
    }

    private static void fail(@NotNull CommandCaller caller, @NotNull Component message) {
        caller.sendMessage(message.color(KatzeColor.RED));
    }
}
