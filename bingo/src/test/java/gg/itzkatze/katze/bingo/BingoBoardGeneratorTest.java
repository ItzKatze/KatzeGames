package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.bingo.board.BingoBoard;
import gg.itzkatze.katze.bingo.board.BingoBoardGenerator;
import gg.itzkatze.katze.bingo.board.BingoObjective;
import gg.itzkatze.katze.bingo.board.BingoObjectiveCatalog;
import gg.itzkatze.katze.bingo.board.Difficulty;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class BingoBoardGeneratorTest {
    @Test void everyWinningLineIsBalancedAcrossManySeeds() {
        BingoObjectiveCatalog catalog = new BingoObjectiveCatalog();
        BingoBoardGenerator generator = new BingoBoardGenerator();
        Set<List<String>> boards = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            List<String> ids = generator.generate(catalog.all(), seed);
            List<BingoObjective> board = ids.stream().map(catalog::get).toList();
            assertEquals(25, new HashSet<>(ids).size());
            assertTrue(generator.balanced(board));
            for (List<Integer> line : BingoBoard.LINES) {
                assertEquals(2, line.stream().filter(cell -> board.get(cell).difficulty() == Difficulty.EASY).count());
                assertEquals(2, line.stream().filter(cell -> board.get(cell).difficulty() == Difficulty.MEDIUM).count());
                assertEquals(1, line.stream().filter(cell -> board.get(cell).difficulty() == Difficulty.HARD).count());
            }
            boards.add(ids);
        }
        assertEquals(200, boards.size());
    }

    @Test void deterministicSeedAndInsufficientPool() {
        BingoObjectiveCatalog catalog = new BingoObjectiveCatalog();
        BingoBoardGenerator generator = new BingoBoardGenerator();
        assertEquals(generator.generate(catalog.all(), 123), generator.generate(catalog.all(), 123));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(catalog.all().stream()
                .filter(objective -> objective.difficulty() != Difficulty.HARD).toList(), 1));
    }

    @Test void generatesBalancedBoardsWithoutNetherObjectives() {
        BingoObjectiveCatalog catalog = new BingoObjectiveCatalog();
        var pool = catalog.all().stream().filter(objective -> objective.dimension().equals("NORMAL")).toList();
        for (int seed = 0; seed < 30; seed++) assertEquals(25, new BingoBoardGenerator().generate(pool, seed).size());
    }
}
