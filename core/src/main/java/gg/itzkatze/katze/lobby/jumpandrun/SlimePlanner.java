package gg.itzkatze.katze.lobby.jumpandrun;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class SlimePlanner {

    private SlimePlanner() {
    }

    static @NotNull List<HopReach.Group> approaches(@NotNull HopDifficulty difficulty) {
        int distance = 4;
        int drop = difficulty == HopDifficulty.SLIME_HARD ? 3 : 2;
        return List.of(new HopReach.Group(1, List.of(
                new Cell(distance, -drop, 0), new Cell(-distance, -drop, 0),
                new Cell(0, -drop, distance), new Cell(0, -drop, -distance))));
    }

    static @Nullable HopPlanner.Hop next(@NotNull Cell from, @NotNull HopPlanner.Space space, @NotNull CellBox playArea,
                                         @NotNull Set<Cell> exempt, @NotNull Random random, @NotNull HopDifficulty difficulty,
                                         @Nullable Cell destination) {
        int bounceDistance = difficulty == HopDifficulty.SLIME_HARD ? 5 : 4;
        for (Cell offset : HopSteering.order(approaches(difficulty), from, destination, random)) {
            Cell slime = from.add(offset.x(), offset.y(), offset.z());
            Cell block = slime.add(Integer.signum(offset.x()) * bounceDistance, -offset.y() - 1,
                    Integer.signum(offset.z()) * bounceDistance);
            if (!playArea.contains(slime) || !playArea.contains(block) || !space.canPlace(slime) || !space.canPlace(block)) continue;

            Set<Cell> reserved = new HashSet<>(HopPlanner.corridor(from, slime, HopDifficulty.HARD));
            reserved.addAll(HopPlanner.corridor(slime, block, HopDifficulty.HARD));
            reserved.addAll(HopPlanner.landingZone(slime));
            reserved.addAll(HopPlanner.landingZone(block));
            reserved.add(slime);
            reserved.add(block);
            reserved.remove(from);
            boolean clear = true;
            for (Cell cell : reserved) {
                if (!playArea.contains(cell) || space.isReserved(cell)
                        || (!exempt.contains(cell) && !space.isFree(cell))) {
                    clear = false;
                    break;
                }
            }
            if (!clear || !HopPlanner.hasWayOn(block, from, exempt, space, playArea)) continue;

            return new HopPlanner.Hop(block, reserved, difficulty, slime);
        }
        return null;
    }
}
