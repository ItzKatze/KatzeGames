package gg.itzkatze.katze.lobby;

import gg.itzkatze.katze.level.LevelProgress;
import gg.itzkatze.katze.level.LevelStore;
import gg.itzkatze.katze.level.LevelTag;
import gg.itzkatze.katze.lobby.cosmetics.CosmeticStore;
import gg.itzkatze.katze.lobby.jumpandrun.JumpAndRun;
import gg.itzkatze.katze.lobby.leaderbord.Leaderboard;
import gg.itzkatze.katze.lobby.leaderbord.LeaderboardMetric;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.scoreboard.ScoreboardContent;
import gg.itzkatze.katze.scoreboard.ScoreboardManager;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;
import java.util.UUID;

public final class PointsSidebar implements ScoreboardContent {

    public static void install(@NotNull CosmeticStore store, @NotNull LevelStore levels) {
        store.addBalanceListener(PointsSidebar::refresh);
        levels.addChangeListener(PointsSidebar::refresh);
    }

    public static void refresh(@NotNull UUID player) {
        KatzePlayer.adapt(player).ifPresent(ScoreboardManager::refreshLobby);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("katze.cosmetics.run_title");
    }

    @Override
    public @NotNull Component[] getLines(@NotNull KatzePlayer player) {
        CosmeticStore store = CosmeticStore.get();
        UUID id = player.getUuid();
        OptionalInt score = currentRun(id);
        LevelProgress level = LevelStore.get().progress(id);
        return new Component[]{
                Component.text(" "),
                stat("katze.cosmetics.stat.level", Component.text(level.level(), LevelTag.colorOf(level.level()))),
                stat("katze.cosmetics.stat.xp", level.cost() == 0 ? Component.translatable("katze.level.max") : Component.text(level.into() + "/" + level.cost(), KatzeColor.WHITE)),
                Component.text(" "),
                stat("katze.cosmetics.stat.current_run", score.isPresent() ? Component.text(score.getAsInt(), KatzeColor.WHITE) : Component.text("-", KatzeColor.GRAY)),
                ranked("katze.cosmetics.stat.best_run", Component.text(store.best(id), KatzeColor.WHITE), store.rank(LeaderboardMetric.BEST_RUN, id)),
                Component.text(" "),
                ranked("katze.cosmetics.stat.points", Component.text(store.points(id), KatzeColor.GOLD), store.rank(LeaderboardMetric.CURRENT_POINTS, id)),
                ranked("katze.cosmetics.stat.lifetime_points", Component.text(store.earned(id), KatzeColor.GOLD), store.rank(LeaderboardMetric.LIFETIME_POINTS, id)),
                ranked("katze.cosmetics.stat.cases_opened", Component.text(store.casesOpened(id), KatzeColor.WHITE), store.rank(LeaderboardMetric.CASES_OPENED, id))
        };
    }

    private static @NotNull OptionalInt currentRun(@NotNull UUID player) {
        Lobby lobby = LobbyManager.getActiveLobby();
        if (lobby == null) return OptionalInt.empty();

        return lobby.getFeatures().stream()
                .filter(JumpAndRun.class::isInstance)
                .map(JumpAndRun.class::cast)
                .flatMapToInt(jumpAndRun -> jumpAndRun.score(player).stream())
                .findFirst();
    }

    private static @NotNull Component stat(@NotNull String label, @NotNull Component value) {
        return Component.translatable(label).color(KatzeColor.LIGHT_GRAY).append(Component.text(" ")).append(value);
    }

    private static @NotNull Component ranked(@NotNull String label, @NotNull Component value, @NotNull OptionalInt position) {
        Component line = stat(label, value);
        return position.isEmpty() ? line : line.append(Component.text(" ")).append(Component.translatable("katze.cosmetics.rank_suffix",
                TextArg.number("rank", position.getAsInt())).color(Leaderboard.rankColor(position.getAsInt() - 1)));
    }
}
