package gg.itzkatze.katze.forcebattle.gui;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeRegistry;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.configuration.ChallengeSettingsManager;
import gg.itzkatze.katze.configuration.Setting;
import gg.itzkatze.katze.configuration.states.PlayerTeamBothState;
import gg.itzkatze.katze.configuration.states.SettingStateType;
import gg.itzkatze.katze.forcebattle.ForceBattle;
import gg.itzkatze.katze.forcebattle.ForceBattleSettings;
import gg.itzkatze.katze.game.GameLifecycleManager;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.gui.sign.NumberSignInput;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ForceBattleSettingsGUI extends KatzeGUI {

    public ForceBattleSettingsGUI() {
        super(Component.translatable("katze.forcebattle.gui.settings_title"), 27);
    }

    @Getter
    public enum SettingButton {
        FORCE_ITEM(0, Material.ITEM_FRAME, ForceBattleSettings.FORCE_ITEM, true),
        FORCE_MOB(1, Material.DIAMOND_SWORD, ForceBattleSettings.FORCE_MOB, true),
        FORCE_BIOME(2, Material.GRASS_BLOCK, ForceBattleSettings.FORCE_BIOME, true),
        FORCE_ADVANCEMENT(3, Material.BREWING_STAND, ForceBattleSettings.FORCE_ADVANCEMENT, true),
        FORCE_HEIGHT(4, Material.SCAFFOLDING, ForceBattleSettings.FORCE_HEIGHT, true),
        FORCE_COORDS(5, Material.FILLED_MAP, ForceBattleSettings.FORCE_COORDS, true),
        FORCE_STRUCTURE(6, Material.STRUCTURE_BLOCK, ForceBattleSettings.FORCE_STRUCTURE, true),
        FORCE_DEATH(7, Material.SKELETON_SKULL, ForceBattleSettings.FORCE_DEATH, true),

        BATTLE_DURATION(18, Material.CLOCK, null, false),
        CHAIN_MODE(19, Material.IRON_CHAIN, ForceBattleSettings.CHAIN_MODE, false),
        BACKPACK(20, Material.CHEST, ForceBattleSettings.BACKPACK, false),

        RESET(26, Material.BARRIER, null, false);

        private final int slot;
        private final Material material;
        private final Setting setting;
        private final boolean multiState;

        SettingButton(int slot, Material material, Setting setting, boolean multiState) {
            this.slot = slot;
            this.material = material;
            this.setting = setting;
            this.multiState = multiState;
        }

        public ItemStack createItem() {
            if (this == BATTLE_DURATION) {
                int minutes = KatzeGames.get().getConfig().getInt(
                        ForceBattle.BATTLE_DURATION_MINUTES_KEY,
                        ForceBattle.DEFAULT_BATTLE_DURATION_MINUTES
                );
                List<Component> loreLines = new ArrayList<>();
                loreLines.add(Component.translatable("katze.forcebattle.gui.button." + name().toLowerCase(Locale.ROOT) + ".hint").color(KatzeColor.LIGHT_GRAY));
                loreLines.add(Component.empty());
                loreLines.add(Component.translatable("katze.forcebattle.gui.current", TextArg.component("state",
                        Component.translatable("katze.forcebattle.duration_minutes", TextArg.number("minutes", minutes)).color(KatzeColor.LIGHT_BLUE))));
                return ItemStackCreator.getStack(
                        Component.translatable("katze.forcebattle.gui.button." + name().toLowerCase(Locale.ROOT)).color(KatzeColor.BLUE),
                        material,
                        1,
                        loreLines
                );
            }

            if (setting == null) {
                return ItemStackCreator.createNamedItemStack(
                        material,
                        Component.translatable("katze.forcebattle.gui.button." + name().toLowerCase(Locale.ROOT)).color(KatzeColor.RED)
                );
            }

            List<Component> loreLines = new ArrayList<>();
            loreLines.add(Component.translatable("katze.forcebattle.gui.button." + name().toLowerCase(Locale.ROOT) + ".hint").color(KatzeColor.LIGHT_GRAY));
            loreLines.add(Component.empty());
            loreLines.add(Component.translatable("katze.forcebattle.gui.current_status",
                    TextArg.component("state", getSettingState().getDisplayName())));

            return ItemStackCreator.getStack(
                    Component.translatable("katze.forcebattle.gui.button." + name().toLowerCase(Locale.ROOT)).color(KatzeColor.BLUE),
                    material,
                    1,
                    loreLines
            );
        }

        private SettingStateType getSettingState() {
            if (setting instanceof ForceBattleSettings fbSetting) {
                Challenge forceBattle = ChallengeRegistry.getChallenge(ChallengeType.FORCE_BATTLE);
                if (forceBattle != null && forceBattle.getSettingsManager() != null) {
                    ChallengeSettingsManager manager = forceBattle.getSettingsManager();
                    return manager.getState(fbSetting);
                }
            }
            return PlayerTeamBothState.OFF;
        }

        public void handleClick(KatzePlayer player) {
            if (this == RESET) {
                GameLifecycleManager.resetGame();
                return;
            }

            if (this == BATTLE_DURATION) {
                int current = KatzeGames.get().getConfig().getInt(
                        ForceBattle.BATTLE_DURATION_MINUTES_KEY,
                        ForceBattle.DEFAULT_BATTLE_DURATION_MINUTES
                );
                NumberSignInput.open(
                        player,
                        Component.translatable("katze.forcebattle.gui.duration_prompt"),
                        current,
                        1,
                        1440,
                        (katzePlayer, value) -> {
                            KatzeGames.get().getConfig().set(ForceBattle.BATTLE_DURATION_MINUTES_KEY, value);
                            KatzeGames.get().saveConfig();
                            KatzeGames.broadcast(Component.translatable("katze.forcebattle.gui.duration_changed", TextArg.component("duration",
                                    Component.translatable("katze.forcebattle.duration_minutes", TextArg.number("minutes", value)).color(KatzeColor.LIGHT_BLUE))));
                            new ForceBattleSettingsGUI().open(katzePlayer);
                        },
                        (katzePlayer, error) -> {
                            katzePlayer.sendMessage(error.color(KatzeColor.RED));
                            new ForceBattleSettingsGUI().open(katzePlayer);
                        }
                );
                return;
            }

            if (setting == null) return;

            SettingStateType next = getSettingState().next();

            if (setting instanceof ForceBattleSettings fbSetting) {
                Challenge forceBattle = ChallengeRegistry.getChallenge(ChallengeType.FORCE_BATTLE);
                if (forceBattle != null && forceBattle.getSettingsManager() != null) {
                    ChallengeSettingsManager manager = forceBattle.getSettingsManager();
                    manager.setState(fbSetting, next);
                }
            }

            KatzeGames.broadcast(Component.translatable(
                    "katze.forcebattle.gui.setting_changed",
                    TextArg.component("name", Component.translatable("katze.forcebattle.gui.button." + name().toLowerCase(Locale.ROOT)).color(KatzeColor.BLUE)),
                    TextArg.component("state", getSettingState().getDisplayName())
            ));

            new ForceBattleSettingsGUI().open(player);
        }
    }

    @Override
    public void setItems(KatzePlayer player) {
        fill(ItemStackCreator.fillerItem());
        set(GUIClickableItem.getGoForthItem(8, new ConfigSettingsGUI()));

        for (SettingButton button : SettingButton.values()) {
            set(new GUIClickableItem(button.getSlot()) {
                @Override
                public void run(final InventoryClickEvent event, final KatzePlayer player) {
                    button.handleClick(player);
                }

                @Override
                public ItemStack getItem(KatzePlayer p) {
                    return button.createItem();
                }
            });
        }
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }
}
