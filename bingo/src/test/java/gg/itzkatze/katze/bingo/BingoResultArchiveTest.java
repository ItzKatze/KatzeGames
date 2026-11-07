package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.bingo.match.BingoResultArchive;
import gg.itzkatze.katze.results.SavedBattle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BingoResultArchiveTest {
    @Test void failedResultWritesSurviveRestartAndRetryExactlyOnce(@TempDir Path directory) {
        Path path = directory.resolve("pending.json");
        SavedBattle battle = new SavedBattle(UUID.randomUUID(), 1, 2, 1, List.of(), List.of(), SavedBattle.Game.BINGO,
                new SavedBattle.Bingo(List.of("A", "B"), -1, "timeout", List.of(), List.of()));
        BingoResultArchive first = new BingoResultArchive(path, result -> false);
        first.submit(battle);
        List<SavedBattle> saved = new ArrayList<>();
        BingoResultArchive restored = new BingoResultArchive(path, result -> { saved.add(result); return true; });
        restored.retry();
        restored.retry();
        assertEquals(List.of(battle), saved);
        new BingoResultArchive(path, result -> { fail("Already archived results must not be retried"); return false; }).retry();
    }
}
