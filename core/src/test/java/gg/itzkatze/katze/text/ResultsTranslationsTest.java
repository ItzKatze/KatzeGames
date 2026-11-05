package gg.itzkatze.katze.text;

import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultsTranslationsTest {
    @Test
    void savedResultsHaveMatchingValidEnglishAndGermanMessages() throws Exception {
        assertEquals(List.of(), TextCheck.problems(List.of(new Bundle("results", load("en"), load("de"))), List.of()));
    }

    private Map<String, String> load(String language) throws Exception {
        Properties properties = new Properties();
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/lang/results/" + language + ".properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties.stringPropertyNames().stream().collect(Collectors.toMap(key -> key, properties::getProperty));
    }
}
