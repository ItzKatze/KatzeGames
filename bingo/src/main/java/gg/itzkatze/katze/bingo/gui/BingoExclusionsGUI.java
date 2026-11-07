package gg.itzkatze.katze.bingo.gui;

import gg.itzkatze.katze.bingo.BingoSettings;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.SettingsMenu;
import gg.itzkatze.katze.objective.ObjectiveExclusion;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class BingoExclusionsGUI extends SettingsMenu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23};

    public BingoExclusionsGUI() { super(Component.translatable("katze.bingo.settings.exclusions"), 36); }

    @Override public void setItems(KatzePlayer player) {
        fill(ItemStackCreator.fillerItem());
        List<ObjectiveExclusion> groups = ObjectiveExclusion.toggleable();
        for (int index = 0; index < groups.size(); index++) {
            ObjectiveExclusion group = groups.get(index);
            BingoSettings.Option setting = BingoSettings.Option.valueOf(group.getSettingId());
            set(new GUIClickableItem(SLOTS[index]) {
                @Override public void run(InventoryClickEvent event, KatzePlayer viewer) {
                    if (ChallengeManager.isGameStarted()) { viewer.sendMessage(Component.translatable("katze.bingo.error.locked")); return; }
                    SettingsManager.setState(setting, SettingsManager.getState(setting).next());
                    new BingoExclusionsGUI().open(viewer);
                }
                @Override public ItemStack getItem(KatzePlayer viewer) {
                    return ItemStackCreator.getStack(group.getDisplayName().color(KatzeColor.LIGHT_BLUE), group.getIcon(),
                            List.of(group.getDescription().color(KatzeColor.LIGHT_GRAY),
                                    SettingsManager.getState(setting).getDisplayName().colorIfAbsent(KatzeColor.WHITE),
                                    Component.translatable("katze.bingo.settings.hint").color(KatzeColor.LIGHT_GRAY)));
                }
            });
        }
        setBack(new BingoSettingsGUI());
    }
}
