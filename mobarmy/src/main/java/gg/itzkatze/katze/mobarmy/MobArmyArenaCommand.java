package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.player.KatzePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

final class MobArmyArenaCommand extends KatzeCommand {
    private final MobArmyGame game;
    private final boolean tracking;

    MobArmyArenaCommand(MobArmyGame game, boolean tracking) {
        super(tracking ? "tracker" : "watch", List.of(), CallerType.PLAYER, tracking ? "/tracker" : "/watch <player>",
                PermissionDefault.TRUE, tracking ? "Track the last arena mobs" : "Watch a remaining MobArmy team after finishing");
        this.game = game;
        this.tracking = tracking;
    }

    @Override protected void executeCommand(CommandCaller caller, String[] args) {
        if (!(caller instanceof KatzePlayer wrapper) || wrapper.getPlayer() == null) return;
        if (tracking) { game.tracker.give(wrapper.getPlayer()); return; }
        if (!game.canWatch(wrapper.getPlayer())) { caller.sendMessage(Component.translatable("katze.mobarmy.watch.unavailable")); return; }
        if (args.length != 1) { sendUsage(caller); return; }
        if (!game.watch(wrapper.getPlayer(), args[0])) caller.sendMessage(Component.translatable("katze.mobarmy.watch.missing"));
    }

    @Override protected List<String> tabComplete(CommandCaller caller, String[] args) {
        if (tracking || !(caller instanceof KatzePlayer wrapper) || wrapper.getPlayer() == null || !game.canWatch(wrapper.getPlayer())) return List.of();
        return args.length == 1 ? game.watchTargets().stream().map(player -> player.getName()).toList() : List.of();
    }
}
