package gg.itzkatze.katze.bingo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BingoHudLayoutTest {
    @Test void ownershipOnlyChangesTheFrameWhileNativeSpritesKeepTheirOwnGlyphs() {
        String neutral = BingoHudLayout.cell(12, -1, false);
        for (int owner = 0; owner < 2; owner++) {
            for (boolean winning : new boolean[]{false, true}) {
                String owned = BingoHudLayout.cell(12, owner, winning);
                assertNotEquals(neutral.charAt(0), owned.charAt(0));
                assertEquals(neutral.substring(1), owned.substring(1));
                assertEquals(0xF0000 + 12, owned.codePoints().toArray()[2]);
            }
        }
    }
    @Test void largeCatalogGlyphsDoNotOverlapIconsPaddingOrOwnershipFrames() {
        for (int index : new int[]{0, 1024, 2048, 4000}) {
            int[] points = BingoHudLayout.cell(index, -1, false).codePoints().toArray();
            assertEquals(4, points.length);
            assertEquals(0xF0000 + index, points[2]);
            assertEquals(0xF4000 + index, points[3]);
            assertNotEquals(points[2], points[3]);
        }
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
        assertEquals(94, BingoHudLayout.BOARD_HEIGHT);
        assertTrue((BingoHudLayout.BOARD_SPACER_LINES + 1) * 9 >= BingoHudLayout.BOARD_HEIGHT);
        assertEquals(15, 2 + 1 + 1 + 1 + BingoHudLayout.BOARD_SPACER_LINES);
    }
}
