package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.bingo.board.BingoObjectiveCatalog;
import gg.itzkatze.katze.bingo.match.BingoMatch;
import gg.itzkatze.katze.bingo.gui.BingoSettingsGUI;
import gg.itzkatze.katze.bingo.match.BingoMatchManager;
import gg.itzkatze.katze.bingo.pack.BingoResourcePack;
import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.challenges.TimerSpec;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.configuration.ChallengeSettingsManager;
import gg.itzkatze.katze.game.GameTimer;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.scoreboard.ScoreboardContent;
import gg.itzkatze.katze.recipe.RecipeCommand;
import gg.itzkatze.katze.util.StringUtility;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Listener;

import java.util.List;

public final class Bingo implements Challenge {
    private final BingoResourcePack pack = new BingoResourcePack();
    private final BingoMatchManager matches = new BingoMatchManager(new BingoObjectiveCatalog(), pack);
    private final ChallengeSettingsManager settings = new ChallengeSettingsManager(this, BingoSettings.Option.class);
    private final BingoScoreboard scoreboard = new BingoScoreboard(matches);

    @Override public Component getEnableError() { return pack.configurationError(); }
    @Override public Component getStartError() { return matches.startError(); }
    @Override public void onEnable() { matches.load(); pack.enable(); }
    @Override public void onDisable() { pack.disable(); }
    @Override public void onGameStart() { matches.start(); }
    @Override public void onGameReset() { matches.reset(); }
    @Override public void saveData() { matches.save(); }
    @Override public void loadData() { matches.load(); }
    @Override public void validateRestoredGame() {
        if (!matches.started()) throw new IllegalStateException("The saved Bingo round is missing bingo.json match data");
        matches.catalog().enabled();
    }
    @Override public ChallengeType getType() { return ChallengeType.BINGO; }
    @Override public List<KatzeCommand> getCommands() { return List.of(new BingoCommand(matches), new RecipeCommand()); }
    @Override public List<Listener> getListeners() { return List.of(pack, new BingoListener(matches)); }
    @Override public List<Runnable> getTasks() { return List.of(matches::tick); }
    @Override public TimerSpec getTimerSpec() { return new TimerSpec(GameTimer.Mode.COUNT_UP, 0); }
    @Override public ScoreboardContent getScoreboardContent() { return scoreboard; }
    @Override public ChallengeSettingsManager getSettingsManager() { return settings; }
    @Override public KatzeGUI getSettingsGUI() { return new BingoSettingsGUI(); }
    @Override public boolean showScoreboard(KatzePlayer player) {
        return matches.packReady(player.getUuid()) && matches.viewed(player.getUuid()) != null && !matches.hidden(player.getUuid());
    }
    @Override public Component getActionbar(KatzePlayer player, boolean running, int seconds) {
        if (!ChallengeManager.isGameStarted()) return Component.empty();
        var match = matches.viewed(player.getUuid());
        if (match == null || match.result != BingoMatch.Result.ACTIVE || match.remaining < 0) return Component.empty();
        return Component.text(StringUtility.formatTime(match.remaining), KatzeColor.WHITE);
    }
    @Override public int getActionbarPriority() { return 200; }
    @Override public List<Component> getSpectatorInfo(KatzePlayer target) {
        var match = matches.assigned(target.getUuid());
        return match == null ? List.of() : List.of(scoreboard.status(match));
    }
}
