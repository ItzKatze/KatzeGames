package gg.itzkatze.katze.commands;

import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.game.GameLifecycleManager;
import gg.itzkatze.katze.game.GameWorldManager;
import gg.itzkatze.katze.game.StartCountdown;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;
import java.util.Locale;

public final class MapCommand extends KatzeCommand {

    private static final List<String> DESTINATIONS = List.of("overworld", "nether", "end", "limbo");

    public MapCommand() {
        super("map", List.of(), CallerType.PLAYER, "/map [overworld|nether|end|limbo]", PermissionDefault.OP,
                "Visit the next game map while players wait in limbo");
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        if (!(caller instanceof KatzePlayer player) || player.getPlayer() == null) return;
        String destination = args.length == 0 ? "overworld" : args[0].toLowerCase(Locale.ROOT);
        if (args.length > 1 || !DESTINATIONS.contains(destination)) {
            sendUsage(caller);
            return;
        }
        if (ChallengeManager.isGameStarted() || StartCountdown.isRunning()) {
            caller.sendMessage(Component.translatable("katze.game.map.game_started").color(KatzeColor.RED));
            return;
        }
        if (destination.equals("limbo")) {
            GameLifecycleManager.sendToLobby(player);
            return;
        }
        if (!GameWorldManager.isReady()) {
            caller.sendMessage(Component.translatable("katze.game.reset.not_ready").color(KatzeColor.RED));
            return;
        }
        World.Environment environment = switch (destination) {
            case "nether" -> World.Environment.NETHER;
            case "end" -> World.Environment.THE_END;
            default -> World.Environment.NORMAL;
        };
        World world = GameWorldManager.getWorld(environment);
        if (world == null) {
            caller.sendMessage(Component.translatable("katze.game.reset.not_ready").color(KatzeColor.RED));
            return;
        }
        Player online = player.getPlayer();
        online.closeInventory();
        if (!online.teleport(world.getSpawnLocation())) return;
        online.getInventory().clear();
        online.setGameMode(GameMode.CREATIVE);
        caller.sendMessage(Component.translatable("katze.game.map.visiting").color(KatzeColor.LIME));
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return DESTINATIONS.stream().filter(destination -> destination.startsWith(prefix)).toList();
    }
}
