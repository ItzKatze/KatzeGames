package gg.itzkatze.katze.commands;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.game.GameLifecycleManager;
import gg.itzkatze.katze.game.GameTimer;
import gg.itzkatze.katze.game.SessionPersistence;
import gg.itzkatze.katze.game.StartCountdown;
import gg.itzkatze.katze.spectator.SpectatorManager;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class StartCommand extends KatzeCommand {

    public StartCommand() {
        super(
                "start",
                List.of(),
                CallerType.NONE,
                "/start",
                PermissionDefault.OP,
                "Command to start the configured game"
        );
    }

    @Override
    public void executeCommand(@NotNull CommandCaller caller, String[] args) {
        if (StartCountdown.isRunning()) {
            caller.sendMessage(Component.translatable("katze.game.start.already_starting").color(KatzeColor.RED));
            return;
        }
        if (!canStart(caller)) return;

        caller.sendMessage(Component.translatable("katze.game.start.countdown", gg.itzkatze.katze.text.TextArg.number("seconds", StartCountdown.SECONDS)).color(KatzeColor.LIME));
        StartCountdown.begin(() -> start(caller));
    }

    private static boolean canStart(@NotNull CommandCaller caller) {
        if (ChallengeManager.getActiveChallenges().isEmpty()) {
            caller.sendMessage(Component.translatable("katze.game.start.no_challenge").color(KatzeColor.RED));
            return false;
        }
        if (ChallengeManager.isGameStarted()) {
            caller.sendMessage(Component.translatable("katze.game.start.already_started").color(KatzeColor.RED));
            return false;
        }
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (!online.isEmpty() && online.stream().allMatch(player -> SpectatorManager.isSpectator(player.getUniqueId()))) {
            caller.sendMessage(Component.translatable("katze.game.start.everyone_spectating").color(KatzeColor.RED));
            return false;
        }
        return true;
    }

    private static void start(@NotNull CommandCaller caller) {
        if (!canStart(caller)) return;

        GameLifecycleManager.startGame();
        if (!ChallengeManager.startActiveChallenges()) {
            GameLifecycleManager.resetGame();
            String reason = ChallengeManager.getLastStartError();
            caller.sendMessage(reason == null ? Component.translatable("katze.game.start.failed").color(KatzeColor.RED) : Component.text(reason).color(KatzeColor.RED));
            return;
        }
        if (KatzeGames.getTimer().getMode() != GameTimer.Mode.DISABLED) {
            KatzeGames.getTimer().start();
        }
        SessionPersistence.checkpoint();
        caller.sendMessage(Component.translatable("katze.game.start.success").color(KatzeColor.LIME));
    }

    @Override
    public List<String> tabComplete(CommandCaller caller, String[] args) {
        return List.of();
    }
}
