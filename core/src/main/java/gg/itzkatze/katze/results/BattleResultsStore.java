package gg.itzkatze.katze.results;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.util.JsonFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BattleResultsStore {
    private static final Logger logger = LoggerFactory.getLogger(BattleResultsStore.class);
    private final Path directory;
    private final Path legacyDirectory;

    public BattleResultsStore(Path directory) {
        this(directory, null);
    }

    public BattleResultsStore(Path directory, Path legacyDirectory) {
        this.directory = directory;
        this.legacyDirectory = legacyDirectory;
    }

    public static BattleResultsStore installed() {
        Path data = KatzeGames.get().getDataDirectory().toPath().resolve("data");
        return new BattleResultsStore(data.resolve("results"), data.resolve("forcebattle-results"));
    }

    public void save(SavedBattle battle) throws IOException {
        JsonFiles.write(directory.resolve(battle.id() + ".json"), battle);
    }

    public List<SavedBattle> forPlayer(UUID player) throws IOException {
        Map<UUID, SavedBattle> rounds = new LinkedHashMap<>();
        for (Path source : legacyDirectory == null ? List.of(directory) : List.of(directory, legacyDirectory)) {
            if (!Files.isDirectory(source)) continue;
            try (var paths = Files.list(source)) {
                for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).toList()) {
                    try {
                        SavedBattle battle = JsonFiles.read(path, SavedBattle.class);
                        if (battle != null && battle.includes(player)) rounds.putIfAbsent(battle.id(), battle);
                    } catch (IOException | RuntimeException e) {
                        logger.warn("Unable to read saved round {}", path, e);
                    }
                }
            }
        }
        return rounds.values().stream().sorted(Comparator.comparingLong(SavedBattle::endedAt).reversed()).toList();
    }

    public Optional<SavedBattle> findForPlayer(UUID round, UUID player) throws IOException {
        SavedBattle battle = JsonFiles.read(directory.resolve(round + ".json"), SavedBattle.class);
        if (battle == null && legacyDirectory != null) battle = JsonFiles.read(legacyDirectory.resolve(round + ".json"), SavedBattle.class);
        return battle != null && battle.includes(player) ? Optional.of(battle) : Optional.empty();
    }
}
