package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.bingo.board.BingoBoard;
import gg.itzkatze.katze.bingo.match.BingoCompetitor;
import gg.itzkatze.katze.bingo.match.BingoMatch;
import gg.itzkatze.katze.util.JsonFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class BingoMatchTest {
    private final UUID first = UUID.randomUUID();
    private final UUID teammate = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    private BingoMatch match() {
        return new BingoMatch(List.of(new BingoCompetitor("a", "A", 1, List.of(new BingoCompetitor.Member(first, "First"),
                        new BingoCompetitor.Member(teammate, "Teammate"))),
                new BingoCompetitor("b", "B", null, List.of(new BingoCompetitor.Member(second, "Second")))),
                IntStream.range(0, 25).mapToObj(Integer::toString).toList(), 30, 12);
    }

    @Test void teammateClaimsAreExclusiveAndIsolatedFromOtherMatches() {
        BingoMatch match = match();
        assertTrue(match.claim(12, 0, teammate));
        assertFalse(match.claim(12, 1, second));
        assertFalse(match.claim(13, 0, second));
        assertFalse(match.claim(13, 0, UUID.randomUUID()));
        assertEquals(1, match.tiles(0));
        assertEquals(teammate, match.contributors[12]);
        assertTrue(match().claim(12, 1, second));
    }

    @Test void allTwelveLinesWinAndFreezeClaims() {
        for (var line : BingoBoard.LINES) {
            BingoMatch match = match();
            for (int cell : line) assertTrue(match.claim(cell, 0, first));
            assertEquals(BingoMatch.Result.LINE, match.result);
            assertEquals(0, match.winner);
            assertEquals(line, match.winningLine);
            assertFalse(match.claim((line.getFirst() + 1) % 25, 1, second));
        }
    }

    @Test void blockingEveryLineProducesDrawBeforeBoardIsFull() {
        BingoMatch match = match();
        String colors = "AABABBBABAAABBABBAABAABABA";
        for (int cell = 0; cell < 25 && match.result == BingoMatch.Result.ACTIVE; cell++) {
            int side = colors.charAt(cell) == 'A' ? 0 : 1;
            match.claim(cell, side, side == 0 ? first : second);
        }
        assertEquals(BingoMatch.Result.DEADLOCK, match.result);
        assertEquals(-1, match.winner);
    }

    @Test void graceFreezesTimeAndBothAbsentFreezesGrace() {
        BingoMatch match = match();
        match.tick(false, false);
        assertEquals(120, match.grace[0]);
        for (int i = 0; i < 119; i++) match.tick(false, true);
        assertEquals(BingoMatch.Result.ACTIVE, match.result);
        assertEquals(30, match.remaining);
        match.tick(true, true);
        assertEquals(120, match.grace[0]);
        assertEquals(29, match.remaining);
        for (int i = 0; i < 120; i++) match.tick(false, true);
        assertEquals(BingoMatch.Result.FORFEIT, match.result);
        assertEquals(1, match.winner);
    }

    @Test void timeoutAndUnlimited() {
        BingoMatch match = match();
        match.claim(0, 0, first);
        match.claim(1, 0, first);
        match.claim(2, 1, second);
        for (int i = 0; i < 30; i++) match.tick(true, true);
        assertEquals(BingoMatch.Result.TIMEOUT, match.result);
        assertEquals(0, match.winner);
        match = match();
        match.remaining = -1;
        for (int i = 0; i < 100; i++) match.tick(true, true);
        assertEquals(BingoMatch.Result.ACTIVE, match.result);
        assertEquals(-1, match.remaining);
    }

    @Test void eitherSideCanWinOnTimeWithMoreObjectivesAndTiedCountsDraw() {
        for (int leader = 0; leader < 2; leader++) {
            BingoMatch match = match();
            UUID winner = leader == 0 ? first : second;
            UUID loser = leader == 0 ? second : first;
            match.claim(0, leader, winner);
            match.claim(1, leader, winner);
            match.claim(2, 1 - leader, loser);
            match.remaining = 1;
            match.tick(true, true);
            assertEquals(BingoMatch.Result.TIMEOUT, match.result);
            assertEquals(leader, match.winner);
            assertEquals(0, match.remaining);
            assertFalse(match.claim(3, 1 - leader, loser));
        }
        for (boolean claimed : new boolean[]{false, true}) {
            BingoMatch match = match();
            if (claimed) { match.claim(0, 0, first); match.claim(1, 1, second); }
            match.timeout();
            assertEquals(BingoMatch.Result.TIMEOUT, match.result);
            assertEquals(-1, match.winner);
        }
    }

    @Test void restartPreservesExactOwnershipRosterAndClock(@TempDir Path directory) throws Exception {
        BingoMatch match = match();
        match.tick(true, true);
        match.claim(6, 0, teammate);
        match.tick(false, true);
        Path path = directory.resolve("match.json");
        JsonFiles.write(path, match);
        BingoMatch restored = JsonFiles.read(path, BingoMatch.class);
        assertNotNull(restored);
        assertEquals(match.id, restored.id);
        assertEquals(match.competitors, restored.competitors);
        assertEquals(match.objectives, restored.objectives);
        assertArrayEquals(match.owners, restored.owners);
        assertArrayEquals(match.contributors, restored.contributors);
        assertArrayEquals(match.grace, restored.grace);
        assertEquals(match.remaining, restored.remaining);
        assertEquals(match.revision, restored.revision);
        assertFalse(restored.claim(6, 1, second));
    }
}
