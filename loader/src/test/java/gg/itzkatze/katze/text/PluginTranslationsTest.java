package gg.itzkatze.katze.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class PluginTranslationsTest {
    @Test void packagedTranslationsPassTheSameValidationAsPluginStartup() {
        try {
            Texts.load(Path.of(System.getProperty("katzegames.test.pluginJar")).toFile());
            assertEquals("Armor Templates", text("katze.objective.exclusion.armor_templates.name", Locale.ENGLISH));
            assertNotEquals("katze.objective.exclusion.armor_templates.name", text("katze.objective.exclusion.armor_templates.name", Locale.GERMAN));
        } finally { Texts.unload(); }
    }

    private String text(String key, Locale locale) {
        return PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(Component.translatable(key), locale));
    }
}
