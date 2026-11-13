package gg.itzkatze.katze.challenges;

import gg.itzkatze.katze.commands.framework.KatzeCommand;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeManagerTest {
    private final Set<Challenge> previousChallenges = new HashSet<>();
    private final AtomicInteger messages = new AtomicInteger();
    private Field serverField;
    private Field startedField;
    private Server previousServer;
    private boolean previousStarted;

    @BeforeEach void isolateState() throws Exception {
        previousChallenges.addAll(ChallengeManager.getActiveChallenges());
        previousStarted = ChallengeManager.isGameStarted();
        ChallengeManager.getActiveChallenges().clear();
        startedField = ChallengeManager.class.getDeclaredField("gameStarted");
        startedField.setAccessible(true);
        startedField.setBoolean(null, false);
        serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        previousServer = Bukkit.getServer();
        Server server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("sendMessage")) { messages.incrementAndGet(); return null; }
                    throw new AssertionError("Unexpected server call: " + method.getName());
                });
        serverField.set(null, server);
    }

    @AfterEach void restoreState() throws Exception {
        ChallengeManager.getActiveChallenges().clear();
        ChallengeManager.getActiveChallenges().addAll(previousChallenges);
        startedField.setBoolean(null, previousStarted);
        serverField.set(null, previousServer);
    }

    @Test void mobArmyCannotBeEnabledDuringAnExistingModifierRound() {
        for (ChallengeType modifier : List.of(ChallengeType.RANDOMIZER, ChallengeType.ITEM_RAIN)) {
            ChallengeManager.getActiveChallenges().clear();
            Challenge active = new ProbeChallenge(modifier, false);
            ChallengeManager.getActiveChallenges().add(active);
            ChallengeManager.markGameStarted();

            assertDoesNotThrow(() -> ChallengeManager.loadChallenge(new ProbeChallenge(ChallengeType.MOB_ARMY, true)));
            assertEquals(Set.of(active), ChallengeManager.getActiveChallenges());
            assertTrue(ChallengeManager.isGameStarted());
        }
        assertEquals(2, messages.get());
    }

    @Test void mobArmyCanStillReachInitializationInTheLobby() {
        Challenge active = new ProbeChallenge(ChallengeType.RANDOMIZER, false);
        ChallengeManager.getActiveChallenges().add(active);

        assertThrows(InitializationReached.class, () -> ChallengeManager.loadChallenge(new ProbeChallenge(ChallengeType.MOB_ARMY, true)));
        assertEquals(Set.of(active), ChallengeManager.getActiveChallenges());
        assertFalse(ChallengeManager.isGameStarted());
        assertEquals(0, messages.get());
    }

    private record ProbeChallenge(ChallengeType type, boolean locked) implements Challenge {
        @Override public ChallengeType getType() { return type; }
        @Override public boolean locksChallengeSelection() { return locked; }
        @Override public void onEnable() { throw new InitializationReached(); }
        @Override public List<KatzeCommand> getCommands() { return List.of(); }
        @Override public List<Listener> getListeners() { return List.of(); }
        @Override public List<Runnable> getTasks() { return List.of(); }
    }

    private static final class InitializationReached extends RuntimeException {}
}
