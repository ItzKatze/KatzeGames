package gg.itzkatze.katze.commands.framework;

import gg.itzkatze.katze.player.KatzePlayer;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class CommandHandler implements BasicCommand {

    private final String commandId;

    CommandHandler(String commandId) {
        this.commandId = commandId;
    }

    public static void install(JavaPlugin plugin) {
        if (!(plugin.getPluginMeta() instanceof PluginDescriptionFile description)) {
            throw new IllegalStateException("Command declarations require plugin.yml metadata");
        }
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            description.getCommands().forEach((name, properties) -> {
                Object aliases = properties.get("aliases");
                List<String> aliasNames = switch (aliases) {
                    case Collection<?> values -> values.stream().map(Object::toString).toList();
                    case null -> List.of();
                    default -> List.of(aliases.toString());
                };
                String help = KatzeCommand.getCommandById(name).map(KatzeCommand::getDescription).orElse(null);
                CommandHandler handler = new CommandHandler(name);
                event.registrar().register(name, help, List.of(), handler);
                for (String alias : aliasNames) {
                    event.registrar().register(alias, help, List.of(), handler);
                }
            });
        });
    }

    private static @NotNull CommandCaller createCaller(CommandSender sender) {
        if (sender instanceof Player player) {
            return KatzePlayer.adapt(player);
        } else if (sender instanceof ConsoleCommandSender) {
            return BukkitConsoleCommandCaller.instance();
        } else if (sender instanceof BlockCommandSender block) {
            return BukkitBlockCommandCaller.adapt(block);
        }
        throw new IllegalArgumentException("Unknown sender type: " + sender.getClass());
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        KatzeCommand.execute(commandId, createCaller(source.getSender()), args);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        return KatzeCommand.tabComplete(commandId, createCaller(source.getSender()), args);
    }
}
