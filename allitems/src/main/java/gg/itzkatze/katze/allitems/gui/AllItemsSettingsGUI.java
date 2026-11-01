package gg.itzkatze.katze.allitems.gui;

import gg.itzkatze.katze.allitems.AllItems;
import gg.itzkatze.katze.allitems.AllItemsSettings;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.configuration.states.SettingStateType;
import gg.itzkatze.katze.gui.impl.SettingsGUI;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AllItemsSettingsGUI extends KatzeGUI {

    public AllItemsSettingsGUI() {
        super(Component.translatable("katze.allitems.settings.title"), 45);
    }

    private enum SettingButton {
        MODE(10, Material.PLAYER_HEAD, "katze.allitems.mode.title", AllItemsSettingsGUI::toggleGameMode, null),
        ITEMS(12, Material.CHEST, "katze.allitems.category.items", null, AllItemsSettings.AllItemsSetting.ITEMS),
        BIOMES(13, Material.GRASS_BLOCK, "katze.allitems.category.biomes", null, AllItemsSettings.AllItemsSetting.BIOMES),
        MOBS(14, Material.ZOMBIE_SPAWN_EGG, "katze.allitems.category.mobs", null, AllItemsSettings.AllItemsSetting.MOBS),
        STRUCTURES(15, Material.STRUCTURE_BLOCK, "katze.allitems.category.structures", null, AllItemsSettings.AllItemsSetting.STRUCTURES),
        ADVANCEMENTS(16, Material.BOOK, "katze.allitems.category.advancements", null, AllItemsSettings.AllItemsSetting.ADVANCEMENTS),
        CHAIN_MODE(28, Material.IRON_CHAIN, "katze.allitems.setting.chain_mode", null, AllItemsSettings.AllItemsSetting.CHAIN_MODE),
        MISSING(30, Material.MAP, "katze.allitems.menu.missing", AllItemsSettingsGUI::openMissing, null),
        LEADERBOARD(32, Material.NAME_TAG, "katze.allitems.menu.leaderboard", AllItemsSettingsGUI::openLeaderboard, null);

        private final int slot;
        private final Material material;
        private final String label;
        private final ClickHandler handler;
        private final AllItemsSettings.AllItemsSetting setting;

        SettingButton(int slot, Material material, String label, ClickHandler handler,
                      AllItemsSettings.AllItemsSetting setting) {
            this.slot = slot;
            this.material = material;
            this.label = label;
            this.handler = handler;
            this.setting = setting;
        }
    }

    @FunctionalInterface
    private interface ClickHandler {
        void onClick(AllItemsSettingsGUI gui, KatzePlayer player);
    }

    @Override
    public void setItems(KatzePlayer player) {
        fill(ItemStackCreator.fillerItem());

        for (SettingButton button : SettingButton.values()) {
            set(new GUIClickableItem(button.slot) {
                @Override
                public void run(InventoryClickEvent event, KatzePlayer player) {
                    if (button.handler != null) {
                        button.handler.onClick(AllItemsSettingsGUI.this, player);
                        return;
                    }
                    if (button.setting != null) {
                        SettingStateType state = SettingsManager.getSettingState(button.setting);
                        if (state != null) {
                            SettingsManager.setState(button.setting, state.next());
                        }
                        if (button.setting == AllItemsSettings.AllItemsSetting.CHAIN_MODE) {
                            var data = AllItems.getData();
                            data.resetChains();
                        }
                        new AllItemsSettingsGUI().open(player);
                    }
                }

                @Override
                public ItemStack getItem(KatzePlayer p) {
                    return buildButtonItem(button);
                }
            });
        }

        // Go back button
        set(GUIClickableItem.getGoBackItem(36, new SettingsGUI()));
    }

    private static void toggleGameMode(AllItemsSettingsGUI gui, KatzePlayer player) {
        AllItemsSettings.GameMode currentMode = AllItemsSettings.getGameMode();
        AllItemsSettings.GameMode newMode = currentMode == AllItemsSettings.GameMode.TEAM
            ? AllItemsSettings.GameMode.INDIVIDUAL
            : AllItemsSettings.GameMode.TEAM;

        AllItemsSettings.setGameMode(newMode);

        Component modeName = Component.translatable(newMode == AllItemsSettings.GameMode.TEAM ? "katze.allitems.mode.team" : "katze.allitems.mode.individual");
        player.sendMessage(Component.translatable("katze.allitems.mode.changed", gg.itzkatze.katze.text.TextArg.component("mode", modeName)).color(KatzeColor.LIME));

        // Refresh the GUI
        gui.open(player);
    }

    private static void openMissing(AllItemsSettingsGUI gui, KatzePlayer player) {
        new AllItemsMissingObjectivesMenu().open(player);
    }

    private static void openLeaderboard(AllItemsSettingsGUI gui, KatzePlayer player) {
        new AllItemsLeaderboardGUI().open(player);
    }

    private ItemStack buildButtonItem(SettingButton button) {
        if (button == SettingButton.MODE) {
            return buildModeItem(button);
        }
        if (button == SettingButton.MISSING) {
            return buildMissingItem(button);
        }
        if (button == SettingButton.LEADERBOARD) {
            return buildLeaderboardItem(button);
        }
        if (button.setting != null) {
            return buildToggleItem(button);
        }
        return buildFallbackItem(button);
    }

    private ItemStack buildModeItem(SettingButton button) {
        boolean isTeamMode = AllItemsSettings.isTeamMode();
        Component modeName = Component.translatable(isTeamMode ? "katze.allitems.mode.team" : "katze.allitems.mode.individual");
        TextColor color = isTeamMode ? KatzeColor.LIME : KatzeColor.LIGHT_BLUE;

        return ItemStackCreator.getStack(
                modeName.color(color),
                button.material,
                1,
                List.of(
                        Component.translatable("katze.allitems.gui.current", gg.itzkatze.katze.text.TextArg.component("state", modeName.color(color))),
                        Component.empty(),
                        Component.translatable("katze.allitems.gui.toggle").color(KatzeColor.YELLOW),
                        Component.empty(),
                        Component.translatable("katze.allitems.mode.team.description"),
                        Component.translatable("katze.allitems.mode.individual.description")
                )
        );
    }

    private ItemStack buildMissingItem(SettingButton button) {
        return ItemStackCreator.getStack(
                Component.translatable(button.label).color(KatzeColor.LIGHT_BLUE),
                button.material,
                1,
                List.of(
                        Component.translatable("katze.allitems.menu.missing.description").color(KatzeColor.LIGHT_GRAY),
                        Component.empty(),
                        Component.translatable("katze.allitems.gui.open").color(KatzeColor.YELLOW)
                )
        );
    }

    private ItemStack buildLeaderboardItem(SettingButton button) {
        return ItemStackCreator.getStack(
                Component.translatable(button.label).color(KatzeColor.GOLD),
                button.material,
                1,
                List.of(
                        Component.translatable("katze.allitems.menu.leaderboard.description").color(KatzeColor.LIGHT_GRAY),
                        Component.empty(),
                        Component.translatable("katze.allitems.gui.open").color(KatzeColor.YELLOW)
                )
        );
    }

    private ItemStack buildToggleItem(SettingButton button) {
        SettingStateType state = SettingsManager.getSettingState(button.setting);
        Component status = state == null ? Component.translatable("katze.allitems.gui.unknown") : state.getDisplayName();
        return ItemStackCreator.getStack(
                Component.translatable(button.label).color(KatzeColor.BLUE),
                button.material,
                1,
                List.of(
                        Component.translatable("katze.allitems.gui.current", gg.itzkatze.katze.text.TextArg.component("state", status)),
                        Component.empty(),
                        Component.translatable("katze.allitems.gui.toggle").color(KatzeColor.YELLOW)
                )
        );
    }

    private ItemStack buildFallbackItem(SettingButton button) {
        return ItemStackCreator.createNamedItemStack(
                button.material,
                Component.translatable(button.label).color(KatzeColor.RED)
        );
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }
}
