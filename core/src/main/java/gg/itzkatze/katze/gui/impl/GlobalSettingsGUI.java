package gg.itzkatze.katze.gui.impl;

import java.util.Locale;

import gg.itzkatze.katze.text.TextArg;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.configuration.GlobalSetting;
import gg.itzkatze.katze.configuration.Setting;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.configuration.states.SettingStateType;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.SettingsMenu;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;



public final class GlobalSettingsGUI extends SettingsMenu {
    @Getter
    public enum GenericSettingButton {
        NO_HUNGER(10, Material.COOKED_BEEF, GlobalSetting.NO_HUNGER),
        NO_DAMAGE(11, Material.IRON_SWORD, GlobalSetting.NO_DAMAGE),
        MOBILE_CRAFTING(12, Material.CRAFTING_TABLE, GlobalSetting.MOBILE_CRAFTING),
        HIDE_POINTS(13, Material.IRON_TRAPDOOR, GlobalSetting.HIDE_POINTS),
        HIDE_OBJECTIVES(14, Material.DARK_OAK_DOOR, GlobalSetting.HIDE_OBJECTIVES),
        NO_DUPLICATE_OBJECTIVES(15, Material.NAME_TAG, GlobalSetting.NO_DUPLICATE_OBJECTIVES),
        SIMPLIFIED_OBJECTIVES(16, Material.DIRT, GlobalSetting.SIMPLIFIED_OBJECTIVES),
        EXTRA_TEAM_OBJECTIVE(19, Material.ALLAY_SPAWN_EGG, GlobalSetting.EXTRA_TEAM_OBJECTIVE),
        SHOW_SCOREBOARD(20, Material.ARMOR_STAND, GlobalSetting.SHOW_SCOREBOARD);

        private final int slot;
        private final Material material;
        private final GlobalSetting setting;

        GenericSettingButton(int slot, Material material, GlobalSetting setting) {
            this.slot = slot;
            this.material = material;
            this.setting = setting;
        }
    }


    public GlobalSettingsGUI() { super(Component.translatable("katze.settings.gui.global_title"), 36); }
    @Override public void setItems(KatzePlayer player) {
        fill(ItemStackCreator.fillerItem());
        for (GenericSettingButton button : GenericSettingButton.values()) {
            Setting setting = button.getSetting();
            Component settingName = Component.translatable("katze.settings.global." + setting.getId().toLowerCase(Locale.ROOT));

            set(new GUIClickableItem(button.getSlot()) {
                @Override
                public void run(InventoryClickEvent event, KatzePlayer player) {
                    SettingStateType nextState = SettingsManager.getState(setting).next();
                    SettingsManager.setState(setting, nextState);

                    KatzeGames.broadcast(
                            Component.translatable("katze.settings.gui.changed", TextArg.component("name", settingName.color(KatzeColor.LIGHT_BLUE)), TextArg.component("state", nextState.getDisplayName())));

                    new GlobalSettingsGUI().open(player);
                }

                @Override
                public ItemStack getItem(KatzePlayer p) {
                    List<Component> loreLines = new ArrayList<>();
                    loreLines.add(Component.translatable("katze.settings.gui.toggle_hint").color(KatzeColor.LIGHT_GRAY));
                    loreLines.add(Component.empty());
                    loreLines.add(Component.translatable("katze.settings.gui.current_status", TextArg.component("state", SettingsManager.getState(setting).getDisplayName())));

                    return ItemStackCreator.getStack(
                            settingName.color(KatzeColor.LIGHT_BLUE),
                            button.getMaterial(),
                            1,
                            loreLines
                    );
                }
            });
        }
        setBack(new SettingsGUI());
    }
}
