package gg.itzkatze.katze.configuration;

import gg.itzkatze.katze.configuration.states.SettingStateType;
import net.kyori.adventure.text.Component;

public interface Setting {

    String getId();

    Component getDisplayName();

    boolean shouldUpdateOnChange();

    SettingStateType getDefaultState();
}
