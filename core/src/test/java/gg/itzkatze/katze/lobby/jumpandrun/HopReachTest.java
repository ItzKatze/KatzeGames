package gg.itzkatze.katze.lobby.jumpandrun;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HopReachTest {

    @Test
    void beginnersReceiveNormalHopsBetweenSlimeJumps() {
        for (int best : new int[]{-1, 0, 1, 75}) {
            assertEquals(HopDifficulty.NORMAL, roll(best, 0));
            assertEquals(HopDifficulty.NORMAL, roll(best, 0.119));
            assertEquals(HopDifficulty.NORMAL, roll(best, 0.121));
            assertEquals(HopDifficulty.NORMAL, roll(best, 0.99));
        }
    }

    @Test
    void hardHopsBecomeMoreFrequentAsBestImproves() {
        assertEquals(HopDifficulty.HARD, roll(76, 0));
        assertEquals(HopDifficulty.NORMAL, roll(76, 0.01));
        assertEquals(HopDifficulty.HARD, roll(120, 0.149));
        assertEquals(HopDifficulty.NORMAL, roll(120, 0.151));
        assertEquals(HopDifficulty.HARD, roll(150, 0.249));
        assertEquals(HopDifficulty.NORMAL, roll(150, 0.251));
    }

    @Test
    void extremeHopsGraduallyAppearAboveOneHundredAndFifty() {
        assertEquals(HopDifficulty.HARD, roll(150, 0));
        assertEquals(HopDifficulty.EXTREME, roll(151, 0));
        assertEquals(HopDifficulty.EXTREME, roll(225, 0.089));
        assertEquals(HopDifficulty.HARD, roll(225, 0.091));
        assertEquals(HopDifficulty.HARD, roll(225, 0.339));
        assertEquals(HopDifficulty.NORMAL, roll(225, 0.341));
    }

    @Test
    void experiencedPlayersKeepTheOriginalMaximumChances() {
        for (int best : new int[]{300, 600, Integer.MAX_VALUE}) {
            assertEquals(HopDifficulty.EXTREME, roll(best, 0.179));
            assertEquals(HopDifficulty.HARD, roll(best, 0.181));
            assertEquals(HopDifficulty.HARD, roll(best, 0.429));
            assertEquals(HopDifficulty.NORMAL, roll(best, 0.431));
            assertEquals(HopDifficulty.NORMAL, roll(best, 0.551));
        }
    }

    @Test
    void difficultHopsAlwaysHaveANormalHopAfterwards() {
        for (HopDifficulty previous : new HopDifficulty[]{HopDifficulty.HARD, HopDifficulty.EXTREME,
                HopDifficulty.SLIME_EASY, HopDifficulty.SLIME_HARD}) {
            assertEquals(HopDifficulty.NORMAL, HopReach.roll(previous, 300, false, fixedRoll(0)));
        }
    }

    @Test
    void hardSlimeHopsUseThePlayersExperience() {
        for (HopDifficulty previous : HopDifficulty.values()) {
            assertEquals(HopDifficulty.SLIME_EASY, HopReach.roll(previous, 75, true, fixedRoll(0)));
            assertEquals(HopDifficulty.SLIME_HARD, HopReach.roll(previous, 300, true, fixedRoll(0.249)));
            assertEquals(HopDifficulty.SLIME_EASY, HopReach.roll(previous, 300, true, fixedRoll(0.251)));
        }
    }

    private HopDifficulty roll(int best, double value) {
        return HopReach.roll(HopDifficulty.NORMAL, best, false, fixedRoll(value));
    }

    private Random fixedRoll(double value) {
        return new Random() {
            @Override
            public double nextDouble() {
                return value;
            }
        };
    }
}
