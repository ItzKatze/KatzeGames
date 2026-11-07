package gg.itzkatze.katze.gui;

import net.kyori.adventure.text.Component;

public abstract class SettingsMenu extends KatzeGUI {
    protected SettingsMenu(Component title, int size) { super(title, size); }

    protected final void setBack(KatzeGUI parent) { set(GUIClickableItem.getGoBackItem(size - 5, parent)); }
    protected final void setForward(KatzeGUI next) { set(GUIClickableItem.getGoForthItem(size - 1, next)); }
    protected final void setClose() { set(GUIClickableItem.getCloseItem(size - 5)); }

    protected final int contentSlot(int index) { return 10 + index / 7 * 9 + index % 7; }

    @Override public final boolean allowItemMoving() { return false; }
}
