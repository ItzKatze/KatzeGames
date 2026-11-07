package gg.itzkatze.katze.forcebattle.gui;

import gg.itzkatze.katze.text.TextArg;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.configuration.states.SettingStateType;
import gg.itzkatze.katze.configuration.Setting;
import gg.itzkatze.katze.objective.ObjectiveExclusion;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.SettingsMenu;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfigSettingsGUI extends SettingsMenu {

    private static final int[] TOGGLE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    public ConfigSettingsGUI() {
        super(Component.translatable("katze.forcebattle.gui.config_title"), 36);
    }

    static @NotNull Map<Integer, ObjectiveExclusion> layout() {
        Map<Integer, ObjectiveExclusion> layout = new LinkedHashMap<>();
        List<ObjectiveExclusion> toggleable = ObjectiveExclusion.toggleable();
        for (int i = 0; i < Math.min(toggleable.size(), TOGGLE_SLOTS.length); i++) {
            layout.put(TOGGLE_SLOTS[i], toggleable.get(i));
        }
        return layout;
    }

    static void toggle(@NotNull ObjectiveExclusion exclusion) {
        Setting setting = Objects.requireNonNull(exclusion.getSetting());
        SettingStateType nextState = SettingsManager.getSettingState(setting).next();
        SettingsManager.setState(setting, nextState);

        KatzeGames.broadcast(Component.translatable("katze.forcebattle.gui.setting_changed",
                TextArg.component("name", exclusion.getDisplayName().color(KatzeColor.LIGHT_BLUE)),
                TextArg.component("state", nextState.getDisplayName())));
    }

    private static @NotNull ItemStack createItem(@NotNull ObjectiveExclusion exclusion) {
        Setting setting = Objects.requireNonNull(exclusion.getSetting());
        List<Component> loreLines = List.of(
                Component.translatable("katze.forcebattle.gui.toggle_hint").color(KatzeColor.LIGHT_GRAY),
                exclusion.getDescription().color(KatzeColor.LIGHT_GRAY),
                Component.empty(),
                Component.translatable("katze.forcebattle.gui.config_current_status").color(KatzeColor.LIGHT_BLUE)
                        .append(Component.text(": ").color(KatzeColor.GRAY))
                        .append(SettingsManager.getSettingState(setting).getDisplayName()));

        return ItemStackCreator.getStack(
                exclusion.getDisplayName().color(KatzeColor.LIGHT_BLUE),
                exclusion.getIcon(),
                1,
                loreLines
        );
    }

    @Override
    public void setItems(KatzePlayer player) {
        fill(ItemStackCreator.fillerItem());
        setBack(new ForceBattleSettingsGUI());

        layout().forEach((slot, exclusion) -> set(new GUIClickableItem(slot) {
            @Override
            public void run(InventoryClickEvent event, KatzePlayer player) {
                toggle(exclusion);
                new ConfigSettingsGUI().open(player);
            }

            @Override
            public ItemStack getItem(KatzePlayer player) {
                return createItem(exclusion);
            }
        }));
    }

}
