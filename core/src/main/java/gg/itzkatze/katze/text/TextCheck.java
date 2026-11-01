package gg.itzkatze.katze.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class TextCheck {

    private static final Pattern CLIENT_ARGUMENT = Pattern.compile("%s", Pattern.LITERAL);
    private static final Pattern UNRESOLVED_TAG = Pattern.compile("</?\\p{L}");
    private static final Set<String> MINI_MESSAGE_COLORS = Stream.concat(NamedTextColor.NAMES.keys().stream(), Stream.of("grey", "dark_grey"))
            .collect(Collectors.toUnmodifiableSet());

    private TextCheck() {
    }

    static @NotNull List<String> problems(@NotNull Collection<Bundle> bundles, @NotNull List<String> strayFiles) {
        List<String> problems = new ArrayList<>();
        for (Bundle bundle : bundles) {
            if (bundle.english() == null) {
                problems.add("lang/" + bundle.name() + ": no en.properties");
            }
            if (bundle.german() == null) {
                problems.add("lang/" + bundle.name() + ": no de.properties");
            }
        }
        for (String file : strayFiles) {
            problems.add(file + ": not a bundle file");
        }
        for (Bundle bundle : bundles) {
            if (bundle.english() != null && bundle.german() != null) {
                checkKeys(bundle, bundle.english(), bundle.german(), problems);
            }
        }
        return problems;
    }

    private static void checkKeys(@NotNull Bundle bundle, @NotNull Map<String, String> english, @NotNull Map<String, String> german, @NotNull List<String> problems) {
        Pattern keyFormat = Pattern.compile("katze\\." + bundle.name() + "(\\.[a-z0-9_]+)+");
        Set<String> keys = new TreeSet<>(english.keySet());
        keys.addAll(german.keySet());
        for (String key : keys) {
            if (!keyFormat.matcher(key).matches()) {
                problems.add(key + ": key must be katze." + bundle.name() + ".<name>");
            }
            String englishText = english.get(key);
            String germanText = german.get(key);
            if (germanText == null) {
                problems.add(key + ": only in English");
            } else if (englishText == null) {
                problems.add(key + ": only in German");
            } else if (bundle.isClient()) {
                long englishCount = CLIENT_ARGUMENT.matcher(englishText).results().count();
                long germanCount = CLIENT_ARGUMENT.matcher(germanText).results().count();
                if (englishCount != germanCount) {
                    problems.add(key + ": %s count differs (en: " + englishCount + ", de: " + germanCount + ")");
                }
            } else {
                Set<String> englishTags = argumentTags(key, "en", englishText, problems);
                Set<String> germanTags = argumentTags(key, "de", germanText, problems);
                if (englishTags != null && germanTags != null && !englishTags.equals(germanTags)) {
                    problems.add(key + ": argument tags differ (en: " + englishTags + ", de: " + germanTags + ")");
                }
            }
        }
    }

    private static @Nullable Set<String> argumentTags(@NotNull String key, @NotNull String language, @NotNull String text, @NotNull List<String> problems) {
        ArgumentTags tags = new ArgumentTags();
        Component parsed;
        try {
            parsed = TextStyle.miniMessage().deserialize(text, tags);
        } catch (ParsingException e) {
            problems.add(key + " (" + language + "): " + e.getMessage());
            return null;
        }
        if (UNRESOLVED_TAG.matcher(PlainTextComponentSerializer.plainText().serialize(parsed)).find()) {
            problems.add(key + " (" + language + "): unresolved tag");
        }
        for (String name : tags.names) {
            if (MINI_MESSAGE_COLORS.contains(name)) {
                problems.add(key + " (" + language + "): " + name + " is a MiniMessage color, not a KatzeColor tag");
            }
        }
        return tags.names;
    }

    private static final class ArgumentTags implements TagResolver {

        private final Set<String> names = new TreeSet<>();

        @Override
        public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) {
            if (!has(name)) {
                return null;
            }
            names.add(name);
            return Tag.selfClosingInserting(Component.empty());
        }

        @Override
        public boolean has(@NotNull String name) {
            return !TextStyle.styleTags().has(name);
        }
    }
}
