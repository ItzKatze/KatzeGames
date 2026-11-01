package gg.itzkatze.katze.forcebattle;

import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.configuration.states.SettingStateType;
import lombok.Getter;
import gg.itzkatze.katze.configuration.states.PlayerTeamBothState;
import gg.itzkatze.katze.challenges.ChallengeRegistry;
import gg.itzkatze.katze.configuration.ChallengeSettingsManager;
import net.kyori.adventure.text.Component;

@Getter
public enum ForceBattleType {

    FORCE_ITEM(ForceBattleSettings.FORCE_ITEM),
    FORCE_MOB(ForceBattleSettings.FORCE_MOB),
    FORCE_BIOME(ForceBattleSettings.FORCE_BIOME),
    FORCE_ADVANCEMENT(ForceBattleSettings.FORCE_ADVANCEMENT),
    FORCE_HEIGHT(ForceBattleSettings.FORCE_HEIGHT),
    FORCE_COORDS(ForceBattleSettings.FORCE_COORDS),
    FORCE_STRUCTURE(ForceBattleSettings.FORCE_STRUCTURE),
    FORCE_DEATH(ForceBattleSettings.FORCE_DEATH),
    ;

    private final ForceBattleSettings linkedSetting;

    ForceBattleType(ForceBattleSettings linkedSetting) {
        this.linkedSetting = linkedSetting;
    }

    public String getPrefix() {
        return name().substring("FORCE_".length());
    }

    public Component displayName() {
        return Component.translatable("katze.forcebattle.type." + name().toLowerCase());
    }

    public SettingStateType getSettingState() {
        Challenge forceBattle = ChallengeRegistry.getChallenge(ChallengeType.FORCE_BATTLE);
        if (forceBattle != null && forceBattle.getSettingsManager() != null) {
            ChallengeSettingsManager manager = forceBattle.getSettingsManager();
            return manager.getState(linkedSetting);
        }
        return PlayerTeamBothState.OFF;
    }
}
