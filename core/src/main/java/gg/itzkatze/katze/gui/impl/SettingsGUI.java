package gg.itzkatze.katze.gui.impl;

import java.util.Comparator;

import gg.itzkatze.katze.text.TextArg;

import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeRegistry;
import gg.itzkatze.katze.challenges.ChallengeSettings;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.GUIMaterial;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.gui.SettingsMenu;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SettingsGUI extends SettingsMenu {

    public SettingsGUI() {
        super(Component.translatable("katze.settings.gui.title"), 27);
    }

    @Override
    public void setItems(KatzePlayer player) {
        fill(ItemStackCreator.fillerItem());

        List<Challenge> challenges = new ArrayList<>(ChallengeRegistry.getAllChallenges());
        challenges.sort(Comparator.comparing(challenge -> challenge.getType().ordinal()));
        int slot = 0;

        for (Challenge challenge : challenges) {
            if (slot >= 7) break;

            set(new GUIClickableItem(contentSlot(slot)) {
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
                            if (ChallengeManager.isChallengeActive(challenge.getType())) return;
                            player.sendMessage(Component.translatable("katze.settings.gui.challenge.disabled_msg", TextArg.component("name", challenge.getType().displayName().color(KatzeColor.RED))));
                        } else {
                            ChallengeManager.loadChallenge(challenge);
                            if (!ChallengeManager.isChallengeActive(challenge.getType())) return;
                            player.sendMessage(Component.translatable("katze.settings.gui.challenge.enabled_msg", TextArg.component("name", challenge.getType().displayName().color(KatzeColor.LIME))));
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
                    lore.add(Component.translatable("katze.settings.gui.status", TextArg.component("state", Component.translatable(active ? "katze.settings.gui.enabled" : "katze.settings.gui.disabled")
                            .color(active ? KatzeColor.LIME : KatzeColor.RED))));

                    if (challenge.hasSettings()) {
                        lore.add(Component.empty());
                        lore.add(Component.translatable("katze.settings.gui.left_click").color(KatzeColor.LIGHT_GRAY));
                        lore.add(Component.translatable("katze.settings.gui.right_click").color(KatzeColor.LIGHT_GRAY));
                    } else {
                        lore.add(Component.empty());
                        lore.add(Component.translatable("katze.settings.gui.click").color(KatzeColor.LIGHT_GRAY));
                    }

                    lore.add(Component.empty());
                    lore.add(Component.translatable("katze.settings.gui.description", TextArg.component("desc", challenge.getType().getDescription().color(KatzeColor.WHITE))));

                    if (material.hasSkull()) {
                        return ItemStackCreator.getSkull(material.skull(), displayName, 1, lore);
                    } else {
                        return ItemStackCreator.getStack(displayName, material.material(), 1, lore);
                    }
                }
            });

            slot++;
        }

        setClose();
        setForward(new GlobalSettingsGUI());
    }
}
