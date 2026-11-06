package gg.itzkatze.katze.lobby.jumpandrun;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class SlimePlanner {

    private static final Map<Cell, Cell> EASY = mirrored(2, new int[][]{
            {4, 0, 8, 0}, {4, 1, 8, 2}, {4, 2, 8, 4}, {3, 3, 6, 6}});
    private static final Map<Cell, Cell> HARD = mirrored(3, new int[][]{
            {4, 0, 9, 0}, {4, 1, 9, 2}, {4, 2, 8, 5}, {3, 3, 7, 7}});

    private SlimePlanner() {
    }

    static @NotNull List<HopReach.Group> approaches(@NotNull HopDifficulty difficulty) {
        return List.of(new HopReach.Group(1, List.copyOf(offsets(difficulty).keySet())));
    }

    static @Nullable HopPlanner.Hop next(@NotNull Cell from, @NotNull HopPlanner.Space space, @NotNull CellBox playArea,
                                         @NotNull Set<Cell> exempt, @NotNull Random random, @NotNull HopDifficulty difficulty,
                                         @Nullable Cell destination) {
        for (Cell offset : HopSteering.order(approaches(difficulty), from, destination, random)) {
            Cell slime = from.add(offset.x(), offset.y(), offset.z());
            Cell landing = offsets(difficulty).get(offset);
            Cell block = from.add(landing.x(), landing.y(), landing.z());
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

    private static @NotNull Map<Cell, Cell> offsets(@NotNull HopDifficulty difficulty) {
        return difficulty == HopDifficulty.SLIME_HARD ? HARD : EASY;
    }

    private static @NotNull Map<Cell, Cell> mirrored(int drop, int[][] shapes) {
        Map<Cell, Cell> offsets = new LinkedHashMap<>();
        for (int[] shape : shapes) {
            for (int signX = -1; signX <= 1; signX += 2) {
                for (int signZ = -1; signZ <= 1; signZ += 2) {
                    offsets.put(new Cell(signX * shape[0], -drop, signZ * shape[1]),
                            new Cell(signX * shape[2], -1, signZ * shape[3]));
                    offsets.put(new Cell(signX * shape[1], -drop, signZ * shape[0]),
                            new Cell(signX * shape[3], -1, signZ * shape[2]));
                }
            }
        }
        return Collections.unmodifiableMap(offsets);
    }
}
