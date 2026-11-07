package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeRegistry;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.configuration.GlobalSetting;
import gg.itzkatze.katze.configuration.SettingsManager;
import org.junit.jupiter.api.Test;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BingoSettingsTest {
    @Test void inactiveBingoSettingsCanBeDisplayedAndChangedWithoutAffectingGlobalSettings() {
        var manager = ChallengeRegistry.getChallenge(ChallengeType.BINGO).getSettingsManager();
        assertNotNull(manager);
        var original = manager.exportStates();
        var global = SettingsManager.getState(GlobalSetting.SHOW_SCOREBOARD);
        try {
            assertFalse(ChallengeManager.isChallengeActive(ChallengeType.BINGO));
            for (BingoSettings.Option option : BingoSettings.Option.values()) {
                var state = SettingsManager.getState(option);
                assertNotNull(state);
                assertNotNull(state.getDisplayName());
                assertSame(manager.getState(option), state);
                var next = state.next();
                SettingsManager.setState(option, next);
                assertSame(next, SettingsManager.getState(option));
            }
            assertSame(global, SettingsManager.getState(GlobalSetting.SHOW_SCOREBOARD));
        } finally {
            manager.importStates(original);
        }
    }

    @Test void bingoExclusionsHaveTheSameDefaultsButDoNotChangeForceBattleSettings() {
        var bingo = ChallengeRegistry.getChallenge(ChallengeType.BINGO).getSettingsManager();
        var forceBattle = ChallengeRegistry.getChallenge(ChallengeType.FORCE_BATTLE).getSettingsManager();
        var original = bingo.exportStates();
        var forceOriginal = forceBattle.exportStates();
        try {
            for (BingoSettings.Option option : BingoSettings.Option.values()) if (option.name().startsWith("EXCLUDE_")) {
                var equivalent = Arrays.stream(forceBattle.getAllSettings()).filter(setting -> setting.getId().equals(option.getId())).findFirst().orElseThrow();
                assertEquals(equivalent.getDefaultState(), option.getDefaultState());
                SettingsManager.setState(option, SettingsManager.getState(option).next());
                assertEquals(forceOriginal, forceBattle.exportStates());
            }
        } finally { bingo.importStates(original); }
    }
}
