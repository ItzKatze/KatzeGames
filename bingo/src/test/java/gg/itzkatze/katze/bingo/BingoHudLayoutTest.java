package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.bingo.board.BingoObjective.Category;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class BingoHudLayoutTest {
    @Test void ownershipOnlyChangesTheFrameWhileNativeSpritesKeepTheirOwnGlyphs() {
        String neutral = BingoHudLayout.cell(12, -1, false, Category.ITEM);
        for (int owner = 0; owner < 2; owner++) {
            for (boolean winning : new boolean[]{false, true}) {
                String owned = BingoHudLayout.cell(12, owner, winning, Category.ITEM);
                assertNotEquals(neutral.charAt(0), owned.charAt(0));
                assertEquals(neutral.substring(1), owned.substring(1));
                assertEquals(0xF0000 + 12, owned.codePoints().toArray()[2]);
            }
        }
    }
    @Test void largeCatalogGlyphsDoNotOverlapIconsPaddingOrOwnershipFrames() {
        for (int index : new int[]{0, 1024, 2048, 4000}) {
            int[] points = BingoHudLayout.cell(index, -1, false, Category.ITEM).codePoints().toArray();
            assertEquals(6, points.length);
            assertEquals(0xF0000 + index, points[2]);
            assertEquals(0xF4000 + index, points[3]);
            assertNotEquals(points[2], points[3]);
        }
    }
    @Test void categoriesHaveDistinctBadgesInsideTheOwnershipFrame() {
        var glyphs = new HashSet<Integer>();
        for (Category category : Category.values()) {
            int[] points = BingoHudLayout.cell(12, -1, false, category).codePoints().toArray();
            assertTrue(glyphs.add(points[4]));
            assertEquals(0xE030 + category.ordinal(), points[4]);
            assertEquals(0xE013, points[5]);
        }
        assertTrue(BingoHudLayout.BADGE_OFFSET > 0);
        assertEquals(BingoHudLayout.CELL_SIZE - 1, BingoHudLayout.BADGE_OFFSET + BingoHudLayout.BADGE_SIZE);
    }

    @Test void competitorNamesAndScoresAreShownInFull() {
        for (String name : new String[]{"IchmagYorkleffs", "WWWWWWWWWWWWWWWW", "A very long team name"}) {
            String line = BingoHudLayout.competitor("■", name, 25);
            assertEquals("■ " + name + ": 25", line);
        }
    }

    @Test void boardHasOnePixelBetweenRowsAndEnoughBlankScoreboardLinesBelow() {
        assertEquals(7, BingoHudLayout.ASCENT);
        assertEquals(BingoHudLayout.CELL_SIZE + 1, BingoHudLayout.ROW_PITCH);
        assertEquals(104, BingoHudLayout.BOARD_HEIGHT);
        assertTrue((BingoHudLayout.BOARD_SPACER_LINES + 1) * 9 >= BingoHudLayout.BOARD_HEIGHT);
        assertEquals(15, 2 + 1 + 1 + BingoHudLayout.BOARD_SPACER_LINES);
    }
}
