package gg.itzkatze.katze.text;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.WrittenBookContent;
import io.papermc.paper.text.Filtered;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Texts {

    private static final Logger logger = LoggerFactory.getLogger("KatzeGames/" + Texts.class.getSimpleName());
    private static final Pattern BUNDLE_FILE = Pattern.compile("lang/(?:([a-z0-9_]+)/)?([a-z0-9_]+)/(en|de)\\.properties");
    private static final Set<String> missingClientKeys = ConcurrentHashMap.newKeySet();

    private static @Nullable KatzeTranslator translator;
    private static Map<String, String> clientTexts = Map.of();

    private Texts() {
    }

    public static void load(@NotNull File pluginJar) {
        Map<String, Map<String, Map<String, String>>> files = new TreeMap<>();
        List<String> strayFiles = new ArrayList<>();
        try (JarFile jar = new JarFile(pluginJar)) {
            for (JarEntry entry : Collections.list(jar.entries())) {
                if (entry.isDirectory() || !entry.getName().startsWith("lang/")) {
                    continue;
                }
                Matcher bundleFile = BUNDLE_FILE.matcher(entry.getName());
                if (bundleFile.matches()) {
                    String bundleName = bundleFile.group(1) == null ? bundleFile.group(2) : bundleFile.group(1);
                    String language = bundleFile.group(3);
                    Map<String, String> texts = files.computeIfAbsent(bundleName, bundle -> new HashMap<>())
                            .computeIfAbsent(language, ignored -> new HashMap<>());
                    for (Map.Entry<String, String> text : read(jar, entry).entrySet()) {
                        if (texts.putIfAbsent(text.getKey(), text.getValue()) != null) {
                            throw new IllegalStateException("Duplicate text key " + text.getKey() + " in bundle " + bundleName);
                        }
                    }
                } else {
                    strayFiles.add(entry.getName());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the texts from " + pluginJar, e);
        }
        List<Bundle> bundles = files.entrySet().stream()
                .map(bundle -> new Bundle(bundle.getKey(), bundle.getValue().get("en"), bundle.getValue().get("de")))
                .toList();

        List<String> problems = TextCheck.problems(bundles, strayFiles);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Texts are incomplete:\n  - " + String.join("\n  - ", problems));
        }

        MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("katzegames", "store"), TextStyle.miniMessage());
        store.defaultLocale(Locale.ENGLISH);
        int texts = 0;
        for (Bundle bundle : bundles) {
            Map<String, String> english = bundle.english();
            Map<String, String> german = bundle.german();
            texts += english.size();
            if (bundle.isClient()) {
                clientTexts = english;
            } else {
                store.registerAll(Locale.ENGLISH, english);
                store.registerAll(Locale.GERMAN, german);
            }
        }
        translator = new KatzeTranslator(store);
        GlobalTranslator.translator().addSource(translator);
        logger.info("Loaded {} texts in {} bundles.", texts, bundles.size());
    }

    public static void unload() {
        if (translator == null) {
            return;
        }
        GlobalTranslator.translator().removeSource(translator);
        translator = null;
    }

    public static @NotNull Component render(@NotNull Component component, @NotNull Player player) {
        return GlobalTranslator.render(component, player.locale());
    }

    public static @Nullable ItemStack render(@Nullable ItemStack item, @NotNull Player player) {
        if (item == null || item.isEmpty()) {
            return item;
        }
        Component customName = item.getData(DataComponentTypes.CUSTOM_NAME);
        Component itemName = item.getData(DataComponentTypes.ITEM_NAME);
        ItemLore lore = item.getData(DataComponentTypes.LORE);
        WrittenBookContent book = item.getData(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        boolean ownCustomName = holdsOwnKey(customName);
        boolean ownItemName = holdsOwnKey(itemName);
        boolean ownLore = lore != null && lore.lines().stream().anyMatch(Texts::holdsOwnKey);
        boolean ownPages = book != null && book.pages().stream().anyMatch(page -> holdsOwnKey(page.raw()) || holdsOwnKey(page.filtered()));
        if (!ownCustomName && !ownItemName && !ownLore && !ownPages) {
            return item;
        }

        ItemStack rendered = item.clone();
        if (ownCustomName) {
            rendered.setData(DataComponentTypes.CUSTOM_NAME, render(customName, player));
        }
        if (ownItemName) {
            rendered.setData(DataComponentTypes.ITEM_NAME, render(itemName, player));
        }
        if (ownLore) {
            List<Component> loreLines = new ArrayList<>();
            for (Component line : lore.lines()) {
                loreLines.addAll(holdsOwnKey(line) ? lines(render(line, player)) : List.of(line));
            }
            rendered.setData(DataComponentTypes.LORE, ItemLore.lore(loreLines));
        }
        if (ownPages) {
            WrittenBookContent.Builder pages = WrittenBookContent.writtenBookContent(book.title(), book.author())
                    .generation(book.generation())
                    .resolved(book.resolved());
            for (Filtered<Component> page : book.pages()) {
                Component filtered = page.filtered();
                pages.addFilteredPage(Filtered.of(render(page.raw(), player), filtered == null ? null : render(filtered, player)));
            }
            rendered.setData(DataComponentTypes.WRITTEN_BOOK_CONTENT, pages.build());
        }
        return rendered;
    }

    public static @NotNull List<Component> lines(@NotNull Component component) {
        List<List<Component>> lines = new ArrayList<>();
        lines.add(new ArrayList<>());
        split(component, Style.empty(), lines);
        if (lines.size() == 1) {
            return List.of(component);
        }
        List<Component> result = new ArrayList<>(lines.size());
        for (List<Component> pieces : lines) {
            result.add(pieces.size() == 1 ? pieces.getFirst() : Component.textOfChildren(pieces.toArray(ComponentLike[]::new)));
        }
        return result;
    }

    public static @NotNull Component client(@NotNull String key) {
        String english = clientTexts.get(key);
        if (english == null) {
            if (missingClientKeys.add(key)) {
                logger.warn("No client text for key {}", key);
            }
            english = key;
        }
        return Component.translatable(key).fallback(english);
    }

    private static @NotNull Map<String, String> read(@NotNull JarFile jar, @NotNull JarEntry entry) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        Map<String, String> texts = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            String normalizedKey = key.startsWith("\uFEFF") ? key.substring(1) : key;
            texts.put(normalizedKey, properties.getProperty(key));
        }
        return texts;
    }

    private static void split(@NotNull Component component, @NotNull Style inherited, @NotNull List<List<Component>> lines) {
        Style style = inherited.merge(component.style());
        if (!(component instanceof TextComponent text)) {
            lines.getLast().add(component.style(style));
            return;
        }
        String[] pieces = text.content().split("\n", -1);
        for (int i = 0; i < pieces.length; i++) {
            if (i > 0) {
                lines.add(new ArrayList<>());
            }
            if (!pieces[i].isEmpty()) {
                lines.getLast().add(Component.text(pieces[i], style));
            }
        }
        for (Component child : text.children()) {
            split(child, style, lines);
        }
    }

    private static boolean holdsOwnKey(@Nullable Component component) {
        if (component == null) {
            return false;
        }
        if (component instanceof TranslatableComponent translatable) {
            if (KatzeTranslator.isOwnKey(translatable.key())) {
                return true;
            }
            for (TranslationArgument argument : translatable.arguments()) {
                if (argument.value() instanceof Component value && holdsOwnKey(value)) {
                    return true;
                }
            }
        }
        for (Component child : component.children()) {
            if (holdsOwnKey(child)) {
                return true;
            }
        }
        return false;
    }
}
