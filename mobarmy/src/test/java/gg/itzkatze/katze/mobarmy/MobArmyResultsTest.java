package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.results.BattleResultsStore;
import gg.itzkatze.katze.results.SavedBattle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyResultsTest {
    @Test void archivesParticipantsTimesTeamsAndCollectedMobsAfterRestart(@TempDir Path directory) throws Exception {
        var match = new MobArmyMatch();
        match.startedAt = 1000;
        match.endedAt = 121000;
        var team = side("team-4", 1250, false);
        team.members.put(UUID.randomUUID(), "Teammate");
        var mob = new MobArmyMatch.CapturedMob();
        mob.type = "IRON_GOLEM";
        team.mobs.add(mob);
        var solo = side(UUID.randomUUID().toString(), 1800, false);
        var forfeited = side(UUID.randomUUID().toString(), 0, true);
        match.sides = List.of(solo, forfeited, team);
        var battle = MobArmyResults.capture(match);
        var store = new BattleResultsStore(directory);
        store.save(battle);
        var restored = new BattleResultsStore(directory).forPlayer(team.members.keySet().iterator().next()).getFirst();
        assertEquals(battle, restored);
        assertEquals(SavedBattle.Game.MOB_ARMY, restored.game());
        assertEquals(120, restored.durationSeconds());
        assertEquals(4, restored.players().size());
        assertEquals(4, restored.teams().getFirst().id());
        assertEquals(1, restored.teams().getFirst().points());
        assertEquals(1, restored.teams().getFirst().objectives().size());
        assertEquals(1250, restored.players().getFirst().statistics().get(1).value());
        assertEquals(1, restored.players().getLast().statistics().getLast().value());
        assertTrue(store.forPlayer(UUID.randomUUID()).isEmpty());
    }

    private MobArmyMatch.Side side(String id, long ticks, boolean forfeited) {
        var side = new MobArmyMatch.Side();
        side.id = id;
        side.members.put(UUID.randomUUID(), id);
        side.elapsedTicks = ticks;
        side.finished = true;
        side.forfeited = forfeited;
        return side;
    }
}
