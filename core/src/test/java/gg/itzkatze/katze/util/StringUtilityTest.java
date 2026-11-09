package gg.itzkatze.katze.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringUtilityTest {

    @Test
    void formatsNamesWithRepeatedSeparators() {
        assertEquals("Iron Ingot", StringUtility.formatName(" __IRON__  INGOT_ "));
        assertEquals("", StringUtility.formatName("___"));
        assertEquals("", StringUtility.formatName(""));
    }

    @Test
    void formatsMaterialNamesIndependentlyOfServerLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("Iron Ingot", StringUtility.formatName("IRON_INGOT"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void wrappingCountsTheSpaceBetweenWords() {
        assertEquals(List.of("iron", "ingot"), StringUtility.splitByWordAndLength("iron ingot", 9));
        assertEquals(List.of("iron ingot"), StringUtility.splitByWordAndLength("iron ingot", 10));
    }

    @Test
    void wrappingPreservesLongWordsWithoutEmptyLines() {
        assertEquals(List.of("diamond", "ore"), StringUtility.splitByWordAndLength(" diamond   ore ", 3));
        assertEquals(List.of(), StringUtility.splitByWordAndLength("  ", 3));
        assertEquals(List.of(), StringUtility.splitByWordAndLength(null, 3));
        assertThrows(IllegalArgumentException.class, () -> StringUtility.splitByWordAndLength("iron", 0));
    }

    @Test
    void lineBreaksHandleWindowsAndUnixAndReturnAnUnmodifiableList() {
        List<String> lines = StringUtility.formatLineBreaks("iron\r\ningot\nore");

        assertEquals(List.of("iron", "ingot", "ore"), lines);
        assertThrows(UnsupportedOperationException.class, () -> lines.set(0, "gold"));
    }
}
