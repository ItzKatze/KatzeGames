package gg.itzkatze.katze.forcebattle.gui;

import gg.itzkatze.katze.forcebattle.ForceBattleTeam;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.GUIItem;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.Skull;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class TeamBackpackGUI extends KatzeGUI {
    private final int index;
    private final ForceBattleTeam team;

    public TeamBackpackGUI(int index, int size, ForceBattleTeam team) {
        super(Component.translatable("katze.forcebattle.backpack.title", TextArg.number("page", index + 1)), size);
        this.index = index;
        this.team = team;
    }

    @Override
    public void setItems(final KatzePlayer player) {
        int inventoryPages = team.getBACKPACK_INVENTORIES().size();

        if (index < 0 || index >= inventoryPages) return;

        int nextArrowSlot = size - 1;
        int prevArrowSlot = size - 9;

        synchronized (items) {
            items.removeIf(item -> {
                if (item.slot == nextArrowSlot || item.slot == prevArrowSlot) {
                    return item instanceof GUIClickableItem;
                }
                return false;
            });
        }

        GUIItem nextItem = get(nextArrowSlot);
        if (nextItem instanceof GUIClickableItem) {
            getInventory().setItem(nextArrowSlot, null);
        }

        GUIItem prevItem = get(prevArrowSlot);
        if (prevItem instanceof GUIClickableItem) {
            getInventory().setItem(prevArrowSlot, null);
        }

        if (index < inventoryPages - 1) set(navigation(size - 1, index + 1, inventoryPages));
        if (index > 0) set(navigation(size - 9, index - 1, inventoryPages));
    }

    private GUIItem navigation(int slot, int page, int pages) {
        boolean forward = page > index;
        return new GUIClickableItem(slot) {
            @Override
            public void run(InventoryClickEvent event, KatzePlayer player) {
                team.getBACKPACK_INVENTORIES().get(page).open(player);
            }

            @Override
            public ItemStack getItem(KatzePlayer player) {
                return ItemStackCreator.getSkull(
                        forward ? Skull.ARROW_RIGHT : Skull.ARROW_LEFT,
                        Component.translatable(forward ? "spectatorMenu.next_page" : "spectatorMenu.previous_page").color(KatzeColor.LIME),
                        1,
                        Component.translatable("book.pageIndicator", Component.text(page + 1), Component.text(pages)).color(KatzeColor.LIGHT_GRAY));
            }
        };
    }

    public boolean isNavigationSlot(int slot) {
        int inventoryPages = team.getBACKPACK_INVENTORIES().size();
        return (slot == size - 1 && index < inventoryPages - 1) || (slot == size - 9 && index > 0);
    }

    @Override
    public boolean allowItemMoving() {
        return true;
    }
}
