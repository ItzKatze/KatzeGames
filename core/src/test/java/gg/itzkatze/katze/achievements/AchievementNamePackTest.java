package gg.itzkatze.katze.achievements;

import org.junit.jupiter.api.Test;
import net.kyori.adventure.text.TextComponent;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AchievementNamePackTest {
    @Test void missingOptionalPackFallsBackToTranslatedTitles() throws Exception {
        AchievementNamePack pack = AchievementNamePack.read(null);
        for (Achievement achievement : Achievement.values()) assertEquals(achievement.title(), pack.title(achievement));
    }

    @Test void presentPackStillRequiresAllKnownNamesAndRejectsUnknownNames() {
        assertThrows(IllegalStateException.class, () -> read("tame_animal=Example"));
        assertThrows(IllegalStateException.class, () -> read(AchievementNamePackFixtures.names() + "\nunknown=Example"));
        assertThrows(IllegalStateException.class, () -> read(AchievementNamePackFixtures.names().replace("Custom tame_animal", "")));
    }

    @Test void presentPackPreservesUnicodeNames() throws Exception {
        AchievementNamePack pack = read(AchievementNamePackFixtures.names().replace("Custom tame_animal", "Überraschung"));
        assertEquals("Überraschung", assertInstanceOf(TextComponent.class, pack.title(Achievement.TAME_ANIMAL)).content());
    }

    private AchievementNamePack read(String names) throws Exception {
        return AchievementNamePack.read(new ByteArrayInputStream(names.getBytes(StandardCharsets.UTF_8)));
    }
}
