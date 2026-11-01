package gg.itzkatze.katze.forcebattle.gui.info;

import gg.itzkatze.katze.data.MobsSimplified;
import gg.itzkatze.katze.data.ObjectiveNames;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.GUIItem;
import gg.itzkatze.katze.gui.GUIMaterial;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class InfoSimplifiedMobsGUI extends KatzeGUI {

    public InfoSimplifiedMobsGUI() {
        super(Component.translatable("katze.forcebattle.info.mobs_title"), 27);
    }

    @Override
    public void setItems(final KatzePlayer player) {
        fill(ItemStackCreator.fillerItem());
        set(GUIClickableItem.getGoBackItem(21, new InfoGUI()));
        set(GUIClickableItem.getCloseItem(22));

        int index = 0;
        for (MobsSimplified simplified : MobsSimplified.values()) {
            set(new GUIItem(index) {
                @Override
                public ItemStack getItem(final KatzePlayer player) {
                    GUIMaterial guiMaterial = simplified.getGuiMaterial();
                    ArrayList<Component> lore = new ArrayList<>();

                    for (EntityType entityType : simplified.getMobs()) {
                        lore.add(Component.translatable("entity.minecraft." + entityType.key().value()).color(KatzeColor.WHITE));
                    }

                    if (guiMaterial.hasSkull()) {
                        return ItemStackCreator.getSkull(guiMaterial.skull(), ObjectiveNames.of(simplified).color(KatzeColor.GOLD), 1, lore);
                    } else {
                        return ItemStackCreator.getStack(ObjectiveNames.of(simplified).color(KatzeColor.GOLD), guiMaterial.material(), 1, lore);
                    }
                }
            });

            index++;
        }
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }

}

