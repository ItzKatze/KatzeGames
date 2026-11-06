package gg.itzkatze.katze.lobby.cosmetics;

import com.google.gson.Gson;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BlockHighlightCosmeticTest {

    private final CosmeticCatalog catalog = CosmeticCatalog.standard();

    @Test
    void catalogContainsAllSixteenDyesAsDistinctHighlightColors() {
        Set<Material> dyes = Arrays.stream(DyeColor.values())
                .map(dye -> Material.valueOf(dye.name() + "_DYE")).collect(Collectors.toSet());
        assertEquals(dyes, catalog.in(CosmeticCategory.HIGHLIGHT).stream().map(Cosmetic::icon).collect(Collectors.toSet()));
        assertEquals(16, catalog.in(CosmeticCategory.HIGHLIGHT).size());
        assertEquals(16, Arrays.stream(BlockHighlightCosmetic.values()).map(BlockHighlightCosmetic::color).distinct().count());
        for (BlockHighlightCosmetic cosmetic : BlockHighlightCosmetic.values()) {
            assertSame(cosmetic, catalog.find(cosmetic.id()).orElseThrow());
        }
    }

    @Test
    void equippedHighlightSurvivesSavingAlongsideOtherCategories() {
        CosmeticProfile profile = new CosmeticProfile();
        Cosmetic block = catalog.in(CosmeticCategory.BLOCK).getFirst();
        profile.winCosmetic(block, 0);
        profile.winCosmetic(BlockHighlightCosmetic.CYAN, 0);
        assertTrue(profile.equip(block, catalog));
        assertTrue(profile.equip(BlockHighlightCosmetic.CYAN, catalog));
        Gson gson = new Gson();
        CosmeticProfile restored = gson.fromJson(gson.toJson(profile), CosmeticProfile.class);
        restored.normalize(catalog);
        assertSame(BlockHighlightCosmetic.CYAN, restored.equipped(CosmeticCategory.HIGHLIGHT, catalog).orElseThrow());
        assertSame(block, restored.equipped(CosmeticCategory.BLOCK, catalog).orElseThrow());
        restored.unequip(CosmeticCategory.HIGHLIGHT, catalog);
        assertTrue(restored.equipped(CosmeticCategory.HIGHLIGHT, catalog).isEmpty());
        assertSame(block, restored.equipped(CosmeticCategory.BLOCK, catalog).orElseThrow());
    }

    @Test
    void allHighlightNamesAndMenuLabelsHaveEnglishAndGermanTranslations() throws Exception {
        for (String language : new String[]{"en", "de"}) {
            Properties messages = new Properties();
            try (var reader = new InputStreamReader(getClass().getResourceAsStream("/lang/cosmetics/" + language + ".properties"), StandardCharsets.UTF_8)) {
                messages.load(reader);
            }
            assertNotNull(messages.getProperty("katze.cosmetics.category.highlight"));
            assertNotNull(messages.getProperty("katze.cosmetics.default.highlight"));
            for (BlockHighlightCosmetic cosmetic : BlockHighlightCosmetic.values()) {
                String key = "katze.cosmetics.cosmetic." + cosmetic.category().name().toLowerCase(Locale.ROOT) + "." + cosmetic.id();
                assertNotNull(messages.getProperty(key), key);
            }
        }
    }
}
