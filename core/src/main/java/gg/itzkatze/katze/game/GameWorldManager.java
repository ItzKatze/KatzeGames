package gg.itzkatze.katze.game;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.util.JsonFiles;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameRules;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class GameWorldManager {

    private static final NamespacedKey LIMBO = key("limbo");
    private static final List<NamespacedKey> GAME_WORLDS = List.of(key("game"), key("game_nether"), key("game_the_end"));
    private static final List<World.Environment> ENVIRONMENTS = List.of(World.Environment.NORMAL, World.Environment.NETHER, World.Environment.THE_END);
    private static State state;
    @Getter
    private static boolean resetting;
    private static boolean ready;
    private static boolean preparingSeed;

    private record State(long seed, boolean pendingReset) {}

    private GameWorldManager() {}

    private static NamespacedKey key(String name) {
        return new NamespacedKey("katzegames", name);
    }

    private static Path stateFile() {
        return KatzeGames.get().getDataDirectory().toPath().resolve("data/game-worlds.json");
    }

    public static void initialize() {
        try {
            if (state != null) return;
            if (ChallengeManager.isGameStarted()) {
                ready = true;
                return;
            }
            saveState(new State(ThreadLocalRandom.current().nextLong(), false));
            createWorlds();
            ready = true;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize game worlds", e);
        }
    }

    public static void loadSavedWorlds() {
        try {
            state = JsonFiles.read(stateFile(), State.class);
            if (state == null) return;
            if (state.pendingReset()) {
                deleteWorlds(unloadWorlds());
            }
            createWorlds();
            saveState(new State(state.seed(), false));
            ready = true;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load saved game worlds", e);
        }
    }

    public static World getWorld() {
        return getWorld(World.Environment.NORMAL);
    }

    public static World getWorld(World.Environment environment) {
        int index = ENVIRONMENTS.indexOf(environment);
        if (index < 0) return null;
        return Bukkit.getWorld(state == null ? switch (environment) {
            case NORMAL -> NamespacedKey.minecraft("overworld");
            case NETHER -> NamespacedKey.minecraft("the_nether");
            case THE_END -> NamespacedKey.minecraft("the_end");
            default -> throw new IllegalArgumentException("Unsupported environment: " + environment);
        } : GAME_WORLDS.get(index));
    }

    public static List<World> getWorlds() {
        return ENVIRONMENTS.stream().map(GameWorldManager::getWorld).filter(Objects::nonNull).toList();
    }

    public static boolean isReady() { return ready && !resetting && !preparingSeed; }

    public static void setPreparingSeed(boolean preparing) { preparingSeed = preparing; }

    public static boolean isLimbo(World world) { return world.getKey().equals(LIMBO); }

    public static World getLimbo() {
        World world = Bukkit.getWorld(LIMBO);
        if (world == null) {
            world = Objects.requireNonNull(WorldCreator.ofKey(LIMBO).generator(new VoidGenerator()).generateStructures(false).createWorld(), "Failed to create limbo");
            world.setGameRule(GameRules.SPAWN_MOBS, false);
            world.setGameRule(GameRules.LOCATOR_BAR, false);
            for (Chunk chunk : world.getLoadedChunks()) LimboBiomes.apply(chunk);
        }
        return world;
    }

    public static void reset(Runnable onSuccess, Runnable onFailure) {
        if (resetting) return;
        resetting = true;
        ready = false;
        Bukkit.getScheduler().runTask(KatzeGames.get(), () -> {
            try {
                long previousSeed = getWorld() == null ? 0 : getWorld().getSeed();
                long seed;
                do {
                    seed = ThreadLocalRandom.current().nextLong();
                } while (seed == previousSeed);
                saveState(new State(seed, true));
                List<GameWorldDirectory> paths = unloadWorlds();
                Bukkit.getScheduler().runTaskAsynchronously(KatzeGames.get(), () -> {
                    try {
                        deleteWorlds(paths);
                        Bukkit.getScheduler().runTask(KatzeGames.get(), () -> recreate(0, onSuccess, onFailure));
                    } catch (Exception e) {
                        Bukkit.getScheduler().runTask(KatzeGames.get(), () -> fail(e, onFailure));
                    }
                });
            } catch (Exception e) {
                fail(e, onFailure);
            }
        });
    }

    private static void recreate(int index, Runnable onSuccess, Runnable onFailure) {
        try {
            createWorld(index);
            if (index + 1 < GAME_WORLDS.size()) {
                Bukkit.getScheduler().runTask(KatzeGames.get(), () -> recreate(index + 1, onSuccess, onFailure));
                return;
            }
            saveState(new State(state.seed(), false));
            resetting = false;
            ready = true;
            onSuccess.run();
        } catch (Exception e) {
            fail(e, onFailure);
        }
    }

    private static void fail(Exception exception, Runnable onFailure) {
        resetting = false;
        ready = false;
        KatzeGames.get().getLogger().log(Level.SEVERE, "Failed to reset game worlds; players remain in limbo. Retry /reset.", exception);
        onFailure.run();
    }

    private static void createWorlds() {
        for (int i = 0; i < GAME_WORLDS.size(); i++) createWorld(i);
    }

    private static void createWorld(int index) {
        Objects.requireNonNull(WorldCreator.ofKey(GAME_WORLDS.get(index)).environment(ENVIRONMENTS.get(index)).seed(state.seed()).createWorld(), "Failed to create game world");
    }

    private static void saveState(State updated) throws IOException {
        JsonFiles.write(stateFile(), updated);
        state = updated;
    }

    private static List<GameWorldDirectory> unloadWorlds() throws IOException {
        List<GameWorldDirectory> paths = new ArrayList<>();
        for (NamespacedKey key : GAME_WORLDS) {
            GameWorldDirectory directory = new GameWorldDirectory(Bukkit.getServer(), key);
            World world = Bukkit.getWorld(key);
            if (world != null) {
                directory.validateWorldPath(world.getWorldPath());
                if (!Bukkit.unloadWorld(world, false)) throw new IOException("Could not unload " + key);
            }
            paths.add(directory);
        }
        return paths;
    }

    private static void deleteWorlds(List<GameWorldDirectory> paths) throws IOException {
        for (GameWorldDirectory directory : paths) directory.delete();
    }
}
