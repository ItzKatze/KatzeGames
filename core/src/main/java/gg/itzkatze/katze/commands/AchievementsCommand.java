package gg.itzkatze.katze.commands;

import gg.itzkatze.katze.achievements.AchievementGui;
import gg.itzkatze.katze.achievements.AchievementService;
import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;
import java.util.Locale;

public final class AchievementsCommand extends KatzeCommand {
    public AchievementsCommand() {
        super("achievements", List.of(), CallerType.PLAYER, "/achievements [names <auto|custom>]", PermissionDefault.TRUE, "Browse your achievements");
    }

    @Override
    public void executeCommand(CommandCaller caller, String[] args) {
        if (!(caller instanceof KatzePlayer player)) return;
        if (args.length == 0) {
            new AchievementGui().open(player);
            return;
        }
        if (args.length != 2 || !args[0].equalsIgnoreCase("names") || !List.of("auto", "custom", "pdf", "insider").contains(args[1].toLowerCase(Locale.ROOT))) {
            sendUsage(caller);
            return;
        }
        boolean custom = !args[1].equalsIgnoreCase("auto");
        if (!AchievementService.get().setCustomNames(player.getUuid(), player.getName(), custom)) {
            player.sendMessage(Component.translatable("katze.achievements.names.save_failed").color(KatzeColor.RED));
            return;
        }
        player.sendMessage(Component.translatable("katze.achievements.names.selected",
                TextArg.component("mode", Component.translatable("katze.achievements.names." + (custom ? "custom" : "auto")))).color(KatzeColor.LIME));
        new AchievementGui().open(player);
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        List<String> choices = args.length == 1 ? List.of("names")
                : args.length == 2 && args[0].equalsIgnoreCase("names") ? List.of("auto", "custom") : List.of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(choice -> choice.startsWith(prefix)).toList();
    }
}
