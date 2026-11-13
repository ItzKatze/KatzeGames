package gg.itzkatze.katze.randomizer.gui;

import java.util.ArrayList;

import gg.itzkatze.katze.text.TextArg;

import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeRegistry;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.configuration.ChallengeSettingsManager;
import gg.itzkatze.katze.configuration.states.SettingStateType;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.SettingsMenu;
import gg.itzkatze.katze.gui.impl.SettingsGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.randomizer.RandomizerSettings;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RandomizerSettingsGUI extends SettingsMenu {

    public RandomizerSettingsGUI() {
        super(Component.translatable("katze.randomizer.gui.title"), 27);
    }

    private enum SettingButton {
        BLOCK_DROPS(10, Material.GRASS_BLOCK, "katze.randomizer.setting.block_drops", RandomizerSettings.RandomizerSetting.BLOCK_DROPS,
                List.of(Component.translatable("katze.randomizer.gui.block_drops.description").color(KatzeColor.LIGHT_GRAY))),
        MOB_DROPS(11, Material.ROTTEN_FLESH, "katze.randomizer.setting.mob_drops", RandomizerSettings.RandomizerSetting.MOB_DROPS,
                List.of(Component.translatable("katze.randomizer.gui.mob_drops.description").color(KatzeColor.LIGHT_GRAY))),
        CHEST_LOOT(13, Material.CHEST, "katze.randomizer.setting.chest_loot", RandomizerSettings.RandomizerSetting.CHEST_LOOT,
                List.of(Component.translatable("katze.randomizer.gui.chest_loot.description").color(KatzeColor.LIGHT_GRAY))),
        SHARED_MAPPINGS(15, Material.PLAYER_HEAD, "katze.randomizer.setting.shared_mappings", RandomizerSettings.RandomizerSetting.SHARED_MAPPINGS,
                List.of(
                        Component.translatable("katze.randomizer.gui.shared_mappings.on").color(KatzeColor.LIGHT_GRAY),
                        Component.translatable("katze.randomizer.gui.shared_mappings.off").color(KatzeColor.LIGHT_GRAY)
                )),
        TEAM_MAPPINGS(16, Material.WHITE_BANNER, "katze.randomizer.setting.team_mappings", RandomizerSettings.RandomizerSetting.TEAM_MAPPINGS,
                List.of(
                        Component.translatable("katze.randomizer.gui.team_mappings.description").color(KatzeColor.LIGHT_GRAY),
                        Component.translatable("katze.randomizer.gui.team_mappings.solo").color(KatzeColor.LIGHT_GRAY)
                ));

        private final int slot;
        private final Material material;
        private final String label;
        private final RandomizerSettings.RandomizerSetting setting;
        private final List<Component> description;

        SettingButton(int slot, Material material, String label, RandomizerSettings.RandomizerSetting setting,
                      List<Component> description) {
            this.slot = slot;
            this.material = material;
            this.label = label;
            this.setting = setting;
            this.description = description;
        }
    }

    @Override
    public void setItems(KatzePlayer player) {
        fill(ItemStackCreator.fillerItem());

        for (SettingButton button : SettingButton.values()) {
            set(new GUIClickableItem(button.slot) {
                @Override
                public void run(InventoryClickEvent event, KatzePlayer player) {
                    SettingStateType state = getState(button.setting);
                    if (state != null) {
                        updateSetting(button.setting, state.next());
                    }
                    new RandomizerSettingsGUI().open(player);
                }

                @Override
                public ItemStack getItem(KatzePlayer p) {
                    SettingStateType state = getState(button.setting);
                    Component status = state == null ? Component.translatable("katze.randomizer.gui.unknown") : state.getDisplayName();
                    List<Component> lore = new ArrayList<>(button.description);
                    lore.add(Component.empty());
                    lore.add(Component.translatable("katze.randomizer.gui.current", TextArg.component("state", status)));
                    lore.add(Component.empty());
                    lore.add(Component.translatable("katze.randomizer.gui.toggle").color(KatzeColor.LIGHT_GRAY));
                    return ItemStackCreator.getStack(
                            Component.translatable(button.label).color(KatzeColor.LIGHT_BLUE),
                            button.material,
                            1,
                            lore
                    );
                }
            });
        }

        setBack(new SettingsGUI());
    }

    private SettingStateType getState(RandomizerSettings.RandomizerSetting setting) {
        Challenge challenge = ChallengeRegistry.getChallenge(ChallengeType.RANDOMIZER);
        if (challenge == null || challenge.getSettingsManager() == null) return null;
        return challenge.getSettingsManager().getState(setting);
    }

    private void updateSetting(RandomizerSettings.RandomizerSetting setting, SettingStateType state) {
        Challenge challenge = ChallengeRegistry.getChallenge(ChallengeType.RANDOMIZER);
        if (challenge == null || challenge.getSettingsManager() == null) return;
        ChallengeSettingsManager manager = challenge.getSettingsManager();
        manager.setState(setting, state);
    }
}
