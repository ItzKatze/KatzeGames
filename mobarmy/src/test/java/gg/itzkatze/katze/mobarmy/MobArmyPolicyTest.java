package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.challenges.ChallengeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyPolicyTest {
    @Test void modifiersAreAllowedOnlyDuringCollectionWithoutUnloadingTheirState() {
        var challenge = new MobArmy();
        assertNull(challenge.getScoreboardContent());
        assertTrue(challenge.allowsCombination(ChallengeType.RANDOMIZER));
        assertTrue(challenge.allowsCombination(ChallengeType.ITEM_RAIN));
        assertFalse(challenge.allowsCombination(ChallengeType.BINGO));
        assertFalse(challenge.allowsCombination(ChallengeType.FORCE_BATTLE));
        assertTrue(challenge.locksChallengeSelection());
        challenge.game.match = new MobArmyMatch();
        assertTrue(challenge.allowsGameplay(ChallengeType.RANDOMIZER));
        challenge.game.match.phase = MobArmyMatch.Phase.CONFIGURATION;
        assertFalse(challenge.allowsGameplay(ChallengeType.RANDOMIZER));
        assertFalse(challenge.allowsGameplay(ChallengeType.ITEM_RAIN));
        assertTrue(challenge.allowsGameplay(ChallengeType.MOB_ARMY));
        assertFalse(challenge.requiresCombatDamage());
        challenge.game.match.phase = MobArmyMatch.Phase.COMBAT;
        assertTrue(challenge.requiresCombatDamage());
        challenge.game.match.phase = MobArmyMatch.Phase.RESULTS;
        assertFalse(challenge.requiresCombatDamage());
    }

    @Test void settingsDefaultToNinetyMinutesWithGearAndBackpackEnabled() {
        var challenge = new MobArmy();
        assertEquals(5400, challenge.getTimerSpec().startSeconds());
        assertTrue(challenge.settings.isEnabled(MobArmySettings.STARTER_EQUIPMENT));
        assertTrue(challenge.settings.isEnabled(MobArmySettings.BACKPACK));
        assertTrue(challenge.settings.importState(MobArmySettings.COLLECTION_DURATION, "120"));
        assertEquals(7200, challenge.getTimerSpec().startSeconds());
        assertFalse(challenge.settings.importState(MobArmySettings.COLLECTION_DURATION, "0"));
        assertFalse(challenge.settings.importState(MobArmySettings.COLLECTION_DURATION, "1441"));
        assertFalse(challenge.settings.importState(MobArmySettings.COLLECTION_DURATION, "bad"));
        assertEquals(7200, challenge.getTimerSpec().startSeconds());
    }
}
