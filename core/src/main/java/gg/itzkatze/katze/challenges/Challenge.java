package gg.itzkatze.katze.challenges;

import gg.itzkatze.katze.bossbar.BossbarContent;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.game.seed.SeedRequirement;
import gg.itzkatze.katze.scoreboard.ScoreboardContent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface Challenge extends ChallengeSettings {

    default boolean allowsGameplay(ChallengeType type) { return true; }

    default boolean allowsCombination(ChallengeType type) { return true; }

    default boolean locksChallengeSelection() { return false; }

    default boolean requiresCombatDamage() { return false; }

    @Nullable default Inventory getBackpack(KatzePlayer player) { return null; }

    default void onEnable() {}

    @Nullable default Component getEnableError() { return null; }

    @Nullable default Component getStartError() { return null; }

    default boolean showScoreboard(@NotNull KatzePlayer player) { return false; }

    default void onDisable() {}

    default void onGameStart() {}

    default void onGameReset() {}

    default List<SeedRequirement> getSeedRequirements() {
        return List.of();
    }

    default void onLobbyJoin(@NotNull KatzePlayer player) {}

    default void saveData() {}

    default void loadData() {}

    default void validateRestoredGame() {}

    @Nullable default TimerSpec getTimerSpec() {
        return null;
    }

    default void onTimerElapsed() {}

    @NotNull ChallengeType getType();

    @NotNull List<KatzeCommand> getCommands();

    @NotNull List<Listener> getListeners();

    @NotNull List<Runnable> getTasks();

    @Nullable default Component getActionbar(@NotNull KatzePlayer player, boolean timerRunning, int timerSeconds) {
        return null;
    }

    default int getActionbarPriority() {
        return 0;
    }

    @NotNull default List<Component> getSpectatorInfo(@NotNull KatzePlayer target) {
        return Collections.emptyList();
    }

    @Nullable default Component getNametagSuffix(@NotNull KatzePlayer target) {
        return null;
    }

    @Nullable default ScoreboardContent getScoreboardContent() {
        return null; // Default implementation returns null
    }

    @NotNull default List<BossbarContent> getBossbarContents() {
        return Collections.emptyList();
    }
}
