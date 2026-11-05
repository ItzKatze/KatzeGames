package gg.itzkatze.katze.game.seed;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeedSearchAreaTest {
    @Test
    void diameterOfTenThousandMeansRadiusOfFiveThousand() {
        SeedSearchArea area = new SeedSearchArea(0, 0, 5000);
        assertTrue(area.contains(5000, 0));
        assertTrue(area.contains(-5000, 0));
        assertTrue(area.contains(3000, 4000));
        assertFalse(area.contains(5001, 0));
        assertFalse(area.contains(5000, 5000));
    }

    @Test
    void offsetsTheCircleAroundTheBattleCenter() {
        SeedSearchArea area = new SeedSearchArea(100.5, 0.5, 5000);
        assertTrue(area.contains(5100.5, 0.5));
        assertFalse(area.contains(-5000, 0.5));
        assertFalse(area.contains(Double.NaN, 0));
    }

    @Test
    void rejectsInvalidCentersAndRadii() {
        assertThrows(IllegalArgumentException.class, () -> new SeedSearchArea(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new SeedSearchArea(Double.NaN, 0, 5000));
    }
}
