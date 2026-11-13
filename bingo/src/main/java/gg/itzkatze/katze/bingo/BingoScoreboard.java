package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.bingo.board.BingoObjective;
import gg.itzkatze.katze.bingo.match.BingoMatch;
import gg.itzkatze.katze.bingo.match.BingoMatchManager;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.scoreboard.ScoreboardContent;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.StringUtility;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.ShadowColor;

import java.util.ArrayList;
import java.util.List;

public final class BingoScoreboard implements ScoreboardContent {
    private final BingoMatchManager matches;
    public BingoScoreboard(BingoMatchManager matches) { this.matches = matches; }
    @Override public Component getTitle() { return Component.text("BINGO"); }
    @Override public int getPriority() { return 200; }
    @Override public Component[] getLines(KatzePlayer player) {
        if (!matches.packReady(player.getUuid()) || matches.hidden(player.getUuid())) return new Component[0];
        BingoMatch match = matches.viewed(player.getUuid());
        if (match == null) return new Component[0];
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text(BingoHudLayout.competitor("■", match.competitors.get(0).name(), match.tiles(0)), KatzeColor.LIGHT_BLUE));
        lines.add(Component.text(BingoHudLayout.competitor("■", match.competitors.get(1).name(), match.tiles(1)), KatzeColor.ORANGE));
        Component columns = Component.text("\uE002").font(Key.key("katzegames", "bingo/layout"));
        for (int column = 0; column < 5; column++) {
            columns = columns.append(Component.text(String.valueOf((char) ('A' + column))).font(Key.key("katzegames", "bingo/layout")));
            if (column < 4) columns = columns.append(Component.text("\uE001").font(Key.key("katzegames", "bingo/layout")));
        }
        lines.add(columns.color(KatzeColor.WHITE));
        Component graphics = Component.empty();
        List<BingoObjective> catalog = matches.catalog().all();
        for (int row = 0; row < 5; row++) {
            StringBuilder text = new StringBuilder();
            text.append('\uE012').append(row + 1).append('\uE011');
            for (int column = 0; column < 5; column++) {
                int cell = row * 5 + column;
                int index = catalog.indexOf(matches.catalog().get(match.objectives.get(cell)));
                text.append(BingoHudLayout.cell(index, match.owners[cell], match.winningLine.contains(cell), catalog.get(index).category()));
            }
            if (row != 4) text.append('\uE000');
            graphics = graphics.append(Component.text(text.toString()).font(Key.key("katzegames", "bingo/row_" + row))
                    .color(KatzeColor.WHITE).shadowColor(ShadowColor.none()).decoration(TextDecoration.BOLD, false));
        }
        lines.add(graphics);
        for (int i = 0; i < BingoHudLayout.BOARD_SPACER_LINES; i++) lines.add(Component.text(" "));
        return lines.toArray(Component[]::new);
    }

    public Component status(BingoMatch match) {
        if (match.result != BingoMatch.Result.ACTIVE) return match.winner < 0 ? Component.translatable("katze.bingo.draw")
                : Component.translatable("katze.bingo.winner", TextArg.text("winner", match.competitors.get(match.winner).name()));
        if (!KatzeGames.getTimer().isRunning()) return Component.translatable("katze.game.timer.paused");
        if (!matches.online(match, 0) || !matches.online(match, 1)) {
            int side = !matches.online(match, 0) ? 0 : 1;
            return Component.translatable("katze.bingo.reconnect", TextArg.number("seconds", match.grace[side]));
        }
        return match.remaining < 0 ? Component.translatable("katze.bingo.duration.unlimited")
                : Component.text(StringUtility.formatTime(match.remaining), KatzeColor.WHITE);
    }
}
