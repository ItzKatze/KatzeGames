package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.game.SessionPersistence;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.gui.impl.SettingsGUI;
import gg.itzkatze.katze.gui.sign.NumberSignInput;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

final class MobArmyMenus {

    private MobArmyMenus() {}

    static ItemStack item(Material material, Component title, List<Component> lore) {
        return ItemStackCreator.getStack(styled(title), material, 1, lore.stream().map(MobArmyMenus::styled).toList());
    }

    static Component styled(Component text) {
        return text.colorIfAbsent(KatzeColor.WHITE).decoration(TextDecoration.ITALIC, false);
    }

    static GUIClickableItem button(int slot, ItemStack item, BiConsumer<InventoryClickEvent, KatzePlayer> action) {
        return new GUIClickableItem(slot) {
            @Override
            public void run(InventoryClickEvent event, KatzePlayer player) {action.accept(event, player);}

            @Override
            public ItemStack getItem(KatzePlayer player) {return item;}
        };
    }

    static Component name(MobArmyMatch.CapturedMob mob) {
        return Component.translatable("entity.minecraft." + mob.type.toLowerCase(Locale.ROOT));
    }

    static ItemStack mobItem(List<MobArmyMatch.CapturedMob> mobs, int wave, boolean details) {
        var mob = mobs.getFirst();
        Material material = Material.matchMaterial(mob.type + "_SPAWN_EGG");
        if (material == null) material = Material.PAPER;
        return item(material, name(mob), mobLore(mobs, wave, details));
    }

    static List<Component> mobLore(List<MobArmyMatch.CapturedMob> mobs, int wave, boolean details) {
        List<Component> lore = new ArrayList<>();
        if (details) lore.add(Component.translatable("katze.mobarmy.collected", TextArg.number("count", mobs.size())));
        else {
            lore.add(Component.translatable("katze.mobarmy.gui.assigned", TextArg.number("count", mobs.stream().filter(entry -> entry.wave == wave).count())));
            lore.add(Component.translatable("katze.mobarmy.gui.available", TextArg.number("count", mobs.stream().filter(entry -> entry.wave == -1).count())));
            lore.add(Component.empty());
            lore.add(Component.translatable("katze.mobarmy.gui.add").color(KatzeColor.LIGHT_GRAY));
            lore.add(Component.translatable("katze.mobarmy.gui.remove").color(KatzeColor.LIGHT_GRAY));
        }
        return lore.stream().map(MobArmyMenus::styled).toList();
    }

    static List<Component> waveLore(MobArmyMatch.Side side, int wave) {
        List<Component> lore = new ArrayList<>();
        for (var mobs : side.mobsByType().values()) {
            long count = mobs.stream().filter(mob -> mob.wave == wave).count();
            if (count > 0) lore.add(styled(Component.text(count + "x ").append(name(mobs.getFirst()))));
        }
        return lore;
    }

    abstract static class Menu extends KatzeGUI {

        final MobArmyGame game;
        final MobArmyMatch.Side side;

        Menu(MobArmyGame game, MobArmyMatch.Side side, String key, int size) {
            super(Component.translatable("katze.mobarmy.gui." + key), size);
            this.game = game;
            this.side = side;
        }

        boolean owns(Player player) {
            return game.side(player.getUniqueId()) == side && game.match != null
                    && !side.finished && player.getGameMode() != GameMode.SPECTATOR;
        }

        boolean editing(KatzePlayer player) {
            return player.getPlayer() != null && owns(player.getPlayer()) && game.running()
                    && game.match.phase == MobArmyMatch.Phase.CONFIGURATION;
        }

        @Override
        public boolean allowItemMoving() {return false;}

    }

    static final class Waves extends Menu {

        Waves(MobArmyGame game, MobArmyMatch.Side side) {super(game, side, "waves", 9);}

        @Override
        public void setItems(KatzePlayer viewer) {
            clear();
            for (int wave = 0; wave < 3; wave++) {
                int index = wave;
                set(button(
                        wave, item(Material.DIAMOND_SWORD, Component.translatable("katze.mobarmy.gui.wave", TextArg.number("wave", wave + 1)), waveLore(side, wave)),
                        (event, player) -> {if (editing(player)) new Configure(game, side, index, 0).open(player);}
                ));
            }
            set(button(
                    8, item(
                            side.ready ? Material.LIME_WOOL : Material.RED_WOOL,
                            Component.translatable(side.ready ? "katze.mobarmy.gui.ready_on" : "katze.mobarmy.gui.ready"), List.of()
                    ),
                    (event, player) -> {if (editing(player)) game.ready(side);}
            ));
        }

    }

    static final class Configure extends Menu {

        final int wave;
        final int page;

        Configure(MobArmyGame game, MobArmyMatch.Side side, int wave, int page) {
            super(game, side, wave < 0 ? "details" : "configure", 54);
            this.wave = wave;
            this.page = page;
        }

        @Override
        public void setItems(KatzePlayer viewer) {
            clear();
            boolean details = wave < 0;
            set(button(
                    0, item(Material.RED_WOOL, Component.translatable("katze.gui.back"), List.of()), (event, player) -> {
                        if (details) player.getPlayer().closeInventory();
                        else if (editing(player)) new Waves(game, side).open(player);
                    }
            ));
            var types = new ArrayList<>(side.mobsByType().entrySet());
            int start = page * 36;
            for (int i = start; i < Math.min(types.size(), start + 36); i++) {
                var type = types.get(i);
                set(button(
                        9 + i - start, mobItem(type.getValue(), wave, details), (event, player) -> {
                            if (!details && editing(player) && (event.isLeftClick() || event.isRightClick())) {
                                game.allocate(side, type.getKey(), wave, event.isLeftClick());
                            }
                        }
                ));
            }
            if (page > 0) navigation(45, page - 1);
            if (start + 36 < types.size()) navigation(53, page + 1);
        }

        private void navigation(int slot, int next) {
            set(button(
                    slot, item(Material.ARROW, Component.translatable(slot == 45 ? "spectatorMenu.previous_page" : "spectatorMenu.next_page"), List.of()),
                    (event, player) -> {
                        if (wave < 0 && game.match != null && game.match.phase == MobArmyMatch.Phase.RESULTS || editing(player)) {
                            new Configure(game, side, wave, next).open(player);
                        }
                    }
            ));
        }

    }

    static final class Backpack extends Menu {

        Backpack(MobArmyGame game, MobArmyMatch.Side side) {super(game, side, "backpack", 27);}

        @Override
        public void setItems(KatzePlayer player) {}

        @Override
        public boolean allowItemMoving() {return true;}

        @Override
        public void onClose(InventoryCloseEvent event, CloseReason reason) {game.dirty();}

        boolean accessible(Player player) {
            return owns(player) && game.running() && !game.roof(player)
                    && game.challenge.settings.isEnabled(MobArmySettings.BACKPACK)
                    && (game.match.phase == MobArmyMatch.Phase.COLLECTION || game.match.phase == MobArmyMatch.Phase.CONFIGURATION || game.combat());
        }

    }

    static final class Settings extends KatzeGUI {

        final MobArmy challenge;

        Settings(MobArmy challenge) {
            super(Component.translatable("katze.mobarmy.gui.settings"), 27);
            this.challenge = challenge;
        }

        @Override
        public boolean allowItemMoving() {return false;}

        private boolean editable(KatzePlayer player) {
            return player.getPlayer() != null && player.getPlayer().isOp() && !ChallengeManager.isGameStarted();
        }

        @Override
        public void setItems(KatzePlayer viewer) {
            clear();
            int slot = 10;
            for (MobArmySettings setting : MobArmySettings.values()) {
                Material material = setting == MobArmySettings.BACKPACK
                        ? Material.CHEST
                        : setting == MobArmySettings.COLLECTION_DURATION ? Material.CLOCK : Material.DIAMOND_PICKAXE;
                set(button(
                        slot++, item(material, setting.getDisplayName(), List.of(challenge.settings.getState(setting).getDisplayName())), (event, player) -> {
                            if (!editable(player)) return;
                            if (setting == MobArmySettings.COLLECTION_DURATION) {
                                int current = ((MobArmySettings.Duration) challenge.settings.getState(setting)).minutes();
                                NumberSignInput.open(
                                        player, setting.getDisplayName(), current, 1, 1440, (who, minutes) -> {
                                            if (editable(who)) {
                                                challenge.settings.setState(setting, new MobArmySettings.Duration(minutes));
                                                SessionPersistence.requestSave();
                                            }
                                            open(who);
                                        }, KatzePlayer::sendMessage
                                );
                            } else {
                                challenge.settings.setState(setting, challenge.settings.getState(setting).next());
                                SessionPersistence.requestSave();
                                open(player);
                            }
                        }
                ));
            }
            set(GUIClickableItem.getGoBackItem(18, new SettingsGUI()));
        }

    }

}
