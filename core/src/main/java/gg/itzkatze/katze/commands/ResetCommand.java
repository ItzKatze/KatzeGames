package gg.itzkatze.katze.commands;

import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.game.GameLifecycleManager;
import gg.itzkatze.katze.game.GameWorldManager;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ResetCommand extends KatzeCommand {

    public ResetCommand() {
        super(
                "reset",
                List.of(),
                CallerType.NONE,
                "/reset",
                PermissionDefault.OP,
                "Command to reset the configured game"
        );
    }

    @Override
    public void executeCommand(@NotNull CommandCaller caller, String[] args) {
        if (GameWorldManager.isResetting()) {
            caller.sendMessage(Component.translatable("katze.game.reset.in_progress").color(KatzeColor.RED));
            return;
        }

        caller.sendMessage(Component.translatable("katze.game.reset.started").color(KatzeColor.YELLOW));
        GameLifecycleManager.resetGame(
                () -> caller.sendMessage(Component.translatable("katze.game.reset.success").color(KatzeColor.LIME)),
                () -> caller.sendMessage(Component.translatable("katze.game.reset.failed").color(KatzeColor.RED)));
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        return List.of();
    }
}
