package gg.itzkatze.katze.mobarmy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArenaLayoutTest {
    private final ArenaLayout layout = new ArenaLayout(87, 47, 97, 80, 43);

    @Test void customDimensionsDriveBoundsAndRoofHeight() {
        assertEquals(123, layout.roof());
        assertTrue(layout.contains(80.5, 94, 90.5, false));
        assertFalse(layout.contains(87, 94, 40, false));
        assertFalse(layout.contains(40, 124, 40, false));
        assertTrue(layout.contains(40, 124, 40, true));
        assertFalse(layout.contains(40, 94, 97, false));
    }

    @Test void touchingWallsOrTheRoofDoesNotRecoverMobsToAnotherPosition() {
        assertTrue(layout.containsMob(-0.3, 94, 40));
        assertTrue(layout.containsMob(87.3, 94, 40));
        assertTrue(layout.containsMob(40, 94, 97.3));
        assertTrue(layout.containsMob(40, 124, 40));
        assertFalse(layout.containsMob(40, 20, 40));
        assertFalse(layout.containsMob(140, 94, 40));
    }

    @Test void teamsUseDistinctWorldKeys() {
        assertNotEquals(MobArmyArena.worldKey(0), MobArmyArena.worldKey(1));
        assertEquals(0, MobArmyArena.worldIndex(MobArmyArena.worldKey(0), 3));
        assertEquals(1, MobArmyArena.worldIndex(MobArmyArena.worldKey(1), 3));
        assertEquals(-1, MobArmyArena.worldIndex(MobArmyArena.worldKey(3), 3));
    }
}
