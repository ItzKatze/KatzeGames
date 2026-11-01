package gg.itzkatze.katze.allitems;

import gg.itzkatze.katze.configuration.Setting;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.configuration.states.OnOffState;
import gg.itzkatze.katze.configuration.states.SettingStateType;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import lombok.Setter;

public class AllItemsSettings {

    public enum GameMode {
        TEAM,
        INDIVIDUAL
    }

    @Setter
    @Getter
    private static GameMode gameMode = GameMode.TEAM;

    public static boolean isTeamMode() {
        return gameMode == GameMode.TEAM;
    }

    public static boolean isIndividualMode() {
        return gameMode == GameMode.INDIVIDUAL;
    }

    public static boolean isItemsEnabled() {
        return SettingsManager.isEnabled(AllItemsSetting.ITEMS);
    }

    public static boolean isBiomesEnabled() {
        return SettingsManager.isEnabled(AllItemsSetting.BIOMES);
    }

    public static boolean isMobsEnabled() {
        return SettingsManager.isEnabled(AllItemsSetting.MOBS);
    }

    public static boolean isStructuresEnabled() {
        return SettingsManager.isEnabled(AllItemsSetting.STRUCTURES);
    }

    public static boolean isAdvancementsEnabled() {
        return SettingsManager.isEnabled(AllItemsSetting.ADVANCEMENTS);
    }

    public static boolean isChainModeEnabled() {
        return SettingsManager.isEnabled(AllItemsSetting.CHAIN_MODE);
    }

    @Getter
    public enum AllItemsSetting implements Setting {
        ITEMS(false, OnOffState.ON),
        BIOMES(false, OnOffState.OFF),
        MOBS(false, OnOffState.OFF),
        STRUCTURES(false, OnOffState.OFF),
        ADVANCEMENTS(false, OnOffState.OFF),
        CHAIN_MODE(false, OnOffState.OFF);

        private final boolean updateOnChange;
        private final SettingStateType defaultState;

        AllItemsSetting(boolean updateOnChange, SettingStateType defaultState) {
            this.updateOnChange = updateOnChange;
            this.defaultState = defaultState;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("katze.allitems.setting." + name().toLowerCase(java.util.Locale.ROOT));
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
}
