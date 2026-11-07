package gg.itzkatze.katze.bingo.gui;

import gg.itzkatze.katze.bingo.BingoSettings;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.SettingsMenu;
import gg.itzkatze.katze.gui.impl.SettingsGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class BingoSettingsGUI extends SettingsMenu {
    public BingoSettingsGUI() { super(Component.translatable("katze.bingo.settings.title"), 27); }
    @Override public void setItems(KatzePlayer player) {
        fill(ItemStackCreator.fillerItem());
        for (BingoSettings.Option option : BingoSettings.Option.values()) {
            if (option.name().startsWith("EXCLUDE_")) continue;
            set(new GUIClickableItem(10 + option.ordinal()) {
                @Override public void run(InventoryClickEvent event, KatzePlayer viewer) {
                    if (ChallengeManager.isGameStarted()) { viewer.sendMessage(Component.translatable("katze.bingo.error.locked")); return; }
                    SettingsManager.setState(option, SettingsManager.getState(option).next());
                    new BingoSettingsGUI().open(viewer);
                }
                @Override public ItemStack getItem(KatzePlayer viewer) {
                    Material icon = switch (option) {
                        case DURATION -> Material.CLOCK;
                        case ALLOW_UNEQUAL_TEAMS -> Material.PLAYER_HEAD;
                        case ITEM -> Material.CHEST;
                        case MOB -> Material.ZOMBIE_HEAD;
                        case BIOME -> Material.GRASS_BLOCK;
                        case STRUCTURE -> Material.BRICKS;
                        case ADVANCEMENT -> Material.KNOWLEDGE_BOOK;
                        default -> Material.BARRIER;
                    };
                    return ItemStackCreator.getStack(option.getDisplayName().color(KatzeColor.LIGHT_BLUE), icon,
                            List.of(SettingsManager.getState(option).getDisplayName().colorIfAbsent(KatzeColor.WHITE),
                                    Component.translatable("katze.bingo.settings.hint").color(KatzeColor.LIGHT_GRAY)));
                }
            });
        }
        setForward(new BingoExclusionsGUI());
        setBack(new SettingsGUI());
    }
}
