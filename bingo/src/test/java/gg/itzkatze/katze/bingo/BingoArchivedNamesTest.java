package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.bingo.board.BingoObjectiveCatalog;
import gg.itzkatze.katze.bingo.match.BingoResultArchive;
import gg.itzkatze.katze.results.SavedBattle;
import gg.itzkatze.katze.text.TextStyle;
import gg.itzkatze.katze.util.JsonFiles;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class BingoArchivedNamesTest {
    @Test void unclaimedNamesRetainTranslationsAndNamedTargetsAfterJsonRoundTrip(@TempDir Path directory) throws Exception {
        var store = MiniMessageTranslationStore.create(Key.key("katze", "archive-test"), TextStyle.miniMessage());
        for (Locale locale : new Locale[]{Locale.ENGLISH, Locale.GERMAN}) {
            var properties = new Properties();
            try (var reader = new InputStreamReader(getClass().getResourceAsStream("/lang/bingo/objectives/" + locale.getLanguage() + ".properties"), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            for (String key : properties.stringPropertyNames()) store.register(key, locale, properties.getProperty(key));
        }
        GlobalTranslator.translator().addSource(store);
        try {
            var catalog = new BingoObjectiveCatalog();
            for (var objective : catalog.all()) {
                var cell = BingoResultArchive.cell(catalog.name(objective.id()), catalog.difficulty(objective.id()), catalog.icon(objective.id()).name(), -1, "", 0);
                Path path = directory.resolve("cell.json");
                JsonFiles.write(path, cell);
                var restored = JsonFiles.read(path, SavedBattle.BingoCell.class);
                assertEquals(cell, restored);
                assertFalse(restored.name().contains("katze.bingo.objective."), objective.id());
                assertFalse(restored.germanName().contains("katze.bingo.objective."), objective.id());
                assertFalse(restored.name().contains("<target>"), objective.id());
                assertFalse(restored.germanName().contains("<target>"), objective.id());
            }
        } finally {
            GlobalTranslator.translator().removeSource(store);
        }
    }
}
