package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.bingo.board.BingoObjective.Category;

public final class BingoHudLayout {
    public static final int CELL_SIZE = 20;
    public static final int ROW_PITCH = CELL_SIZE + 1;
    public static final int BOARD_HEIGHT = 4 * ROW_PITCH + CELL_SIZE;
    public static final int ROW_LABEL_WIDTH = 8;
    public static final int ROW_ADVANCE = ROW_LABEL_WIDTH + 1 + 5 * (CELL_SIZE + 1);
    public static final int ASCENT = 7;
    public static final int BOARD_SPACER_LINES = 11;
    public static final int ICON_SIZE = 16;
    public static final int BADGE_SIZE = 8;
    public static final int BADGE_OFFSET = CELL_SIZE - BADGE_SIZE - 1;

    private BingoHudLayout() {}

    public static String competitor(String marker, String name, int tiles) {
        return marker + " " + name + ": " + tiles;
    }

    public static String cell(int icon, int owner, boolean winning, Category category) {
        int state = owner < 0 ? 0 : owner + (winning ? 3 : 1);
        return new StringBuilder().appendCodePoint(0xE020 + state).append('\uE010').appendCodePoint(0xF0000 + icon).appendCodePoint(0xF4000 + icon).appendCodePoint(0xE030 + category.ordinal()).append('\uE013').toString();
    }
}
