package gg.itzkatze.katze.achievements;

import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public final class AchievementNamePack {
    private static final AchievementNamePack CUSTOM = loadCustom();
    private final Map<String, String> names;

    private AchievementNamePack(Map<String, String> names) {
        this.names = Map.copyOf(names);
    }

    public static AchievementNamePack custom() { return CUSTOM; }

    public Component title(Achievement achievement) {
        String name = names.get(achievement.id());
        return name == null ? achievement.title() : Component.text(name);
    }

    private static AchievementNamePack loadCustom() {
        try (InputStream stream = AchievementNamePack.class.getResourceAsStream("/text-packs/achievements/custom.properties")) {
            return read(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read Custom achievement name pack", e);
        }
    }

    static AchievementNamePack read(InputStream stream) throws IOException {
        if (stream == null) return new AchievementNamePack(Map.of());
        Properties properties = new Properties();
        properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        Set<String> expected = Arrays.stream(Achievement.values()).map(Achievement::id).collect(Collectors.toSet());
        if (!properties.stringPropertyNames().equals(expected) || properties.values().stream().anyMatch(value -> value.toString().isBlank())) {
            throw new IllegalStateException("Custom achievement name pack must contain every achievement name exactly once");
        }
        return new AchievementNamePack(properties.stringPropertyNames().stream().collect(Collectors.toMap(key -> key, properties::getProperty)));
    }
}
