package gg.itzkatze.katze.lobby.cosmetics;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmeticProfileTest {

    private final CosmeticCatalog catalog = CosmeticCatalog.standard();

    @Test
    void randomCyclesBetweenRunAndJumpIndependentlyForEachCategory() {
        CosmeticProfile profile = new CosmeticProfile();
        profile.equipRandom(CosmeticCategory.BLOCK, catalog);
        assertTrue(profile.isRandom(CosmeticCategory.BLOCK));
        assertFalse(profile.isRandomPerJump(CosmeticCategory.BLOCK));

        profile.equipRandom(CosmeticCategory.BLOCK, catalog);
        profile.equipRandom(CosmeticCategory.SOUND, catalog);
        assertTrue(profile.isRandomPerJump(CosmeticCategory.BLOCK));
        assertFalse(profile.isRandomPerJump(CosmeticCategory.SOUND));

        profile.equipRandom(CosmeticCategory.BLOCK, catalog);
        assertTrue(profile.isRandom(CosmeticCategory.BLOCK));
        assertFalse(profile.isRandomPerJump(CosmeticCategory.BLOCK));
    }

    @Test
    void randomModeSurvivesSerializationAndClearsWhenUnequipped() {
        CosmeticProfile profile = new CosmeticProfile();
        profile.equipRandom(CosmeticCategory.MUSIC, catalog);
        profile.equipRandom(CosmeticCategory.MUSIC, catalog);

        Gson gson = new Gson();
        CosmeticProfile restored = gson.fromJson(gson.toJson(profile), CosmeticProfile.class);
        restored.normalize(catalog);
        assertTrue(restored.isRandomPerJump(CosmeticCategory.MUSIC));

        restored.unequip(CosmeticCategory.MUSIC, catalog);
        assertFalse(restored.isRandom(CosmeticCategory.MUSIC));
        assertFalse(restored.isRandomPerJump(CosmeticCategory.MUSIC));
    }

    @Test
    void existingRandomProfilesDefaultToPerRun() {
        CosmeticProfile profile = new Gson().fromJson("{\"randomCategories\":[\"BLOCK\"],\"randomPerJumpCategories\":null}", CosmeticProfile.class);
        profile.normalize(catalog);

        assertTrue(profile.isRandom(CosmeticCategory.BLOCK));
        assertFalse(profile.isRandomPerJump(CosmeticCategory.BLOCK));
    }
}
