package gg.itzkatze.katze.mobarmy;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyMatchTest {
    @Test void competingTeammateEditsCannotSpendTheSameKillTwice() {
        var match = match(2);
        var side = match.sides.getFirst();
        side.mobs.add(mob("drowned-trident"));
        assertTrue(match.allocate(side, "DROWNED", 0, true));
        assertFalse(match.allocate(side, "DROWNED", 1, true));
        assertFalse(match.allocate(side, "DROWNED", 2, false));
        assertEquals(1, side.mobs.size());
        assertTrue(match.allocate(side, "DROWNED", 0, false));
        assertTrue(match.allocate(side, "DROWNED", 2, true));
        assertEquals(2, side.mobs.getFirst().wave);
    }

    @Test void successfulEditsClearReadinessButStaleClicksDoNot() {
        var match = match(2);
        var side = match.sides.getFirst();
        side.mobs.add(mob("zombie"));
        assertFalse(match.toggleReady(side));
        assertTrue(side.ready);
        assertFalse(match.allocate(side, "missing", 0, true));
        assertTrue(side.ready);
        assertTrue(match.allocate(side, "DROWNED", 0, true));
        assertFalse(side.ready);
        assertFalse(match.toggleReady(match.sides.get(1)));
        assertTrue(match.toggleReady(side));
        match.phase = MobArmyMatch.Phase.COMBAT;
        assertFalse(match.allocate(side, "DROWNED", 0, false));
        assertFalse(match.toggleReady(side));
    }

    @Test void mobTypesShareOnePoolWhileIndividualEquipmentAndAttributesArePreserved() {
        var match = match(2);
        var side = match.sides.getFirst();
        var trident = mob("drowned-trident");
        trident.data = "trident-snapshot";
        var unarmed = mob("drowned-unarmed");
        unarmed.data = "unarmed-snapshot";
        side.mobs.addAll(List.of(trident, unarmed));

        assertEquals(1, side.mobsByType().size());
        assertEquals(List.of(trident, unarmed), side.mobsByType().get("DROWNED"));
        assertTrue(match.allocate(side, "DROWNED", 0, true));
        assertTrue(match.allocate(side, "DROWNED", 1, true));
        assertFalse(match.allocate(side, "DROWNED", 2, true));
        assertTrue(match.allocate(side, "DROWNED", 0, false));
        assertTrue(match.allocate(side, "DROWNED", 2, true));
        assertEquals(2, trident.wave);
        assertEquals(1, unarmed.wave);
        assertEquals("trident-snapshot", trident.data);
        assertEquals("unarmed-snapshot", unarmed.data);
    }

    @Test void resultLinksAndTeamLookupsUseMemberNames() {
        var match = match(2);
        var side = match.sides.getFirst();
        side.id = UUID.randomUUID().toString();
        side.members.clear();
        side.members.put(UUID.randomUUID(), "ItzKatze");
        side.members.put(UUID.randomUUID(), "SatinTrousers");

        assertEquals("/waves details ItzKatze", side.detailsCommand());
        assertSame(side, match.findSide("itzkatze"));
        assertSame(side, match.findSide("SATINTROUSERS"));
        assertNull(match.findSide("MissingPlayer"));
    }

    @Test void arenaRebuildingDoesNotCountAsCombatOrConsumeTheSpawnQueue() {
        var match = match(2);
        match.phase = MobArmyMatch.Phase.COMBAT;
        var side = match.sides.getFirst();
        side.preparing = true;
        side.queue.add(mob("drowned"));
        side.roofTicks.put(side.members.keySet().iterator().next(), 600L);
        for (int i = 0; i < 100; i++) {
            match.tickClock(side, true);
            assertNull(match.nextSpawn(side));
        }
        assertEquals(0, side.elapsedTicks);
        assertEquals(1, side.queue.size());
        assertEquals(600L, side.roofTicks.values().iterator().next());
        assertThrows(IllegalStateException.class, () -> match.advance(side));
    }

    @Test void threeTeamsFightSixWavesSkippingTheirOwnSets() {
        var match = match(3);
        match.phase = MobArmyMatch.Phase.COMBAT;
        var side = match.sides.get(1);
        assertEquals(List.of(match.sides.get(0), match.sides.get(2)), match.opponents(side));
        for (int wave = 0; wave < 6; wave++) {
            match.prepareWave(side);
            assertTrue(side.queue.isEmpty());
            assertEquals(wave / 3, side.opponent);
            assertEquals(wave % 3, side.wave);
            var progress = match.advance(side);
            assertEquals(wave == 5 ? MobArmyMatch.Progress.FINISHED : wave == 2 ? MobArmyMatch.Progress.OPPONENT : MobArmyMatch.Progress.WAVE, progress);
        }
        assertTrue(side.finished);
        assertThrows(IllegalStateException.class, () -> match.advance(side));
    }

    @Test void spawnQueueHasNoCapAndReleasesFivePerSecond() {
        var match = match(2);
        var side = match.sides.getFirst();
        var opponent = match.sides.get(1);
        for (int i = 0; i < 800; i++) { var mob = mob("rabbit"); mob.wave = 0; opponent.mobs.add(mob); }
        match.phase = MobArmyMatch.Phase.COMBAT;
        match.prepareWave(side);
        int spawned = 0;
        for (int tick = 0; tick < 20; tick++) if (match.nextSpawn(side) != null) spawned++;
        assertEquals(5, spawned);
        assertEquals(795, side.queue.size());
        assertThrows(IllegalStateException.class, () -> match.advance(side));
        side.queue.clear();
        side.live.put(UUID.randomUUID(), new MobArmyMatch.LiveMob("data", 0, 0, 0, 0, 0));
        assertThrows(IllegalStateException.class, () -> match.advance(side));
    }

    @Test void penaltiesPauseAndResetOnlyWhenAnOpponentSetEnds() {
        assertArrayEquals(new int[]{30, 60, 90, 120, 150, 180}, java.util.stream.IntStream.rangeClosed(1, 6).map(MobArmyMatch::penaltySeconds).toArray());
        var match = match(3);
        var side = match.sides.getFirst();
        UUID player = side.members.keySet().iterator().next();
        side.roofTicks.put(player, 600L);
        side.deaths.put(player, 1);
        match.phase = MobArmyMatch.Phase.COMBAT;
        match.tickClock(side, false);
        assertEquals(600, side.roofTicks.get(player));
        assertEquals(0, side.elapsedTicks);
        match.tickClock(side, true);
        assertEquals(599, side.roofTicks.get(player));
        assertEquals(1, side.elapsedTicks);
        match.advance(side);
        assertFalse(side.deaths.isEmpty());
        match.advance(side);
        match.advance(side);
        assertTrue(side.deaths.isEmpty());
        assertTrue(side.roofTicks.isEmpty());
        side.finished = true;
        match.tickClock(side, true);
        assertEquals(1, side.elapsedTicks);
    }

    @Test void lowestTimeWinsWithSharedVictoryAndDnfExcluded() {
        var match = match(3);
        for (var side : match.sides) { side.finished = true; side.elapsedTicks = 400; }
        match.sides.getFirst().forfeited = true;
        match.sides.getFirst().elapsedTicks = 0;
        assertEquals(List.of(match.sides.get(1), match.sides.get(2)), match.winners());
        match.sides.get(2).elapsedTicks++;
        assertEquals(List.of(match.sides.get(1)), match.winners());
    }

    @Test void snapshotRoundTripPreservesQueuesPenaltiesAndVariantAssignments() {
        var match = match(2);
        var side = match.sides.getFirst();
        var mob = mob("drowned-trident"); mob.wave = 2;
        side.mobs.add(mob);
        side.queue.add(mob);
        side.ready = true;
        side.roofTicks.put(side.members.keySet().iterator().next(), 123L);
        side.live.put(UUID.randomUUID(), new MobArmyMatch.LiveMob("full-entity", 10, 80, 12, 90, 20));
        var gson = new Gson();
        var restored = gson.fromJson(gson.toJson(match), MobArmyMatch.class);
        restored.validate();
        assertEquals(match.id, restored.id);
        assertEquals(2, restored.sides.getFirst().mobs.getFirst().wave);
        assertEquals("full-entity", restored.sides.getFirst().live.values().iterator().next().data());
        assertEquals(side.roofTicks, restored.sides.getFirst().roofTicks);
        assertEquals(side.queue.getFirst().variant, restored.sides.getFirst().queue.getFirst().variant);
        assertTrue(restored.sides.getFirst().ready);
    }

    @Test void invalidRecoveryCannotSilentlyStartFresh() {
        var match = match(2);
        match.sides.get(1).members.putAll(match.sides.getFirst().members);
        assertThrows(IllegalStateException.class, match::validate);
        match = match(2);
        match.phase = MobArmyMatch.Phase.COMBAT;
        assertThrows(IllegalStateException.class, match::validate);
    }

    private static MobArmyMatch match(int count) {
        var match = new MobArmyMatch();
        match.phase = MobArmyMatch.Phase.CONFIGURATION;
        for (int i = 0; i < count; i++) {
            var side = new MobArmyMatch.Side();
            side.id = "team-" + i;
            UUID uuid = UUID.randomUUID();
            side.members.put(uuid, "Player" + i);
            var loadout = new PlayerLoadout();
            loadout.inventorySize = 43;
            loadout.inventory = new ArrayList<>(Collections.nCopies(loadout.inventorySize, null));
            loadout.cursor = new ArrayList<>(Collections.nCopies(1, null));
            loadout.effects = List.of();
            loadout.world = "minecraft:overworld";
            loadout.health = 20;
            side.players.put(uuid, loadout);
            match.sides.add(side);
        }
        return match;
    }
    private static MobArmyMatch.CapturedMob mob(String variant) {
        var mob = new MobArmyMatch.CapturedMob(); mob.type = "DROWNED"; mob.variant = variant; mob.data = "entity-data"; return mob;
    }
}
