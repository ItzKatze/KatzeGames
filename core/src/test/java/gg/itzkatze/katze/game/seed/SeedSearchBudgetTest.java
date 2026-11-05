package gg.itzkatze.katze.game.seed;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class SeedSearchBudgetTest {
    @Test
    void retriesStopAtTheConfiguredCandidateLimit() {
        SeedSearchBudget budget = new SeedSearchBudget(3, Duration.ofSeconds(1), () -> 0);
        assertEquals(1, budget.attempt());
        assertTrue(budget.retry());
        assertEquals(2, budget.attempt());
        assertTrue(budget.retry());
        assertEquals(3, budget.attempt());
        assertFalse(budget.retry());
        assertEquals(3, budget.attempt());
    }

    @Test
    void timeoutCoversTheWholeSearchAndWorldReplacementTime() {
        AtomicLong clock = new AtomicLong(10);
        SeedSearchBudget budget = new SeedSearchBudget(100, Duration.ofNanos(30), clock::get);
        clock.set(20);
        assertTrue(budget.retry());
        clock.set(39);
        assertFalse(budget.expired());
        clock.set(40);
        assertTrue(budget.expired());
        assertFalse(budget.retry());
        assertEquals(2, budget.attempt());
    }

    @Test
    void oneAllowedCandidateCannotBeRetried() {
        assertFalse(new SeedSearchBudget(1, Duration.ofSeconds(1), () -> 0).retry());
    }
}
