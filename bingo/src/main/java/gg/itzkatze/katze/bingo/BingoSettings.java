package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.bingo.board.BingoObjective;
import gg.itzkatze.katze.configuration.Setting;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.configuration.GlobalSetting;
import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.objective.ObjectiveExclusion;
import gg.itzkatze.katze.objective.ObjectiveFilter;
import gg.itzkatze.katze.configuration.states.OnOffState;
import gg.itzkatze.katze.configuration.states.SettingStateType;
import net.kyori.adventure.text.Component;

import java.util.Locale;
import java.util.Arrays;

public final class BingoSettings {
    private BingoSettings() {}

    public enum Option implements Setting {
        DURATION(Duration.MIN_45), ALLOW_UNEQUAL_TEAMS(OnOffState.OFF),
        ITEM(OnOffState.ON), MOB(OnOffState.ON), BIOME(OnOffState.ON), STRUCTURE(OnOffState.ON), ADVANCEMENT(OnOffState.ON),
        EXCLUDE_MUSIC_DISCS(OnOffState.ON), EXCLUDE_BANNER_PATTERNS(OnOffState.ON), EXCLUDE_ARMOR_TEMPLATES(OnOffState.ON),
        EXCLUDE_POTTERY_SHERDS(OnOffState.ON), EXCLUDE_ORES(OnOffState.ON), EXCLUDE_END(OnOffState.ON), EXCLUDE_TRIAL_ITEMS(OnOffState.ON),
        EXCLUDE_OXIDIZED_COPPER(OnOffState.ON), EXCLUDE_HEADS(OnOffState.ON), EXCLUDE_SNIFFER(OnOffState.ON),
        EXCLUDE_UNREALISTIC(OnOffState.ON), EXCLUDE_MAPS(OnOffState.ON);

        private final SettingStateType defaultState;
        Option(SettingStateType defaultState) { this.defaultState = defaultState; }
        @Override public String getId() { return name(); }
        @Override public SettingStateType getDefaultState() { return defaultState; }
        @Override public boolean shouldUpdateOnChange() { return false; }
        @Override public Component getDisplayName() {
            if (name().startsWith("EXCLUDE_")) return Arrays.stream(ObjectiveExclusion.values()).filter(group -> name().equals(group.getSettingId())).findFirst().orElseThrow().getDisplayName();
            return Component.translatable("katze.bingo.setting." + name().toLowerCase(Locale.ROOT));
        }
    }

    public enum Duration implements SettingStateType {
        MIN_30(1800), MIN_45(2700), MIN_60(3600), UNLIMITED(-1);
        private final int seconds;
        Duration(int seconds) { this.seconds = seconds; }
        public int seconds() { return seconds; }
        @Override public Component getDisplayName() { return Component.translatable("katze.bingo.duration." + name().toLowerCase(Locale.ROOT)); }
        @Override public SettingStateType next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public static boolean categoryEnabled(BingoObjective.Category category) { return SettingsManager.isEnabled(Option.valueOf(category.name())); }
    public static boolean unequalTeams() { return SettingsManager.isEnabled(Option.ALLOW_UNEQUAL_TEAMS); }
    public static boolean simplified() { return SettingsManager.isEnabled(GlobalSetting.SIMPLIFIED_OBJECTIVES); }
    public static ObjectiveFilter filter() {
        return ObjectiveFilter.of(Arrays.stream(ObjectiveExclusion.values())
                .filter(group -> group.getSettingId() == null || SettingsManager.isEnabled(Option.valueOf(group.getSettingId()))).toList(),
                KatzeGames.get().getConfig().getConfigurationSection("exclude"));
    }
    public static int seconds() {
        SettingStateType state = SettingsManager.getSettingState(Option.DURATION);
        return state instanceof Duration duration ? duration.seconds() : 2700;
    }
}
