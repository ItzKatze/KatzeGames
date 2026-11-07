package gg.itzkatze.katze.bingo;

public final class BingoHudLayout {
    public static final int CELL_SIZE = 18;
    public static final int ROW_PITCH = CELL_SIZE + 1;
    public static final int BOARD_HEIGHT = 4 * ROW_PITCH + CELL_SIZE;
    public static final int ROW_LABEL_WIDTH = 8;
    public static final int ROW_ADVANCE = ROW_LABEL_WIDTH + 1 + 5 * (CELL_SIZE + 1);
    public static final int ASCENT = 7;
    public static final int BOARD_SPACER_LINES = 10;
    public static final int ICON_SIZE = 16;

    private BingoHudLayout() {}

    public static String competitor(String marker, String name, int tiles) {
        return marker + " " + name + ": " + tiles;
    }

    public static String cell(int icon, int owner, boolean winning) {
        int state = owner < 0 ? 0 : owner + (winning ? 3 : 1);
        return new StringBuilder().appendCodePoint(0xE020 + state).append('\uE010').appendCodePoint(0xF0000 + icon).appendCodePoint(0xF4000 + icon).toString();
    }
}
