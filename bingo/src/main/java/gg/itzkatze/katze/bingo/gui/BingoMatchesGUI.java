package gg.itzkatze.katze.bingo.gui;

import gg.itzkatze.katze.bingo.BingoScoreboard;
import gg.itzkatze.katze.bingo.match.BingoMatchManager;
import gg.itzkatze.katze.bingo.match.BingoMatch;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class BingoMatchesGUI extends KatzeGUI {
    private final BingoMatchManager matches;
    private final int page;
    public BingoMatchesGUI(BingoMatchManager matches, int page) {
        super(Component.translatable("katze.bingo.matches.title"), 54);
        this.matches = matches;
        this.page = page;
    }
    @Override public void setItems(KatzePlayer player) {
        fill(ItemStackCreator.fillerItem());
        if (matches.matches().isEmpty()) {
            set(new GUIClickableItem(22) {
                @Override public void run(InventoryClickEvent event, KatzePlayer viewer) {}
                @Override public ItemStack getItem(KatzePlayer viewer) {
                    return ItemStackCreator.getStack(Component.translatable("katze.bingo.waiting").color(KatzeColor.LIGHT_BLUE), Material.CLOCK, List.of());
                }
            });
        }
        for (int slot = 0; slot < 45 && page * 45 + slot < matches.matches().size(); slot++) {
            var match = matches.matches().get(page * 45 + slot);
            set(new GUIClickableItem(slot) {
                @Override public void run(InventoryClickEvent event, KatzePlayer viewer) {
                    var own = matches.assigned(viewer.getUuid());
                    if (own != null && own.result == BingoMatch.Result.ACTIVE && own != match) return;
                    matches.select(viewer.getUuid(), match.id);
                    new BingoBoardGUI(matches, match.id).open(viewer);
                }
                @Override public ItemStack getItem(KatzePlayer viewer) {
                    return ItemStackCreator.getStack(Component.text(match.competitors.get(0).name() + " vs " + match.competitors.get(1).name(), KatzeColor.LIGHT_BLUE), Material.MAP,
                            List.of(new BingoScoreboard(matches).status(match).colorIfAbsent(KatzeColor.WHITE), Component.text(match.id.toString(), KatzeColor.GRAY)));
                }
            });
        }
        if (page > 0) set(GUIClickableItem.getGoBackItem(45, new BingoMatchesGUI(matches, page - 1)));
        if ((page + 1) * 45 < matches.matches().size()) set(GUIClickableItem.getGoForthItem(53, new BingoMatchesGUI(matches, page + 1)));
        set(GUIClickableItem.getCloseItem(49));
    }
    @Override public boolean allowItemMoving() { return false; }
}
