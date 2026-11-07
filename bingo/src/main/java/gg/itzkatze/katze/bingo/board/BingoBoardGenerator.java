package gg.itzkatze.katze.bingo.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Arrays;

public final class BingoBoardGenerator {
    public List<String> generate(List<BingoObjective> pool, long seed) {
        Random random = new Random(seed);
        Map<Difficulty, List<BingoObjective>> tiers = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            List<BingoObjective> candidates = new ArrayList<>(pool.stream().filter(objective -> objective.difficulty() == difficulty).toList());
            if (candidates.size() < (difficulty == Difficulty.HARD ? 5 : 10)) {
                throw new IllegalArgumentException("katze.bingo.error.pool");
            }
            tiers.put(difficulty, candidates);
        }
        for (int attempt = 0; attempt < 20000; attempt++) {
            tiers.replaceAll((difficulty, candidates) -> mixed(candidates, random));
            Map<Difficulty, Integer> used = new EnumMap<>(Difficulty.class);
            List<BingoObjective> board = new ArrayList<>();
            int rotation = random.nextInt(4);
            boolean reflected = random.nextBoolean();
            for (int cell = 0; cell < 25; cell++) {
                int row = cell / 5;
                int column = reflected ? 4 - cell % 5 : cell % 5;
                for (int i = 0; i < rotation; i++) {
                    int previous = row;
                    row = column;
                    column = 4 - previous;
                }
                int value = (row + 2 * column) % 5;
                Difficulty difficulty = value < 2 ? Difficulty.EASY : value < 4 ? Difficulty.MEDIUM : Difficulty.HARD;
                int index = used.getOrDefault(difficulty, 0);
                board.add(tiers.get(difficulty).get(index));
                used.put(difficulty, index + 1);
            }
            if (balanced(board)) return board.stream().map(BingoObjective::id).toList();
        }
        throw new IllegalArgumentException("katze.bingo.error.balance");
    }

    public boolean balanced(List<BingoObjective> board) {
        if (board.size() != 25 || board.stream().map(BingoObjective::id).distinct().count() != 25) return false;
        for (int first = 0; first < board.size(); first++) for (int second = first + 1; second < board.size(); second++) {
            BingoObjective left = board.get(first), right = board.get(second);
            if (left.category() == right.category() && Arrays.stream(left.target().split(",")).anyMatch(target -> Arrays.asList(right.target().split(",")).contains(target))) return false;
        }
        int categoryLimit = board.stream().map(BingoObjective::category).distinct().count() == 1 ? 5 : 3;
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (List<Integer> line : BingoBoard.LINES) {
            Map<Difficulty, Integer> counts = new EnumMap<>(Difficulty.class);
            Map<String, Integer> dependencies = new HashMap<>();
            Map<BingoObjective.Category, Integer> categories = new EnumMap<>(BingoObjective.Category.class);
            int effort = 0;
            for (int cell : line) {
                BingoObjective objective = board.get(cell);
                counts.merge(objective.difficulty(), 1, Integer::sum);
                if (!objective.dependency().isEmpty() && dependencies.merge(objective.dependency(), 1, Integer::sum) > 2) return false;
                if (categories.merge(objective.category(), 1, Integer::sum) > categoryLimit) return false;
                effort += objective.effort();
            }
            if (counts.getOrDefault(Difficulty.EASY, 0) != 2 || counts.getOrDefault(Difficulty.MEDIUM, 0) != 2
                    || counts.getOrDefault(Difficulty.HARD, 0) != 1) return false;
            min = Math.min(min, effort);
            max = Math.max(max, effort);
        }
        return max - min <= 6;
    }

    private List<BingoObjective> mixed(List<BingoObjective> candidates, Random random) {
        Map<BingoObjective.Category, List<BingoObjective>> categories = new EnumMap<>(BingoObjective.Category.class);
        for (BingoObjective objective : candidates) categories.computeIfAbsent(objective.category(), ignored -> new ArrayList<>()).add(objective);
        categories.values().forEach(values -> Collections.shuffle(values, random));
        List<BingoObjective> result = new ArrayList<>();
        for (int index = 0; result.size() < candidates.size(); index++) {
            List<BingoObjective.Category> order = new ArrayList<>(categories.keySet());
            Collections.shuffle(order, random);
            for (BingoObjective.Category category : order) if (index < categories.get(category).size()) result.add(categories.get(category).get(index));
        }
        return result;
    }
}
