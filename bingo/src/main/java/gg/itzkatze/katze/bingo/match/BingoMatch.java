package gg.itzkatze.katze.bingo.match;

import gg.itzkatze.katze.bingo.board.BingoBoard;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class BingoMatch {
    public enum Result { ACTIVE, LINE, DEADLOCK, TIMEOUT, FORFEIT, ADMIN_DRAW, ABORTED }

    public UUID id = UUID.randomUUID();
    public long seed;
    public long startedAt = System.currentTimeMillis();
    public long endedAt;
    public List<BingoCompetitor> competitors;
    public List<String> objectives;
    public int[] owners = new int[25];
    public UUID[] contributors = new UUID[25];
    public int[] claimedAt = new int[25];
    public int[] grace = {120, 120};
    public int remaining;
    public int elapsed;
    public int revision;
    public int tiePriority;
    public int winner = -1;
    public Result result = Result.ACTIVE;
    public List<Integer> winningLine = List.of();
    public boolean archived;

    public BingoMatch(List<BingoCompetitor> competitors, List<String> objectives, int seconds, long seed) {
        if (competitors.size() != 2 || objectives.size() != 25) throw new IllegalArgumentException("Invalid Bingo match");
        this.competitors = List.copyOf(competitors);
        this.objectives = List.copyOf(objectives);
        this.remaining = seconds;
        this.seed = seed;
        tiePriority = (int) (seed & 1);
        Arrays.fill(owners, -1);
        Arrays.fill(claimedAt, -1);
    }

    public int side(UUID player) {
        for (int side = 0; side < 2; side++) if (competitors.get(side).contains(player)) return side;
        return -1;
    }

    public boolean claim(int cell, int side, UUID player) {
        if (result != Result.ACTIVE || cell < 0 || cell >= 25 || side < 0 || side > 1 || owners[cell] != -1
                || !competitors.get(side).contains(player)) return false;
        owners[cell] = side;
        contributors[cell] = player;
        claimedAt[cell] = elapsed;
        revision++;
        winningLine = BingoBoard.winningLine(owners, side);
        if (!winningLine.isEmpty()) finish(Result.LINE, side);
        else if (!BingoBoard.viable(owners, 0) && !BingoBoard.viable(owners, 1)) finish(Result.DEADLOCK, -1);
        return true;
    }

    public int tiles(int side) { return (int) Arrays.stream(owners).filter(owner -> owner == side).count(); }

    public void timeout() {
        if (result != Result.ACTIVE) return;
        remaining = 0;
        int first = tiles(0), second = tiles(1);
        finish(Result.TIMEOUT, first == second ? -1 : first > second ? 0 : 1);
    }

    public void finish(Result reason, int side) {
        if (result != Result.ACTIVE) return;
        result = reason;
        winner = side;
        endedAt = System.currentTimeMillis();
        revision++;
    }

    public void tick(boolean firstOnline, boolean secondOnline) {
        if (result != Result.ACTIVE) return;
        if (!firstOnline && !secondOnline) return;
        boolean[] online = {firstOnline, secondOnline};
        for (int side = 0; side < 2; side++) {
            if (online[side]) grace[side] = 120;
            else if (--grace[side] <= 0) { finish(Result.FORFEIT, 1 - side); return; }
        }
        if (!firstOnline || !secondOnline) return;
        elapsed++;
        if (remaining > 0 && --remaining == 0) timeout();
    }
}
