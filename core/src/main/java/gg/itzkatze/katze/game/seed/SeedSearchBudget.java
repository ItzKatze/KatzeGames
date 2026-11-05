package gg.itzkatze.katze.game.seed;

import java.time.Duration;
import java.util.function.LongSupplier;

public final class SeedSearchBudget {
    private final int maxAttempts;
    private final LongSupplier clock;
    private final long started;
    private final long timeout;
    private int attempt = 1;

    public SeedSearchBudget(int maxAttempts, Duration timeout, LongSupplier clock) {
        if (maxAttempts < 1 || timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("Invalid seed search limits");
        this.maxAttempts = maxAttempts;
        this.clock = clock;
        this.timeout = timeout.toNanos();
        started = clock.getAsLong();
    }

    public int attempt() {
        return attempt;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public boolean expired() {
        return clock.getAsLong() - started >= timeout;
    }

    public boolean retry() {
        if (expired() || attempt >= maxAttempts) return false;
        attempt++;
        return true;
    }
}
