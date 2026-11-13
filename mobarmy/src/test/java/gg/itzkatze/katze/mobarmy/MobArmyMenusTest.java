package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyMenusTest {
    @Test void waveLoreTotalsDifferentVariantsOfTheSameType() {
        var side = new MobArmyMatch.Side();
        side.mobs.addAll(List.of(mob("DROWNED", "trident", 0), mob("DROWNED", "unarmed", 0),
                mob("DROWNED", "baby", 1), mob("ZOMBIE", "adult", 0)));

        var lore = MobArmyMenus.waveLore(side, 0);

        assertEquals(2, lore.size());
        assertEquals("2x ", ((TextComponent) lore.getFirst()).content());
        assertEquals("1x ", ((TextComponent) lore.get(1)).content());
        lore.forEach(line -> {
            assertEquals(KatzeColor.WHITE, line.color());
            assertEquals(TextDecoration.State.FALSE, line.decoration(TextDecoration.ITALIC));
        });
    }

    @Test void configuratorLoreHasWhiteCountsAndGrayHintsWithoutRawMobData() {
        var lore = MobArmyMenus.mobLore(List.of(mob("DROWNED", "trident", 0), mob("DROWNED", "unarmed", -1)), 0, false);

        assertEquals(5, lore.size());
        assertEquals(KatzeColor.WHITE, lore.getFirst().color());
        assertEquals(KatzeColor.WHITE, lore.get(1).color());
        assertEquals(Component.empty().color(KatzeColor.WHITE).decoration(TextDecoration.ITALIC, false), lore.get(2));
        assertEquals(KatzeColor.LIGHT_GRAY, lore.get(3).color());
        assertEquals(KatzeColor.LIGHT_GRAY, lore.get(4).color());
        lore.forEach(line -> assertEquals(TextDecoration.State.FALSE, line.decoration(TextDecoration.ITALIC)));
    }

    @Test void detailsLoreShowsOnlyTheCombinedCollectedCount() {
        var lore = MobArmyMenus.mobLore(List.of(mob("DROWNED", "trident", 0), mob("DROWNED", "unarmed", -1)), -1, true);
        assertEquals(1, lore.size());
        assertEquals(KatzeColor.WHITE, lore.getFirst().color());
    }

    private static MobArmyMatch.CapturedMob mob(String type, String variant, int wave) {
        var mob = new MobArmyMatch.CapturedMob();
        mob.type = type;
        mob.variant = variant;
        mob.wave = wave;
        return mob;
    }
}
