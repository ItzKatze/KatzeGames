package gg.itzkatze.katze.configuration;

import gg.itzkatze.katze.configuration.states.OnOffState;
import gg.itzkatze.katze.configuration.states.SettingStateType;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@Getter
public enum GlobalSetting implements Setting {
    NO_HUNGER(false, OnOffState.OFF),
    NO_DAMAGE(false, OnOffState.OFF),
    MOBILE_CRAFTING(false, OnOffState.OFF),
    HIDE_POINTS(false, OnOffState.ON),
    HIDE_OBJECTIVES(false, OnOffState.OFF),
    NO_DUPLICATE_OBJECTIVES(true, OnOffState.ON),
    SIMPLIFIED_OBJECTIVES(true, OnOffState.ON),
    EXTRA_TEAM_OBJECTIVE(true, OnOffState.ON),
    SHOW_SCOREBOARD(false, OnOffState.ON);

    private final boolean shouldUpdateOnChange;
    private final SettingStateType defaultState;

    GlobalSetting(boolean shouldUpdateOnChange, SettingStateType defaultState) {
        this.shouldUpdateOnChange = shouldUpdateOnChange;
        this.defaultState = defaultState;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("katze.settings.global." + name().toLowerCase(Locale.ROOT));
    }

    @Override
    @NotNull public String getId() {
        return name();
    }

    @Override
    public boolean shouldUpdateOnChange() {
        return shouldUpdateOnChange;
    }

    @Override
    public SettingStateType getDefaultState() {
        return defaultState;
    }
}
