package gg.itzkatze.katze.text;

import gg.itzkatze.katze.achievements.Achievement;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.text.NumberFormat;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

final class AchievementTranslationsTest {
    @Test
    void catalogueAndUiHaveMatchingValidTranslations() throws Exception {
        Map<String, String> english = load("en");
        Map<String, String> german = load("de");
        assertEquals(List.of(), TextCheck.problems(List.of(new Bundle("achievements", english, german)), List.of()));
        for (Achievement achievement : Achievement.values()) {
            assertTrue(english.containsKey("katze.achievements." + achievement.id() + ".name"));
            assertTrue(english.containsKey("katze.achievements." + achievement.id() + ".description"));
            if (achievement.stages().size() > 1) assertTrue(english.containsKey("katze.achievements." + achievement.id() + ".stage_description"));
        }
    }

    @Test
    void stageDescriptionsRevealOnlyTheCompletedStageTarget() throws Exception {
        MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("katzegames", "achievement-test"), TextStyle.miniMessage());
        store.defaultLocale(Locale.ENGLISH);
        store.registerAll(Locale.ENGLISH, load("en"));
        KatzeTranslator translator = new KatzeTranslator(store);
        for (Achievement achievement : Achievement.values()) {
            if (achievement.stages().size() == 1) continue;
            for (var stage : achievement.stages()) {
                Component translated = translator.translate(assertInstanceOf(TranslatableComponent.class, achievement.description(stage)), Locale.ENGLISH);
                assertNotNull(translated);
                String text = PlainTextComponentSerializer.plainText().serialize(translated);
                List<String> targets = Pattern.compile("\\d[\\d,]*").matcher(text).results().map(match -> match.group()).toList();
                assertEquals(List.of(NumberFormat.getIntegerInstance(Locale.ENGLISH).format(stage.target())), targets, achievement.id());
            }
        }
    }

    private Map<String, String> load(String language) throws Exception {
        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/lang/achievements/" + language + ".properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties.stringPropertyNames().stream().collect(Collectors.toMap(key -> key, properties::getProperty));
    }
}
