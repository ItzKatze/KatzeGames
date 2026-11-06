package gg.itzkatze.katze.lobby.jumpandrun;

import gg.itzkatze.katze.lobby.cosmetics.CosmeticCatalog;
import gg.itzkatze.katze.lobby.cosmetics.CosmeticStore;
import gg.itzkatze.katze.player.data.PlayerDataStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JumpRunTest {

    @TempDir Path directory;

    @BeforeEach
    void setUp() {
        PlayerDataStore data = new PlayerDataStore(directory.resolve("players.json"), (task, ticks) -> {});
        CosmeticStore.install(new CosmeticStore(data, CosmeticCatalog.of(List.of())));
    }

    @AfterEach
    void tearDown() {
        CosmeticStore.install(null);
    }

    @Test
    void slimeAndFinalBlockFormOneScoringJump() {
        JumpRun run = run();

        for (HopDifficulty difficulty : new HopDifficulty[]{HopDifficulty.SLIME_EASY, HopDifficulty.SLIME_HARD}) {
            int before = run.getScore();
            Cell slime = run.getCurrent().add(4, -3, 0);
            Cell target = slime.add(5, 2, 0);
            run.setNext(new HopPlanner.Hop(target, Set.of(slime, target), difficulty, slime), true);

            assertEquals(before, run.getScore());
            assertEquals(slime, run.getSlime());
            assertEquals(target, run.getNext());
            assertNotEquals(slime, run.getNext());
            assertTrue(run.land());
            assertEquals(before + 1, run.getScore());
            assertEquals(target, run.getCurrent());
            assertNull(run.getSlime());
            assertNull(run.getNext());
            assertFalse(run.isNextEvent());
        }
    }

    @Test
    void slimeJumpsAreDueEveryTenJumps() {
        JumpRun run = run();
        for (int jump = 1; jump <= 40; jump++) {
            assertEquals(jump % 10 == 0, run.isSlimeDue());
            complete(run, jump % 10 == 0 ? HopDifficulty.SLIME_EASY : HopDifficulty.NORMAL);
            assertEquals(jump, run.getScore());
        }
    }

    @Test
    void blockedSlimeJumpsStayDueUntilOneCanBePlaced() {
        JumpRun run = run();
        for (int jump = 1; jump <= 11; jump++) complete(run, HopDifficulty.NORMAL);
        assertTrue(run.isSlimeDue());
        complete(run, HopDifficulty.SLIME_HARD);
        for (int jump = 1; jump < 10; jump++) {
            assertFalse(run.isSlimeDue());
            complete(run, HopDifficulty.NORMAL);
        }
        assertTrue(run.isSlimeDue());
    }

    private void complete(JumpRun run, HopDifficulty difficulty) {
        Cell target = run.getCurrent().add(8, -1, 0);
        Cell slime = difficulty.isSlime() ? run.getCurrent().add(4, -2, 0) : null;
        run.setNext(new HopPlanner.Hop(target, Set.of(), difficulty, slime), false);
        run.land();
    }

    private JumpRun run() {
        UUID id = UUID.randomUUID();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getUniqueId")) return id;
                    throw new UnsupportedOperationException(method.getName());
                });
        Cell start = new Cell(0, 20, 0);
        Cell first = new Cell(3, 20, 0);
        RunLook look = new RunLook(Material.WHITE_CONCRETE, null, Component.empty());
        return new JumpRun(player, look, start, first);
    }
}
