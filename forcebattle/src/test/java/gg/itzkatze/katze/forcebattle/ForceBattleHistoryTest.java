package gg.itzkatze.katze.forcebattle;

import gg.itzkatze.katze.util.JsonFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ForceBattleHistoryTest {
    @TempDir
    Path directory;

    @Test
    void restoredRoundKeepsItsOriginalIdentityAndRoster() throws Exception {
        var member = new ForceBattleHistory.Member(UUID.randomUUID(), "Player", 7);
        var original = new ForceBattleHistory.Round(UUID.randomUUID(), 1234, List.of(member));
        Path file = directory.resolve("round.json");
        JsonFiles.write(file, original);
        var restored = JsonFiles.read(file, ForceBattleHistory.Round.class);

        assertEquals(original, restored);
        ForceBattleHistory.restore(restored);
        try {
            assertEquals(original, ForceBattleHistory.current());
        } finally {
            ForceBattleHistory.restore(null);
        }
    }

    @Test
    void rosterIsIndependentOfLaterLobbyChanges() {
        var member = new ForceBattleHistory.Member(UUID.randomUUID(), "Player", null);
        List<ForceBattleHistory.Member> lobbyPlayers = new ArrayList<>(List.of(member));
        var round = new ForceBattleHistory.Round(UUID.randomUUID(), 1234, lobbyPlayers);
        lobbyPlayers.clear();

        assertEquals(List.of(member), round.members());
        assertThrows(UnsupportedOperationException.class, () -> round.members().clear());
    }
}
