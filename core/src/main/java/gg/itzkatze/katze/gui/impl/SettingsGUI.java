package gg.itzkatze.katze.gui.impl;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeRegistry;
import gg.itzkatze.katze.challenges.ChallengeSettings;
import gg.itzkatze.katze.configuration.GlobalSetting;
import gg.itzkatze.katze.configuration.Setting;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.configuration.states.SettingStateType;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.GUIMaterial;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SettingsGUI extends KatzeGUI {

    public SettingsGUI() {
        super(Component.translatable("katze.settings.gui.title"), 54);
    }

    @Getter
    public enum GenericSettingButton {
        NO_HUNGER(45, Material.COOKED_BEEF, GlobalSetting.NO_HUNGER),
        NO_DAMAGE(46, Material.IRON_SWORD, GlobalSetting.NO_DAMAGE),
        MOBILE_CRAFTING(47, Material.CRAFTING_TABLE, GlobalSetting.MOBILE_CRAFTING),
        HIDE_POINTS(48, Material.IRON_TRAPDOOR, GlobalSetting.HIDE_POINTS),
        HIDE_OBJECTIVES(49, Material.DARK_OAK_DOOR, GlobalSetting.HIDE_OBJECTIVES),
        NO_DUPLICATE_OBJECTIVES(50, Material.NAME_TAG, GlobalSetting.NO_DUPLICATE_OBJECTIVES),
        SIMPLIFIED_OBJECTIVES(51, Material.DIRT, GlobalSetting.SIMPLIFIED_OBJECTIVES),
        EXTRA_TEAM_OBJECTIVE(52, Material.ALLAY_SPAWN_EGG, GlobalSetting.EXTRA_TEAM_OBJECTIVE),
        SHOW_SCOREBOARD(53, Material.ARMOR_STAND, GlobalSetting.SHOW_SCOREBOARD);

        private final int slot;
        private final Material material;
        private final GlobalSetting setting;

        GenericSettingButton(int slot, Material material, GlobalSetting setting) {
            this.slot = slot;
            this.material = material;
            this.setting = setting;
        }
    }

    @Override
    public void setItems(KatzePlayer player) {
        fill(ItemStackCreator.fillerItem());

        List<Challenge> challenges = new ArrayList<>(ChallengeRegistry.getAllChallenges());
        int slot = 0;

        for (Challenge challenge : challenges) {
            if (slot >= 45) break;

            set(new GUIClickableItem(slot) {
                @Override
                public void run(InventoryClickEvent event, KatzePlayer player) {
                    if (event.getClick() == ClickType.RIGHT && challenge instanceof ChallengeSettings settings && settings.hasSettings()) {
                        KatzeGUI settingsGUI = settings.getSettingsGUI();
                        if (settingsGUI != null) {
                            settingsGUI.open(player);
                        }
                    } else {
                        if (ChallengeManager.isChallengeActive(challenge.getType())) {
                            ChallengeManager.unloadChallenge(challenge);
                            player.sendMessage(Component.translatable("katze.settings.gui.challenge.disabled_msg", gg.itzkatze.katze.text.TextArg.component("name", challenge.getType().displayName().color(KatzeColor.RED))));
                        } else {
                            ChallengeManager.loadChallenge(challenge);
                            player.sendMessage(Component.translatable("katze.settings.gui.challenge.enabled_msg", gg.itzkatze.katze.text.TextArg.component("name", challenge.getType().displayName().color(KatzeColor.LIME))));
                        }
                        new SettingsGUI().open(player);
                    }
                }

                @Override
                public ItemStack getItem(KatzePlayer p) {
                    boolean active = ChallengeManager.isChallengeActive(challenge.getType());
                    GUIMaterial material = challenge.getType().getMaterial();
                    Component displayName = challenge.getType().displayName();

                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.translatable("katze.settings.gui.status", gg.itzkatze.katze.text.TextArg.component("state", Component.translatable(active ? "katze.settings.gui.enabled" : "katze.settings.gui.disabled")
                            .color(active ? KatzeColor.LIME : KatzeColor.RED))));

                    if (challenge.hasSettings()) {
                        lore.add(Component.empty());
                        lore.add(Component.translatable("katze.settings.gui.left_click").color(KatzeColor.YELLOW));
                        lore.add(Component.translatable("katze.settings.gui.right_click").color(KatzeColor.YELLOW));
                    } else {
                        lore.add(Component.empty());
                        lore.add(Component.translatable("katze.settings.gui.click").color(KatzeColor.YELLOW));
                    }

                    lore.add(Component.empty());
                    lore.add(Component.translatable("katze.settings.gui.description", gg.itzkatze.katze.text.TextArg.component("desc", challenge.getType().getDescription().color(KatzeColor.WHITE))));

                    if (material.hasSkull()) {
                        return ItemStackCreator.getSkull(material.skull(), displayName, 1, lore);
                    } else {
                        return ItemStackCreator.getStack(displayName, material.material(), 1, lore);
                    }
                }
            });

            slot++;
        }

        for (GenericSettingButton button : GenericSettingButton.values()) {
            Setting setting = button.getSetting();
            Component settingName = Component.translatable("katze.settings.global." + setting.getId().toLowerCase(java.util.Locale.ROOT));

            set(new GUIClickableItem(button.getSlot()) {
                @Override
                public void run(InventoryClickEvent event, KatzePlayer player) {
                    SettingStateType nextState = SettingsManager.getState(setting).next();
                    SettingsManager.setState(setting, nextState);

                    KatzeGames.broadcast(
                            Component.translatable("katze.settings.gui.changed", gg.itzkatze.katze.text.TextArg.component("name", settingName.color(KatzeColor.BLUE)), gg.itzkatze.katze.text.TextArg.component("state", nextState.getDisplayName())));

                    new SettingsGUI().open(player);
                }

                @Override
                public ItemStack getItem(KatzePlayer p) {
                    List<Component> loreLines = new ArrayList<>();
                    loreLines.add(Component.translatable("katze.settings.gui.toggle_hint").color(KatzeColor.LIGHT_GRAY));
                    loreLines.add(Component.empty());
                    loreLines.add(Component.translatable("katze.settings.gui.current_status", gg.itzkatze.katze.text.TextArg.component("state", SettingsManager.getState(setting).getDisplayName())));

                    return ItemStackCreator.getStack(
                            settingName.color(KatzeColor.BLUE),
                            button.getMaterial(),
                            1,
                            loreLines
                    );
                }
            });
        }
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }
}
