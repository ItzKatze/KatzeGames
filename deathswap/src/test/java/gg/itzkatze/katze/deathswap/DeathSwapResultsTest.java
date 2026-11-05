package gg.itzkatze.katze.deathswap;

import gg.itzkatze.katze.results.ResultRoster;
import gg.itzkatze.katze.results.SavedBattle;
import gg.itzkatze.katze.util.JsonFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeathSwapResultsTest {
    @TempDir
    Path directory;

    @Test
    void retainsEliminatedPlayersAndTheirSurvivalStatsWithoutGrantingSpectatorsAccess() {
        UUID winner = UUID.randomUUID();
        UUID runnerUp = UUID.randomUUID();
        UUID firstOut = UUID.randomUUID();
        UUID spectator = UUID.randomUUID();
        ResultRoster roster = new ResultRoster(UUID.randomUUID(), 1000, List.of(
                new ResultRoster.Member(winner, "Winner"), new ResultRoster.Member(runnerUp, "Runner up"),
                new ResultRoster.Member(firstOut, "First out")));
        SavedBattle result = DeathSwapResults.snapshot(roster, List.of(
                new DeathSwapResults.Elimination(firstOut, 3, 65, 1),
                new DeathSwapResults.Elimination(runnerUp, 2, 125, 2)), winner, 125, 2, 2000);

        assertEquals(SavedBattle.Game.DEATH_SWAP, result.game());
        assertTrue(result.includes(firstOut));
        assertFalse(result.includes(spectator));
        assertEquals(List.of(3, 2, 1), result.players().stream().map(SavedBattle.Player::points).toList());
        assertEquals(List.of(new SavedBattle.Statistic("place", 3, -1), new SavedBattle.Statistic("survived", 65, -1),
                new SavedBattle.Statistic("swaps", 1, -1)), result.players().getLast().statistics());
        assertEquals(new SavedBattle.Statistic("place", 1, -1), result.players().getFirst().statistics().getFirst());
    }

    @Test
    void originalRosterAndEliminationHistoryCanBeRestoredBeforeTheWinnerIsDecided() throws Exception {
        UUID winner = UUID.randomUUID();
        UUID eliminated = UUID.randomUUID();
        ResultRoster roster = new ResultRoster(UUID.randomUUID(), 1000, List.of(
                new ResultRoster.Member(winner, "Winner"), new ResultRoster.Member(eliminated, "Eliminated")));
        Path rosterFile = directory.resolve("roster.json");
        Path eliminationFile = directory.resolve("elimination.json");
        var elimination = new DeathSwapResults.Elimination(eliminated, 2, 90, 1);
        JsonFiles.write(rosterFile, roster);
        JsonFiles.write(eliminationFile, elimination);
        SavedBattle result = DeathSwapResults.snapshot(JsonFiles.read(rosterFile, ResultRoster.class),
                List.of(JsonFiles.read(eliminationFile, DeathSwapResults.Elimination.class)), winner, 90, 1, 2000);

        assertEquals(roster.id(), result.id());
        assertTrue(result.includes(eliminated));
        assertEquals(90, result.durationSeconds());
    }
}
