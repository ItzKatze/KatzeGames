package gg.itzkatze.katze.game.seed;

import gg.itzkatze.katze.game.seed.SeedGenerator.Certificate;
import gg.itzkatze.katze.game.seed.SeedGenerator.Report;
import gg.itzkatze.katze.game.seed.SeedGenerator.Witness;
import gg.itzkatze.katze.game.seed.SeedGenerator.WorldSignature;
import gg.itzkatze.katze.util.JsonFiles;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class SeedCertificateTest {
    private static final SeedFeature VILLAGE = new SeedFeature(SeedFeature.Kind.STRUCTURE, World.Environment.NORMAL, "minecraft:village_plains");
    private static final List<SeedRequirement> REQUIREMENTS = List.of(new SeedRequirement("village", List.of(VILLAGE)));

    @TempDir Path directory;

    @Test
    void restoresAcceptedMapFromTheExistingValidationReport() throws Exception {
        Certificate accepted = certificate(worlds(), "server", 5000, REQUIREMENTS);
        Path report = save(accepted);

        Certificate restarted = certificate(List.copyOf(accepted.worlds()), "server", 5000, List.copyOf(REQUIREMENTS));

        assertTrue(restarted.matches(report));
    }

    @Test
    void rejectsReplacedWorldsChangedSeedsEnvironmentsAndSpawns() throws Exception {
        List<WorldSignature> worlds = worlds();
        Path report = save(certificate(worlds, "server", 5000, REQUIREMENTS));
        WorldSignature original = worlds.getFirst();
        List<WorldSignature> replacements = List.of(
                new WorldSignature(UUID.randomUUID().toString(), original.seed(), original.environment(), 0, 0),
                new WorldSignature(original.id(), 456, original.environment(), 0, 0),
                new WorldSignature(original.id(), original.seed(), World.Environment.NETHER, 0, 0),
                new WorldSignature(original.id(), original.seed(), original.environment(), 16, 0),
                new WorldSignature(original.id(), original.seed(), original.environment(), 0, 16));

        for (WorldSignature replacement : replacements) {
            assertFalse(certificate(List.of(replacement, worlds.get(1), worlds.get(2)), "server", 5000, REQUIREMENTS).matches(report));
        }
        assertFalse(certificate(worlds.subList(0, 2), "server", 5000, REQUIREMENTS).matches(report));
    }

    @Test
    void rejectsChangedRequirementsSearchRadiusAndServerVersion() throws Exception {
        List<WorldSignature> worlds = worlds();
        Path report = save(certificate(worlds, "server", 5000, REQUIREMENTS));

        assertFalse(certificate(worlds, "updated server", 5000, REQUIREMENTS).matches(report));
        assertFalse(certificate(worlds, "server", 6000, REQUIREMENTS).matches(report));
        List<SeedRequirement> changed = List.of(new SeedRequirement("different objective", List.of(VILLAGE)));
        assertFalse(certificate(worlds, "server", 5000, changed).matches(report));
    }

    @Test
    void missingOrEmptyReportsDoNotCertifyTheMap() throws Exception {
        Certificate current = certificate(worlds(), "server", 5000, REQUIREMENTS);
        Path report = directory.resolve("seed-validation.json");

        assertFalse(current.matches(report));
        Files.writeString(report, "null");
        assertFalse(current.matches(report));
        Files.writeString(report, "{}");
        assertFalse(current.matches(report));
    }

    private Path save(Certificate certificate) throws Exception {
        Path report = directory.resolve("seed-validation.json");
        JsonFiles.write(report, new Report(certificate, 3,
                List.of(new Witness(VILLAGE, new SeedValidationResult.Position(12, 60, 30)))));
        return report;
    }

    private static Certificate certificate(List<WorldSignature> worlds, String version, int radius, List<SeedRequirement> requirements) {
        return new Certificate(worlds, version, radius, requirements);
    }

    private static List<WorldSignature> worlds() {
        return List.of(
                new WorldSignature(UUID.randomUUID().toString(), 123, World.Environment.NORMAL, 0, 0),
                new WorldSignature(UUID.randomUUID().toString(), 123, World.Environment.NETHER, 0, 0),
                new WorldSignature(UUID.randomUUID().toString(), 123, World.Environment.THE_END, 0, 0));
    }
}
