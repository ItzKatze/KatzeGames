package gg.itzkatze.katze.game.seed;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.game.GameLifecycleManager;
import gg.itzkatze.katze.game.GameWorldManager;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.JsonFiles;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class SeedGenerator {
    private static SeedGenerator running;
    private static Certificate certificate;

    private final Runnable onSuccess;
    private final Consumer<String> onFailure;
    private final int radius;
    private final SeedSearchBudget budget;
    private final long tickBudgetNanos;
    private final List<SeedRequirement> requirements;
    private SeedValidator validator;
    private Certificate candidate;
    private WorldSeedSearch search;
    private long candidateStarted;
    private int ticks;
    private BukkitTask task;

    record WorldSignature(String id, long seed, World.Environment environment, double spawnX, double spawnZ) {}
    record Certificate(List<WorldSignature> worlds, String serverVersion, int radius, List<SeedRequirement> requirements) {
        boolean matches(Path reportFile) throws IOException {
            Report report = JsonFiles.read(reportFile, Report.class);
            return report != null && equals(report.certificate());
        }
    }
    record Witness(SeedFeature feature, SeedValidationResult.Position position) {}
    record Report(Certificate certificate, int attempts, List<Witness> witnesses) {}

    private SeedGenerator(List<SeedRequirement> requirements, int radius, Runnable onSuccess, Consumer<String> onFailure) {
        this.requirements = requirements;
        this.radius = radius;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
        int maxAttempts = Math.max(1, KatzeGames.get().getConfig().getInt("seed-generator.max-attempts", 100));
        long seconds = Math.clamp(KatzeGames.get().getConfig().getLong("seed-generator.timeout-seconds", 300), 1, 86400);
        budget = new SeedSearchBudget(maxAttempts, Duration.ofSeconds(seconds), System::nanoTime);
        tickBudgetNanos = Math.clamp(KatzeGames.get().getConfig().getLong("seed-generator.tick-budget-millis", 8), 1, 25) * 1_000_000L;
    }

    public static boolean isRunning() {
        return running != null;
    }

    public static void prepare(Runnable onSuccess, Consumer<String> onFailure) {
        if (running != null) {
            onFailure.accept("A battle seed is already being prepared.");
            return;
        }
        if (ChallengeManager.isGameStarted() || !GameWorldManager.isReady()) {
            onFailure.accept("Game worlds are not available for seed preparation.");
            return;
        }
        try {
            List<SeedRequirement> requirements = requirements();
            int diameter = KatzeGames.get().getConfig().getInt("seed-generator.diameter", 10000);
            if (diameter < 2 || diameter > 60000 || diameter % 2 != 0) {
                throw new IllegalArgumentException("seed-generator.diameter must be an even number between 2 and 60000");
            }
            int radius = diameter / 2;
            if (requirements.isEmpty() || isCertified(currentCertificate(radius, requirements))) {
                onSuccess.run();
                return;
            }
            running = new SeedGenerator(requirements, radius, onSuccess, onFailure);
            GameWorldManager.setPreparingSeed(true);
            for (KatzePlayer player : KatzePlayer.getOnlinePlayers()) GameLifecycleManager.sendToLobby(player);
            running.beginCandidate();
        } catch (Exception exception) {
            if (running != null) running.fail("Seed preparation failed: " + exception.getMessage(), exception);
            else onFailure.accept("Seed preparation failed: " + exception.getMessage());
        }
    }

    public static void cancel() {
        if (running == null) return;
        running.stop();
        certificate = null;
    }

    private static List<SeedRequirement> requirements() {
        return ChallengeManager.getActiveChallenges().stream()
                .sorted(Comparator.comparing(challenge -> challenge.getType().name()))
                .map(Challenge::getSeedRequirements).flatMap(List::stream).distinct().toList();
    }

    private static Certificate currentCertificate(int radius, List<SeedRequirement> requirements) {
        List<WorldSignature> worlds = GameWorldManager.getWorlds().stream().map(world -> new WorldSignature(
                world.getUID().toString(), world.getSeed(), world.getEnvironment(),
                world.getSpawnLocation().getX(), world.getSpawnLocation().getZ())).toList();
        return new Certificate(worlds, Bukkit.getVersion(), radius, requirements);
    }

    private static Path reportFile() {
        return KatzeGames.get().getDataDirectory().toPath().resolve("data/seed-validation.json");
    }

    private static boolean isCertified(Certificate current) {
        if (current.equals(certificate)) return true;
        try {
            if (!current.matches(reportFile())) return false;
            certificate = current;
            return true;
        } catch (IOException | RuntimeException exception) {
            KatzeGames.get().getLogger().log(Level.WARNING, "Failed to restore seed validation; the current map will be checked again.", exception);
            return false;
        }
    }

    private void beginCandidate() {
        if (running != this) return;
        if (!withinBudget()) return;
        try {
            search = new WorldSeedSearch(radius);
            candidate = currentCertificate(radius, requirements);
            List<SeedRequirement> checks = SeedRequirementOptimizer.optimize(requirements);
            validator = SeedValidator.batched(checks, search);
            candidateStarted = System.nanoTime();
            ticks = 0;
            if (budget.attempt() == 1 && requirements.stream().anyMatch(requirement -> requirement.objective().startsWith("coverage/"))) {
                announce("Unmapped indirect objectives require additional biome and structure coverage, respecting simplified groups and exclusions.");
            }
            announce("Checking battle seed " + budget.attempt() + "/" + budget.maxAttempts() + " (" + checks.size() + " checks from " + requirements.size() + " requirements).");
            task = Bukkit.getScheduler().runTaskTimer(KatzeGames.get(), this::tick, 1L, 1L);
        } catch (Exception exception) {
            fail("Unable to check battle seed: " + exception.getMessage(), exception);
        }
    }

    private void tick() {
        if (running != this || !withinBudget()) return;
        try {
            if (ticks++ % 20 == 0 && !requirements.equals(requirements())) {
                fail("Battle settings changed during seed preparation. Run /start again.", null);
                return;
            }
            if (!candidate.equals(currentCertificate(radius, requirements))) {
                fail("Battle worlds or spawn positions changed during seed preparation. Run /start again.", null);
                return;
            }
            long sliceStarted = System.nanoTime();
            SeedValidationResult result;
            int steps = 0;
            do {
                result = validator.step();
            } while (result == null && ++steps < 128 && System.nanoTime() - sliceStarted < tickBudgetNanos && !budget.expired());
            if (!withinBudget() || result == null) return;
            if (!requirements.equals(requirements())) {
                fail("Battle settings changed during seed preparation. Run /start again.", null);
                return;
            }
            long elapsedMillis = (System.nanoTime() - candidateStarted) / 1_000_000;
            String timing = " (" + elapsedMillis + " ms, " + search.locateCalls() + " locate calls)";
            if (result.passed()) {
                Certificate accepted = currentCertificate(radius, requirements);
                List<Witness> witnesses = result.found().entrySet().stream()
                        .map(entry -> new Witness(entry.getKey(), entry.getValue())).toList();
                JsonFiles.write(reportFile(),
                        new Report(accepted, budget.attempt(), witnesses));
                certificate = accepted;
                stop();
                announce("Battle seed accepted after " + budget.attempt() + " attempt(s)" + timing + ".");
                onSuccess.run();
            } else {
                task.cancel();
                task = null;
                announce("Seed rejected: missing " + result.missingRequirement() + timing + ".");
                if (!budget.retry()) {
                    fail("No suitable battle seed found after " + budget.attempt() + " attempts. Adjust requirements or retry /start.", null);
                    return;
                }
                GameWorldManager.reset(this::beginCandidate, () -> {
                    if (running == this) fail("Failed to replace rejected seed. Retry /reset.", null);
                });
            }
        } catch (Exception exception) {
            fail("Seed validation failed: " + exception.getMessage(), exception);
        }
    }

    private boolean withinBudget() {
        if (!budget.expired()) return true;
        fail("Battle seed search timed out after " + budget.attempt() + " attempt(s). Retry /start or adjust seed-generator limits.", null);
        return false;
    }

    private void fail(String reason, Exception exception) {
        stop();
        certificate = null;
        if (exception != null) KatzeGames.get().getLogger().log(Level.SEVERE, reason, exception);
        else KatzeGames.get().getLogger().warning(reason);
        onFailure.accept(reason);
    }

    private void stop() {
        if (task != null) task.cancel();
        task = null;
        running = null;
        GameWorldManager.setPreparingSeed(false);
    }

    private static void announce(String message) {
        KatzeGames.get().getLogger().info(message);
        Bukkit.getOnlinePlayers().stream().filter(player -> player.isOp())
                .forEach(player -> player.sendMessage(Component.text(message)));
    }
}
