package gg.itzkatze.katze.bingo.gui;

import gg.itzkatze.katze.bingo.BingoScoreboard;
import gg.itzkatze.katze.bingo.board.BingoObjective;
import gg.itzkatze.katze.bingo.match.BingoCompetitor;
import gg.itzkatze.katze.bingo.match.BingoMatch;
import gg.itzkatze.katze.bingo.match.BingoMatchManager;
import gg.itzkatze.katze.gui.GUIItem;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.StringUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class BingoBoardGUI extends KatzeGUI {
    private final BingoMatchManager matches;
    private final UUID board;
    public BingoBoardGUI(BingoMatchManager matches, UUID board) {
        super(Component.translatable("katze.bingo.board.title"), 54);
        this.matches = matches;
        this.board = board;
    }
    @Override public void setItems(KatzePlayer player) {
        fill(ItemStackCreator.fillerItem());
        BingoMatch match = matches.find(board);
        if (match == null || !matches.packReady(player.getUuid())) return;
        for (int cell = 0; cell < 25; cell++) {
            int index = cell;
            set(new GUIItem(cell / 5 * 9 + cell % 5 + 2) {
                @Override public ItemStack getItem(KatzePlayer viewer) { return objective(match, index, viewer); }
            });
        }
        set(new GUIItem(47) {
            @Override public ItemStack getItem(KatzePlayer viewer) {
                return ItemStackCreator.getStack(Component.translatable("katze.bingo.overview.title").color(KatzeColor.LIGHT_BLUE), Material.CLOCK,
                        List.of(Component.translatable("katze.bingo.overview.status", TextArg.component("status", new BingoScoreboard(matches).status(match))).color(KatzeColor.WHITE),
                                Component.text(match.competitors.get(0).name() + ": " + match.tiles(0), KatzeColor.LIGHT_BLUE),
                                Component.text(match.competitors.get(1).name() + ": " + match.tiles(1), KatzeColor.ORANGE),
                                Component.translatable("katze.bingo.overview.remaining", TextArg.number("count", 25 - match.tiles(0) - match.tiles(1))).color(KatzeColor.LIME)));
            }
        });
        set(new GUIItem(51) {
            @Override public ItemStack getItem(KatzePlayer viewer) {
                List<Component> legend = new ArrayList<>();
                for (var category : BingoObjective.Category.values()) {
                    legend.add(Component.text(Character.toString(0xE030 + category.ordinal()), KatzeColor.WHITE)
                            .font(Key.key("katzegames", "bingo/categories"))
                            .append(Component.space().font(Key.key("minecraft", "default")))
                            .append(Component.translatable("katze.bingo.category." + category.name().toLowerCase(Locale.ROOT))
                                    .font(Key.key("minecraft", "default")).color(KatzeColor.LIGHT_GRAY)));
                }
                return ItemStackCreator.getStack(Component.translatable("katze.bingo.categories.title"), Material.BOOK, legend);
            }
        });
        set(GUIClickableItem.getCloseItem(49));
        if (match.result != BingoMatch.Result.ACTIVE || match.side(player.getUuid()) < 0) set(GUIClickableItem.getGoBackItem(45, new BingoMatchesGUI(matches, 0)));
    }

    private ItemStack objective(BingoMatch match, int cell, KatzePlayer viewer) {
        String id = match.objectives.get(cell);
        BingoObjective objective = matches.catalog().get(id);
        int owner = match.owners[cell];
        List<Component> lore = new ArrayList<>();
        if (owner < 0) {
            lore.add(Component.translatable("katze.bingo.unclaimed").color(KatzeColor.LIME).decorate(TextDecoration.BOLD));
            if (objective.category() == BingoObjective.Category.ITEM) {
                int progress = 0;
                int side = match.side(viewer.getUuid());
                if (side >= 0) for (var member : match.competitors.get(side).members()) {
                    Player online = Bukkit.getPlayer(member.id());
                    if (online != null) progress = Math.max(progress, matches.progress(online, objective));
                }
                lore.add(matches.catalog().progress(id, progress));
            }
        } else {
            lore.add(Component.translatable("katze.bingo.owned", TextArg.text("owner", match.competitors.get(owner).name()))
                    .color(owner == 0 ? KatzeColor.LIGHT_BLUE : KatzeColor.ORANGE).decorate(TextDecoration.BOLD));
            String contributor = match.competitors.get(owner).members().stream().filter(member -> member.id().equals(match.contributors[cell]))
                    .map(BingoCompetitor.Member::name).findFirst().orElse("");
            lore.add(Component.translatable("katze.bingo.contribution", TextArg.text("player", contributor), TextArg.text("time", StringUtility.formatTime(match.claimedAt[cell]))));
        }
        lore.add(1, matches.catalog().difficulty(id));
        if (match.winningLine.contains(cell)) lore.add(Component.translatable("katze.bingo.winning_line").color(KatzeColor.GOLD));
        Component name = matches.catalog().name(id)
                .color(owner < 0 ? KatzeColor.WHITE : owner == 0 ? KatzeColor.LIGHT_BLUE : KatzeColor.ORANGE);
        Material icon = owner < 0 ? matches.catalog().icon(id) : owner == 0 ? Material.BLUE_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE;
        ItemStack stack = ItemStackCreator.getStack(name, icon, lore.stream().map(line -> line.colorIfAbsent(KatzeColor.LIGHT_GRAY)).toList());
        stack.editMeta(meta -> {
            if (owner >= 0) meta.setEnchantmentGlintOverride(match.winningLine.contains(cell));
        });
        return stack;
    }
    @Override public boolean allowItemMoving() { return false; }
}
