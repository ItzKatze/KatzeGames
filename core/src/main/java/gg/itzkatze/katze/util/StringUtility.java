package gg.itzkatze.katze.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class StringUtility {

    /**
     * Converts a given time in seconds to a human-readable string.
     *
     * @param time The time in seconds to convert.
     * @return A string in the format "xd yh zm ws" representing the given time.
     */
    public static String formatTime(int time) {
        int days = time / 86400;
        int hours = time / 3600 % 24;
        int minutes = time / 60 % 60;
        int seconds = time % 60;

        StringBuilder message = new StringBuilder();

        if (days >= 1) {
            message.append(days).append("d ");
        }
        if (hours >= 1) {
            message.append(hours).append("h ");
        }
        if (minutes >= 1) {
            message.append(minutes).append("m ");
        }
        if (seconds >= 1) {
            message.append(seconds).append("s ");
        }
        if (time == 0) {
            message.append("0s");
        }
        return message.toString();
    }

    /**
     * Formats a given string by replacing underscores with spaces, splitting the string into words, capitalizing the first letter of each word, and joining the words back together with spaces.
     *
     * @param input The string to format.
     * @return A formatted string.
     */
    public static @NotNull String formatName(String input) {
        String[] words = input.replace('_', ' ').split("\\s+");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!formatted.isEmpty()) formatted.append(' ');
            String formattedWord = word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT);
            formatted.append(formattedWord);
        }
        return formatted.toString();
    }

    public static List<String> splitByWordAndLength(String text, int maxLength) {
        if (maxLength < 1) throw new IllegalArgumentException("Maximum line length must be positive");
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;

        String[] words = text.strip().split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (!line.isEmpty() && line.length() + 1 + word.length() > maxLength) {
                lines.add(line.toString());
                line = new StringBuilder();
            }
            if (!line.isEmpty()) line.append(" ");
            line.append(word);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    /**
     * Splits the input string at line breaks and returns an unmodifiable list.
     *
     * @param input The string to be split.
     * @return An unmodifiable list of lines.
     */
    public static @Unmodifiable List<String> formatLineBreaks(@NotNull String input) {
        return List.of(input.split("\\R"));
    }

    @NotNull public static <T extends Collection<? super String>> T copyPartialMatches(
            @NotNull final String token,
            @NotNull final Iterable<String> originals,
            @NotNull final T collection
    ) throws UnsupportedOperationException, IllegalArgumentException {
        for (String string : originals) {
            if (startsWithIgnoreCase(string, token)) {
                collection.add(string);
            }
        }
        return collection;
    }

    public static boolean startsWithIgnoreCase(@NotNull final String string, @NotNull final String prefix) throws IllegalArgumentException, NullPointerException {
        if (string.length() < prefix.length()) {
            return false;
        }
        return string.regionMatches(true, 0, prefix, 0, prefix.length());
    }

}
