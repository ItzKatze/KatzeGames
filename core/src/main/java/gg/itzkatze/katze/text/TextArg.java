package gg.itzkatze.katze.text;

import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

public final class TextArg {

    private static final String DAY_PATTERN = "MMM d";

    private TextArg() {
    }

    public static @NotNull ComponentLike text(@NotNull String name, @NotNull String value) {
        return Argument.string(name, value);
    }

    public static @NotNull ComponentLike component(@NotNull String name, @NotNull ComponentLike value) {
        return Argument.component(name, value);
    }

    public static @NotNull ComponentLike number(@NotNull String name, long value) {
        return formatted(name, (words, locale) -> counted(NumberFormat.getIntegerInstance(locale).format(value), value, words));
    }

    public static @NotNull ComponentLike signed(@NotNull String name, long value) {
        return formatted(name, (words, locale) -> (value >= 0 ? "+" : "-") + counted(NumberFormat.getIntegerInstance(locale).format(Math.abs(value)), value, words));
    }

    public static @NotNull ComponentLike decimal(@NotNull String name, double value) {
        return formatted(name, (arguments, locale) -> {
            NumberFormat format = NumberFormat.getNumberInstance(locale);
            format.setMinimumFractionDigits(1);
            format.setMaximumFractionDigits(1);
            return format.format(value);
        });
    }

    public static @NotNull ComponentLike day(@NotNull String name, @NotNull MonthDay value) {
        return formatted(name, (arguments, locale) -> DateTimeFormatter.ofPattern(arguments.isEmpty() ? DAY_PATTERN : arguments.getFirst(), locale).format(value));
    }

    private static @NotNull ComponentLike formatted(@NotNull String name, @NotNull BiFunction<List<String>, Locale, String> format) {
        return Argument.tagResolver(TagResolver.resolver(name, (queue, context) -> {
            List<String> arguments = new ArrayList<>();
            while (queue.hasNext()) {
                arguments.add(queue.pop().value());
            }
            Pointered target = context.target();
            Locale locale = target == null ? Locale.ENGLISH : target.getOrDefault(Identity.LOCALE, Locale.ENGLISH);
            return Tag.selfClosingInserting(Component.text(format.apply(arguments, locale)));
        }));
    }

    private static @NotNull String counted(@NotNull String number, long value, @NotNull List<String> words) {
        if (words.size() != 2) {
            return number;
        }
        return number + " " + (Math.abs(value) == 1 ? words.get(0) : words.get(1));
    }
}
