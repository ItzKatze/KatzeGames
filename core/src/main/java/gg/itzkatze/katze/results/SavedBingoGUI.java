package gg.itzkatze.katze.results;

import gg.itzkatze.katze.gui.GUIItem;
import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.text.Texts;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.StringUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class SavedBingoGUI extends KatzeGUI {
    private final SavedBattle battle;
    public SavedBingoGUI(SavedBattle battle) {
        super(Component.translatable("katze.bingo.board.title"), 54);
        this.battle = battle;
    }
    @Override public void setItems(KatzePlayer player) {
        fill(ItemStackCreator.fillerItem());
        SavedBattle.Bingo bingo = battle.bingo();
        if (bingo == null) return;
        for (int index = 0; index < Math.min(25, bingo.cells().size()); index++) {
            int cellIndex = index;
            SavedBattle.BingoCell cell = bingo.cells().get(index);
            set(new GUIItem(index / 5 * 9 + index % 5 + 2) {
                @Override public ItemStack getItem(KatzePlayer viewer) {
                    List<Component> lore = new ArrayList<>();
                    lore.add(GsonComponentSerializer.gson().deserialize(cell.difficulty()));
                    if (cell.owner() >= 0) {
                        lore.add(Component.translatable("katze.bingo.owned", TextArg.text("owner", bingo.competitors().get(cell.owner()))));
                        lore.add(Component.translatable("katze.bingo.contribution", TextArg.text("player", cell.contributor()),
                                TextArg.text("time", StringUtility.formatTime(cell.claimedAt()))));
                    } else lore.add(Component.translatable("katze.bingo.unclaimed"));
                    if (bingo.winningLine().contains(cellIndex)) lore.add(Component.translatable("katze.bingo.winning_line").color(KatzeColor.GOLD));
                    Component name = GsonComponentSerializer.gson().deserialize(cell.germanName() != null && Texts.isGerman(viewer.getPlayer().locale()) ? cell.germanName() : cell.name())
                            .color(cell.owner() < 0 ? KatzeColor.WHITE : cell.owner() == 0 ? KatzeColor.LIGHT_BLUE : KatzeColor.ORANGE);
                    Material material = Material.matchMaterial(cell.icon());
                    ItemStack item = ItemStackCreator.getStack(name, material == null ? Material.PAPER : material,
                            lore.stream().map(line -> line.colorIfAbsent(KatzeColor.LIGHT_GRAY)).toList());
                    item.editMeta(meta -> {
                        if (cell.owner() >= 0) meta.setEnchantmentGlintOverride(true);
                    });
                    return item;
                }
            });
        }
        set(new GUIItem(47) {
            @Override public ItemStack getItem(KatzePlayer viewer) {
                Component result = bingo.winner() < 0 ? Component.translatable("katze.bingo.draw")
                        : Component.translatable("katze.bingo.winner", TextArg.text("winner", bingo.competitors().get(bingo.winner())));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.translatable("katze.bingo.result." + bingo.reason()));
                lore.add(Component.text(StringUtility.formatTime(battle.durationSeconds())));
                for (SavedBattle.Player member : battle.players()) lore.add(Component.text(member.name() + ": " + member.points()));
                return ItemStackCreator.getStack(result.colorIfAbsent(KatzeColor.WHITE), Material.CLOCK,
                        lore.stream().map(line -> line.colorIfAbsent(KatzeColor.LIGHT_GRAY)).toList());
            }
        });
        set(GUIClickableItem.getGoBackItem(45, new SavedResultsGUI()));
        set(GUIClickableItem.getCloseItem(49));
    }
    @Override public boolean allowItemMoving() { return false; }
}
