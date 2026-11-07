package gg.itzkatze.katze.bingo.board;

import java.util.ArrayList;
import java.util.List;

public final class BingoBoard {
    public static final List<List<Integer>> LINES = lines();

    private BingoBoard() {}

    private static List<List<Integer>> lines() {
        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            List<Integer> row = new ArrayList<>();
            List<Integer> column = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                row.add(i * 5 + j);
                column.add(j * 5 + i);
            }
            result.add(List.copyOf(row));
            result.add(List.copyOf(column));
        }
        result.add(List.of(0, 6, 12, 18, 24));
        result.add(List.of(4, 8, 12, 16, 20));
        return List.copyOf(result);
    }

    public static String coordinate(int index) {
        return "" + (char) ('A' + index % 5) + (index / 5 + 1);
    }

    public static List<Integer> winningLine(int[] owners, int side) {
        return LINES.stream().filter(line -> line.stream().allMatch(cell -> owners[cell] == side)).findFirst().orElse(List.of());
    }

    public static boolean viable(int[] owners, int side) {
        return LINES.stream().anyMatch(line -> line.stream().noneMatch(cell -> owners[cell] == 1 - side));
    }
}
