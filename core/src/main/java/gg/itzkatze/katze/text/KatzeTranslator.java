package gg.itzkatze.katze.text;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class KatzeTranslator implements Translator {

    private static final Logger logger = LoggerFactory.getLogger("KatzeGames/Texts");
    private static final Key NAME = Key.key("katzegames", "texts");
    private static final Set<String> GERMAN_LANGUAGES = Set.of("de", "bar", "ksh", "nds", "sxu");

    private final MiniMessageTranslationStore store;
    private final Set<String> missingKeys = ConcurrentHashMap.newKeySet();

    KatzeTranslator(@NotNull MiniMessageTranslationStore store) {
        this.store = store;
    }

    static @NotNull Locale normalize(@NotNull Locale locale) {
        return GERMAN_LANGUAGES.contains(locale.getLanguage()) ? Locale.GERMAN : Locale.ENGLISH;
    }

    static boolean isOwnKey(@NotNull String key) {
        return key.startsWith("katze.") && !key.startsWith("katze.client.");
    }

    @Override
    public @NotNull Key name() {
        return NAME;
    }

    @Override
    public @NotNull TriState hasAnyTranslations() {
        return store.hasAnyTranslations();
    }

    @Override
    public boolean canTranslate(@NotNull String key, @NotNull Locale locale) {
        if (store.contains(key)) {
            return true;
        }
        if (isOwnKey(key) && missingKeys.add(key)) {
            logger.warn("No text for key {}", key);
        }
        return false;
    }

    @Override
    public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
        return null;
    }

    @Override
    public @Nullable Component translate(@NotNull TranslatableComponent component, @NotNull Locale locale) {
        return store.translate(component, normalize(locale));
    }
}
