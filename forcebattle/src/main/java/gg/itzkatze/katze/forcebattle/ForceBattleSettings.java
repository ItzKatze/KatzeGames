package gg.itzkatze.katze.forcebattle;

import gg.itzkatze.katze.configuration.Setting;
import gg.itzkatze.katze.configuration.states.OnOffState;
import gg.itzkatze.katze.configuration.states.PlayerTeamBothState;
import gg.itzkatze.katze.configuration.states.SettingStateType;
import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.util.Locale;

@Getter
public enum ForceBattleSettings implements Setting {
    FORCE_ITEM(true, PlayerTeamBothState.BOTH),
    FORCE_MOB(true, PlayerTeamBothState.TEAM),
    FORCE_BIOME(true, PlayerTeamBothState.TEAM),
    FORCE_ADVANCEMENT(true, PlayerTeamBothState.TEAM),
    FORCE_HEIGHT(true, PlayerTeamBothState.TEAM),
    FORCE_COORDS(true, PlayerTeamBothState.TEAM),
    FORCE_STRUCTURE(true, PlayerTeamBothState.TEAM),
    FORCE_DEATH(true, PlayerTeamBothState.TEAM),

    BACKPACK(false, OnOffState.ON),
    CHAIN_MODE(true, OnOffState.OFF),

    EXCLUDE_MUSIC_DISCS(true, OnOffState.ON),
    EXCLUDE_BANNER_PATTERNS(true, OnOffState.ON),
    EXCLUDE_ARMOR_TEMPLATES(true, OnOffState.ON),
    EXCLUDE_POTTERY_SHERDS(true, OnOffState.ON),
    EXCLUDE_ORES(true, OnOffState.ON),
    EXCLUDE_END(true, OnOffState.ON),
    EXCLUDE_TRIAL_ITEMS(true, OnOffState.ON),
    EXCLUDE_OXIDIZED_COPPER(true, OnOffState.ON),
    EXCLUDE_HEADS(true, OnOffState.ON),
    EXCLUDE_SNIFFER(true, OnOffState.ON),
    EXCLUDE_UNREALISTIC(true, OnOffState.ON),
    EXCLUDE_MAPS(true, OnOffState.ON);

    private final boolean updateOnChange;
    private final SettingStateType defaultState;

    ForceBattleSettings(boolean updateOnChange, SettingStateType defaultState) {
        this.updateOnChange = updateOnChange;
        this.defaultState = defaultState;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("katze.forcebattle.setting." + name().toLowerCase(Locale.ROOT));
    }

    @Override
    public String getId() {
        return name();
    }

    @Override
    public boolean shouldUpdateOnChange() {
        return updateOnChange;
    }

    @Override
    public SettingStateType getDefaultState() {
        return defaultState;
    }
}
