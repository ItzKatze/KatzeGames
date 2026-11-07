package gg.itzkatze.katze.achievements;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

final class AchievementNamePackFixtures {
    private AchievementNamePackFixtures() {}

    static String names() {
        return Arrays.stream(Achievement.values()).map(achievement -> achievement.id() + "=Custom " + achievement.id())
                .collect(Collectors.joining("\n"));
    }

    static AchievementNamePack custom() {
        try { return AchievementNamePack.read(new ByteArrayInputStream(names().getBytes(StandardCharsets.UTF_8))); }
        catch (IOException exception) { throw new UncheckedIOException(exception); }
    }
}
