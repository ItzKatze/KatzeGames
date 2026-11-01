package gg.itzkatze.katze.text;

import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

public final class TextStyle {

    private static final TagResolver STYLE_TAGS = TagResolver.resolver(
            StandardTags.decorations(),
            StandardTags.reset(),
            StandardTags.newline(),
            StandardTags.clickEvent(),
            StandardTags.hoverEvent(),
            colorTags());
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().tags(STYLE_TAGS).build();

    private TextStyle() {
    }

    public static @NotNull MiniMessage miniMessage() {
        return MINI_MESSAGE;
    }

    public static @NotNull TagResolver styleTags() {
        return STYLE_TAGS;
    }

    private static @NotNull TagResolver colorTags() {
        TagResolver.Builder colors = TagResolver.builder();
        for (Field field : KatzeColor.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == TextColor.class) {
                colors.resolver(Placeholder.styling(field.getName().toLowerCase(Locale.ROOT), color(field)));
            }
        }
        return colors.build();
    }

    private static @NotNull TextColor color(@NotNull Field field) {
        try {
            return (TextColor) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read KatzeColor." + field.getName(), e);
        }
    }
}
