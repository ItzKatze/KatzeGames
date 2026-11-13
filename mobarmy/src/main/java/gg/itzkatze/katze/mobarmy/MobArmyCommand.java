package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.player.KatzePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

final class MobArmyCommand extends KatzeCommand {

    private final MobArmyGame game;
    private final boolean backpack;

    private MobArmyCommand(MobArmyGame game, boolean backpack) {
        super(
                backpack ? "backpack" : "waves", backpack ? List.of("bp") : List.of(), CallerType.PLAYER,
                backpack ? "/backpack" : "/waves [details <player>|forfeit <player>]", PermissionDefault.TRUE,
                backpack ? "Open your MobArmy backpack" : "Configure MobArmy waves or view results"
        );
        this.game = game;
        this.backpack = backpack;
    }

    static List<KatzeCommand> commands(MobArmyGame game) {
        return List.of(new MobArmyCommand(game, false), new MobArmyCommand(game, true), new MobArmyArenaCommand(game, true), new MobArmyArenaCommand(game, false));
    }

    @Override
    protected void executeCommand(CommandCaller caller, String[] args) {
        if (!(caller instanceof KatzePlayer player) || player.getPlayer() == null || game.match == null) return;
        var side = game.side(player.getUuid());
        if (args.length == 2 && !backpack) {
            var selected = game.match.findSide(args[1]);
            if (selected != null && args[0].equalsIgnoreCase("details") && game.match.phase == MobArmyMatch.Phase.RESULTS) {
                new MobArmyMenus.Configure(game, selected, -1, 0).open(player);
                return;
            }
            if (selected != null && args[0].equalsIgnoreCase("forfeit") && player.hasPermission(PermissionDefault.OP)) {
                game.forfeit(selected);
                return;
            }
        }
        if (side != null && backpack && game.backpack(side).accessible(player.getPlayer())) {
            game.backpack(side).open(player);
        } else if (side != null && !backpack && game.running() && game.match.phase == MobArmyMatch.Phase.CONFIGURATION) {
            new MobArmyMenus.Waves(game, side).open(player);
        } else caller.sendMessage(Component.translatable("katze.mobarmy.error.phase"));
    }

    @Override
    protected List<String> tabComplete(CommandCaller caller, String[] args) {
        if (backpack || game.match == null) return List.of();
        if (args.length == 1) return caller.hasPermission(PermissionDefault.OP) ? List.of("details", "forfeit") : List.of("details");
        if (args.length == 2) return game.match.sides.stream().flatMap(side -> side.members.values().stream()).toList();
        return List.of();
    }

}
