package gg.itzkatze.katze.results;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BattleResultsStoreTest {
    @TempDir
    Path directory;

    private final UUID participant = UUID.randomUUID();
    private final UUID opponent = UUID.randomUUID();
    private final UUID spectator = UUID.randomUUID();

    @Test
    void resultsAndObjectiveHistoriesSurviveAStoreRestart() throws Exception {
        SavedBattle battle = battle(1000);
        new BattleResultsStore(directory).save(battle);

        BattleResultsStore restarted = new BattleResultsStore(directory);
        assertEquals(List.of(battle), restarted.forPlayer(participant));
        assertEquals(battle, restarted.findForPlayer(battle.id(), participant).orElseThrow());
        assertEquals(battle, restarted.findForPlayer(battle.id(), opponent).orElseThrow());
        assertTrue(restarted.forPlayer(participant).getFirst().players().getFirst().objectives().getFirst().skipped());
    }

    @Test
    void spectatorsAndOtherPlayersCannotListOrOpenARoundEvenWithItsId() throws Exception {
        BattleResultsStore store = new BattleResultsStore(directory);
        SavedBattle battle = battle(1000);
        store.save(battle);

        assertTrue(store.forPlayer(spectator).isEmpty());
        assertTrue(store.findForPlayer(battle.id(), spectator).isEmpty());
        assertTrue(store.findForPlayer(battle.id(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void listsOnlyTheViewersRoundsNewestFirst() throws Exception {
        BattleResultsStore store = new BattleResultsStore(directory);
        SavedBattle older = battle(1000);
        SavedBattle newer = battle(2000);
        store.save(newer);
        store.save(older);
        store.save(new SavedBattle(UUID.randomUUID(), 0, 3000, 90,
                List.of(new SavedBattle.Player(spectator, "Spectator in another round", null, 0, List.of())), List.of()));

        assertEquals(List.of(newer, older), store.forPlayer(participant));
    }

    @Test
    void savingTheSameRoundTwiceDoesNotCreateDuplicateHistory() throws Exception {
        BattleResultsStore store = new BattleResultsStore(directory);
        SavedBattle battle = battle(1000);
        store.save(battle);
        store.save(battle);

        assertEquals(1, store.forPlayer(participant).size());
    }

    @Test
    void emptyHistoryAndMissingRoundsAreHandled() throws Exception {
        BattleResultsStore store = new BattleResultsStore(directory.resolve("not-created"));
        assertTrue(store.forPlayer(participant).isEmpty());
        assertTrue(store.findForPlayer(UUID.randomUUID(), participant).isEmpty());
    }

    @Test
    void oneDamagedRoundDoesNotHideTheRestOfTheHistory() throws Exception {
        BattleResultsStore store = new BattleResultsStore(directory);
        SavedBattle battle = battle(1000);
        store.save(battle);
        Files.writeString(directory.resolve(UUID.randomUUID() + ".json"), "{invalid");

        assertEquals(List.of(battle), store.forPlayer(participant));
    }

    @Test
    void capturedResultsDoNotChangeWhenTheSourceCollectionsAreCleared() {
        List<SavedBattle.Objective> objectives = new ArrayList<>();
        objectives.add(new SavedBattle.Objective("\"Stone\"", "\"Item\"", "STONE", "Player", 30, true, false));
        SavedBattle.Player player = new SavedBattle.Player(participant, "Player", 4, 1, objectives);
        List<SavedBattle.Player> players = new ArrayList<>(List.of(player));
        SavedBattle battle = new SavedBattle(UUID.randomUUID(), 0, 1000, 90, players, List.of());
        objectives.clear();
        players.clear();

        assertTrue(battle.includes(participant));
        assertEquals(1, battle.players().getFirst().objectives().size());
        assertThrows(UnsupportedOperationException.class, () -> battle.players().clear());
    }

    private SavedBattle battle(long endedAt) {
        SavedBattle.Objective skipped = new SavedBattle.Objective("\"Stone\"", "\"Item\"", "STONE", "Player", 45, true, true);
        SavedBattle.Objective unfinished = new SavedBattle.Objective("\"Diamond\"", "\"Item\"", "DIAMOND", "", -1, false, false);
        return new SavedBattle(UUID.randomUUID(), endedAt - 90, endedAt, 90,
                List.of(new SavedBattle.Player(participant, "Player", 4, 3, List.of(skipped, unfinished)),
                        new SavedBattle.Player(opponent, "Opponent", null, 2, List.of())),
                List.of(new SavedBattle.Team(4, 3, List.of(skipped, unfinished))));
    }

    @Test
    void readsOldForceBattleFilesWithoutGameOrStatisticsAndKeepsAccessChecks() throws Exception {
        Path legacy = directory.resolve("forcebattle-results");
        Files.createDirectories(legacy);
        UUID round = UUID.randomUUID();
        Files.writeString(legacy.resolve(round + ".json"), """
                {"id":"%s","startedAt":1000,"endedAt":2000,"durationSeconds":90,
                 "players":[{"id":"%s","name":"Player","teamId":null,"points":2,"objectives":[]}],"teams":[]}
                """.formatted(round, participant));
        BattleResultsStore store = new BattleResultsStore(directory.resolve("results"), legacy);

        SavedBattle restored = store.findForPlayer(round, participant).orElseThrow();
        assertEquals(SavedBattle.Game.FORCE_BATTLE, restored.game());
        assertTrue(restored.players().getFirst().statistics().isEmpty());
        assertEquals(1, store.forPlayer(participant).size());
        assertTrue(store.findForPlayer(round, spectator).isEmpty());
        store.save(restored);
        assertEquals(1, store.forPlayer(participant).size());
    }

    @Test
    void persistsMixedGameHistoryAndStatisticsAfterRestart() throws Exception {
        BattleResultsStore store = new BattleResultsStore(directory);
        SavedBattle forceBattle = battle(1000);
        var player = new SavedBattle.Player(participant, "Player", null, 1, List.of(),
                List.of(new SavedBattle.Statistic("place", 1, -1), new SavedBattle.Statistic("swaps", 3, -1)));
        SavedBattle deathSwap = new SavedBattle(UUID.randomUUID(), 1000, 2000, 180,
                List.of(player), List.of(), SavedBattle.Game.DEATH_SWAP);
        SavedBattle allItems = new SavedBattle(UUID.randomUUID(), 2000, 3000, 900,
                List.of(new SavedBattle.Player(participant, "Player", 0, 20, List.of(),
                        List.of(new SavedBattle.Statistic("item", 20, 1000)))), List.of(), SavedBattle.Game.ALL_ITEMS);
        store.save(forceBattle);
        store.save(deathSwap);
        store.save(allItems);

        BattleResultsStore restarted = new BattleResultsStore(directory);
        assertEquals(List.of(allItems, deathSwap, forceBattle), restarted.forPlayer(participant));
        assertTrue(restarted.forPlayer(spectator).isEmpty());
        assertEquals(deathSwap, restarted.findForPlayer(deathSwap.id(), participant).orElseThrow());
    }
}
