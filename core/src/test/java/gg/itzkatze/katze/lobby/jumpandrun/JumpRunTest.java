package gg.itzkatze.katze.lobby.jumpandrun;

import gg.itzkatze.katze.lobby.cosmetics.CosmeticCatalog;
import gg.itzkatze.katze.lobby.cosmetics.CosmeticCategory;
import gg.itzkatze.katze.lobby.cosmetics.CosmeticStore;
import gg.itzkatze.katze.lobby.cosmetics.BlockHighlightCosmetic;
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
        assertEquals(run.getNext(), run.target());

        for (HopDifficulty difficulty : new HopDifficulty[]{HopDifficulty.SLIME_EASY, HopDifficulty.SLIME_HARD}) {
            int before = run.getScore();
            Cell slime = run.getCurrent().add(4, -3, 0);
            Cell target = slime.add(5, 2, 0);
            run.setNext(new HopPlanner.Hop(target, Set.of(slime, target), difficulty, slime), true);

            assertEquals(before, run.getScore());
            assertEquals(slime, run.getSlime());
            assertEquals(slime, run.target());
            assertEquals(target, run.getNext());
            assertNotEquals(slime, run.getNext());
            assertFalse(run.touchSlime(run.getCurrent()));
            assertEquals(slime, run.target());
            assertTrue(run.touchSlime(slime));
            assertEquals(target, run.target());
            assertEquals(before, run.getScore());
            assertFalse(run.touchSlime(slime));
            assertEquals(target, run.target());
            assertTrue(run.land());
            assertEquals(before + 1, run.getScore());
            assertEquals(target, run.getCurrent());
            assertNull(run.getSlime());
            assertNull(run.getNext());
            assertNull(run.target());
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

    @Test
    void highlightsUseTheEquippedColorAndSupportRandomPerJump() {
        PlayerDataStore data = new PlayerDataStore(directory.resolve("highlights.json"), (task, ticks) -> {});
        CosmeticStore store = new CosmeticStore(data, CosmeticCatalog.standard());
        CosmeticStore.install(store);
        JumpRun defaultRun = run();
        assertNull(defaultRun.getHighlightColor());
        Player player = defaultRun.getPlayer();
        store.winCosmetic(player, BlockHighlightCosmetic.RED, 0);
        assertTrue(store.equip(player, BlockHighlightCosmetic.RED));
        JumpRun coloredRun = new JumpRun(player, defaultRun.getLook(), defaultRun.getCurrent(), defaultRun.getNext());
        assertEquals(BlockHighlightCosmetic.RED.color(), coloredRun.getHighlightColor());

        store.equipRandom(player, CosmeticCategory.HIGHLIGHT);
        defaultRun.refreshCosmetics();
        assertNull(defaultRun.getHighlightColor());
        store.equipRandom(player, CosmeticCategory.HIGHLIGHT);
        defaultRun.refreshCosmetics();
        assertEquals(BlockHighlightCosmetic.RED.color(), defaultRun.getHighlightColor());

        Cell slime = coloredRun.getCurrent().add(4, -2, 0);
        Cell target = slime.add(4, 1, 0);
        coloredRun.setNext(new HopPlanner.Hop(target, Set.of(slime, target), HopDifficulty.SLIME_EASY, slime), false);
        assertTrue(coloredRun.touchSlime(slime));
        assertEquals(BlockHighlightCosmetic.RED.color(), coloredRun.getHighlightColor());

        store.unequip(player, CosmeticCategory.HIGHLIGHT);
        JumpRun clearedRun = new JumpRun(player, coloredRun.getLook(), coloredRun.getCurrent(), target);
        assertNull(clearedRun.getHighlightColor());
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
                    if (method.getName().equals("getName")) return "Runner";
                    throw new UnsupportedOperationException(method.getName());
                });
        Cell start = new Cell(0, 20, 0);
        Cell first = new Cell(3, 20, 0);
        RunLook look = new RunLook(Material.WHITE_CONCRETE, null, Component.empty());
        return new JumpRun(player, look, start, first);
    }
}
