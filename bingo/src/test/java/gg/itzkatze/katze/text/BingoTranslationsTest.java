package gg.itzkatze.katze.text;

import gg.itzkatze.katze.bingo.board.BingoObjectiveCatalog;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class BingoTranslationsTest {
    @Test void allBingoMessagesAndObjectivesHaveValidMatchingTranslations() throws Exception {
        Map<String, String> english = load("en");
        Map<String, String> german = load("de");
        assertEquals(List.of(), TextCheck.problems(List.of(new Bundle("bingo", english, german)), List.of()));
        for (var objective : new BingoObjectiveCatalog().all()) {
            assertTrue(english.containsKey("katze.bingo.objective." + objective.id()));
            assertTrue(german.containsKey("katze.bingo.objective." + objective.id()));
        }
    }
    private Map<String, String> load(String language) throws Exception {
        Map<String, String> result = new HashMap<>();
        for (String section : List.of("game", "objectives")) {
            Properties properties = new Properties();
            try (var reader = new InputStreamReader(getClass().getResourceAsStream("/lang/bingo/" + section + "/" + language + ".properties"), StandardCharsets.UTF_8)) { properties.load(reader); }
            for (String key : properties.stringPropertyNames()) assertNull(result.put(key, properties.getProperty(key)));
        }
        return result;
    }
}
