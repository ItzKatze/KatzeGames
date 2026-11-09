package gg.itzkatze.katze.randomizer;

import gg.itzkatze.katze.configuration.Setting;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.configuration.states.OnOffState;
import gg.itzkatze.katze.configuration.states.SettingStateType;
import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.util.Locale;

public class RandomizerSettings {

    public static boolean isBlockDropsEnabled() {
        return SettingsManager.isEnabled(RandomizerSetting.BLOCK_DROPS);
    }

    public static boolean isMobDropsEnabled() {
        return SettingsManager.isEnabled(RandomizerSetting.MOB_DROPS);
    }

    public static boolean isChestLootEnabled() {
        return SettingsManager.isEnabled(RandomizerSetting.CHEST_LOOT);
    }

    public static boolean isSharedMappingsEnabled() {
        return SettingsManager.isEnabled(RandomizerSetting.SHARED_MAPPINGS);
    }

    @Getter
    public enum RandomizerSetting implements Setting {
        BLOCK_DROPS(false, OnOffState.ON),
        MOB_DROPS(false, OnOffState.OFF),
        CHEST_LOOT(false, OnOffState.ON),
        SHARED_MAPPINGS(true, OnOffState.ON);

        private final boolean updateOnChange;
        private final SettingStateType defaultState;

        RandomizerSetting(boolean updateOnChange, SettingStateType defaultState) {
            this.updateOnChange = updateOnChange;
            this.defaultState = defaultState;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("katze.randomizer.setting." + name().toLowerCase(Locale.ROOT));
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
