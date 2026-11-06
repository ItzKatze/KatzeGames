package gg.itzkatze.katze.lobby.jumpandrun;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SlimePlannerTest {

    private static final Cell FROM = new Cell(0, 20, 0);
    private static final CellBox AREA = new CellBox(-20, 0, -20, 20, 40, 20);

    @Test
    void bothVariantsReserveTheWholeRouteAndHaveAWayOn() {
        for (HopDifficulty difficulty : new HopDifficulty[]{HopDifficulty.SLIME_EASY, HopDifficulty.SLIME_HARD}) {
            Set<Cell> directions = new HashSet<>();
            List<Cell> offsets = SlimePlanner.approaches(difficulty).getFirst().offsets();
            for (int direction = 0; direction < offsets.size(); direction++) {
                HopPlanner.Hop hop = SlimePlanner.next(FROM, new TestSpace(), AREA, Set.of(),
                        prefer(direction), difficulty, null);
                assertNotNull(hop);
                assertEquals(difficulty, hop.difficulty());
                assertNotNull(hop.slime());
                Cell offset = offsets.get(direction);
                assertEquals(FROM.add(offset.x(), offset.y(), offset.z()), hop.slime());
                assertEquals(FROM.y() - 1, hop.block().y());
                assertTrue(hop.reserved().contains(hop.slime()));
                assertTrue(hop.reserved().contains(hop.block()));
                assertTrue(hop.reserved().stream().allMatch(AREA::contains));
                Set<Cell> approach = HopPlanner.corridor(FROM, hop.slime(), HopDifficulty.HARD);
                approach.remove(FROM);
                assertTrue(hop.reserved().containsAll(approach));
                assertTrue(hop.reserved().containsAll(HopPlanner.corridor(hop.slime(), hop.block(), HopDifficulty.HARD)));
                assertTrue(HopPlanner.hasWayOn(hop.block(), FROM, Set.of(), new TestSpace(), AREA));
                directions.add(offset);
                assertReachable(hop);
                assertCannotSkipSlime(hop);
            }
            assertEquals(24, directions.size());
        }
    }

    @Test
    void obstaclesAndOtherPlayersReservationsRejectEveryPartOfARoute() {
        for (HopDifficulty difficulty : new HopDifficulty[]{HopDifficulty.SLIME_EASY, HopDifficulty.SLIME_HARD}) {
            HopPlanner.Hop hop = plan(new TestSpace(), AREA, difficulty, 4);
            assertNotNull(hop);
            for (Cell cell : hop.reserved()) {
                TestSpace space = new TestSpace();
                space.obstacles.add(cell);
                HopPlanner.Hop alternative = plan(space, AREA, difficulty, 4);
                assertTrue(alternative == null || !alternative.reserved().contains(cell));

                space.obstacles.clear();
                space.reserved.add(cell);
                alternative = plan(space, AREA, difficulty, 4);
                assertTrue(alternative == null || !alternative.reserved().contains(cell));
            }
        }
    }

    @Test
    void slimeDropsNeverLeaveThePlayArea() {
        CellBox shallow = new CellBox(-20, FROM.y() - 1, -20, 20, 40, 20);
        for (HopDifficulty difficulty : new HopDifficulty[]{HopDifficulty.SLIME_EASY, HopDifficulty.SLIME_HARD}) {
            assertNull(plan(new TestSpace(), shallow, difficulty, 0));
            HopPlanner.Hop fallback = HopPlanner.next(FROM, new TestSpace(), shallow, Set.of(), new Random(0), difficulty, null);
            assertNotNull(fallback);
            assertEquals(HopDifficulty.NORMAL, fallback.difficulty());
            assertNull(fallback.slime());
        }
    }

    private HopPlanner.Hop plan(TestSpace space, CellBox area, HopDifficulty difficulty, int seed) {
        return SlimePlanner.next(FROM, space, area, Set.of(), new Random(seed * 7919L), difficulty, null);
    }

    private Random prefer(int direction) {
        return new Random() {
            private int index;

            @Override
            public double nextDouble() {
                return index++ == direction ? 0 : 0.5;
            }
        };
    }

    private void assertReachable(HopPlanner.Hop hop) {
        int drop = FROM.y() - hop.slime().y();
        boolean reachable = false;
        for (double takeoff = -0.3; takeoff <= 0.3; takeoff += 0.1) {
            for (double speed = 0.12; speed <= 0.42; speed += 0.02) {
                for (int sprintTick = 0; sprintTick <= 30; sprintTick++) {
                    if (lands(hop.slime(), hop.block(), drop, takeoff, speed, sprintTick)) reachable = true;
                }
            }
        }
        assertTrue(reachable, () -> "Unreachable slime route: " + hop);
    }

    private void assertCannotSkipSlime(HopPlanner.Hop hop) {
        double x = 1.1;
        double vx = 0.5;
        double y = 0;
        double vy = 0.42;
        while (y > -1) {
            vx += 0.026;
            x += vx;
            y += vy;
            vx *= 0.91;
            vy = (vy - 0.08) * 0.98;
        }
        double distance = Math.hypot(hop.block().x() - FROM.x(), hop.block().z() - FROM.z());
        double directionX = (hop.block().x() - FROM.x()) / distance;
        double directionZ = (hop.block().z() - FROM.z()) / distance;
        assertTrue(Math.abs((distance - x) * directionX) > 0.8 || Math.abs((distance - x) * directionZ) > 0.8,
                "A sprint jump must fall short of the final block");
    }

    private boolean lands(Cell slime, Cell target, int drop, double x, double vx, int sprintTick) {
        double distance = Math.hypot(target.x() - FROM.x(), target.z() - FROM.z());
        double directionX = (target.x() - FROM.x()) / distance;
        double directionZ = (target.z() - FROM.z()) / distance;
        double y = 0;
        double vy = 0.42;
        boolean bounced = false;
        for (int tick = 0; tick < 60; tick++) {
            vx += tick >= sprintTick ? 0.026 : 0.02;
            double nextX = x + vx;
            double nextY = y + vy;
            if (!bounced && vy < 0 && nextY <= -drop) {
                if (Math.abs(nextX * directionX - (slime.x() - FROM.x())) > 0.65
                        || Math.abs(nextX * directionZ - (slime.z() - FROM.z())) > 0.65) return false;
                nextY = -drop;
                vy = -vy;
                bounced = true;
            } else if (bounced && vy < 0 && y >= -1 && nextY <= -1) {
                return Math.abs(nextX * directionX - (target.x() - FROM.x())) < 0.6
                        && Math.abs(nextX * directionZ - (target.z() - FROM.z())) < 0.6;
            }
            x = nextX;
            y = nextY;
            vx *= 0.91;
            vy = (vy - 0.08) * 0.98;
        }
        return false;
    }

    private static final class TestSpace implements HopPlanner.Space {
        private final Set<Cell> obstacles = new HashSet<>(Set.of(FROM));
        private final Set<Cell> reserved = new HashSet<>();

        @Override
        public boolean canPlace(Cell cell) {
            return isFree(cell);
        }

        @Override
        public boolean isFree(Cell cell) {
            return !obstacles.contains(cell);
        }

        @Override
        public boolean isLow(Cell cell) {
            return false;
        }

        @Override
        public boolean isReserved(Cell cell) {
            return reserved.contains(cell);
        }
    }
}
