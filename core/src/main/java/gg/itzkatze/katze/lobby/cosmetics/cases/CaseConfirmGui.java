package gg.itzkatze.katze.lobby.cosmetics.cases;

import gg.itzkatze.katze.lobby.cosmetics.Cosmetic;
import gg.itzkatze.katze.lobby.cosmetics.CosmeticStore;
import gg.itzkatze.katze.lobby.cosmetics.PointsText;
import gg.itzkatze.katze.lobby.cosmetics.PurchaseConfirmGui;
import gg.itzkatze.katze.lobby.cosmetics.Rarity;
import gg.itzkatze.katze.lobby.cosmetics.ShopSounds;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CaseConfirmGui extends PurchaseConfirmGui {

    private int count;

    private CaseConfirmGui(int count) {
        super(Component.translatable("katze.cosmetics.confirm_title"));
        this.count = count;
    }

    public static void offer(@NotNull KatzePlayer player) {
        Player bukkit = player.getPlayer();
        if (bukkit == null) return;

        CosmeticStore store = CosmeticStore.get();
        int balance = store.points(bukkit.getUniqueId());
        if (balance < CaseLoot.PRICE) {
            bukkit.sendMessage(PointsText.missingComponent(CaseLoot.PRICE - balance).color(KatzeColor.RED));
            bukkit.playSound(ShopSounds.DENY);
            return;
        }
        new CaseConfirmGui(Math.clamp(store.caseCount(bukkit.getUniqueId()), 1, CaseRollGui.MAX_CASES)).open(player);
    }

    static @NotNull Component pityLine(@NotNull UUID player) {
        CosmeticStore store = CosmeticStore.get();
        List<Cosmetic> missing = CaseLoot.missing(CaseLoot.available(store.catalog(), store.today()), cosmetic -> store.owns(player, cosmetic));
        if (missing.isEmpty()) {
            return Component.translatable("katze.cosmetics.own_everything").color(KatzeColor.LIGHT_GRAY);
        }
        int streak = store.duplicateStreak(player);
        if (CaseLoot.pityDue(streak, missing)) {
            return Component.translatable("katze.cosmetics.next_case_new").color(KatzeColor.MAGENTA);
        }
        int left = CaseLoot.PITY_STREAK - streak;
        return Component.translatable("katze.cosmetics.pity_progress", gg.itzkatze.katze.text.TextArg.number("count", left)).color(KatzeColor.LIGHT_GRAY);
    }

    @Override
    protected @NotNull ItemStack summary(@NotNull KatzePlayer viewer) {
        int balance = CosmeticStore.get().points(viewer.getUuid());
        int price = CaseLoot.PRICE * count;
        List<Component> lore = new ArrayList<>();
        lore.add(Component.translatable("katze.cosmetics.price", gg.itzkatze.katze.text.TextArg.number("points", price)).color(KatzeColor.GOLD));
        lore.add(balance >= price ? Component.translatable("katze.cosmetics.after_opening", gg.itzkatze.katze.text.TextArg.number("points", balance - price)).color(KatzeColor.LIGHT_GRAY)
                : PointsText.missingComponent(price - balance).color(KatzeColor.RED));
        lore.add(pityLine(viewer.getUuid()));
        lore.add(Component.empty());
        lore.add(Component.translatable("katze.cosmetics.contains", KatzeColor.LIGHT_GRAY));
        lore.add(rarities());
        lore.add(Component.empty());
        lore.add(Component.translatable("katze.cosmetics.add_case_hint", gg.itzkatze.katze.text.TextArg.number("max", CaseRollGui.MAX_CASES)).color(KatzeColor.LIGHT_GRAY));
        lore.add(Component.translatable("katze.cosmetics.remove_case_hint").color(KatzeColor.LIGHT_GRAY));
        Component name = Component.translatable(count == 1 ? "katze.cosmetics.case" : "katze.cosmetics.cases",
                gg.itzkatze.katze.text.TextArg.number("count", count)).color(KatzeColor.MAGENTA);
        return ItemStackCreator.getStack(name, Material.ENDER_CHEST, count, lore);
    }

    @Override
    protected @NotNull Component acceptHint() {
        return Component.translatable("katze.cosmetics.open_for", gg.itzkatze.katze.text.TextArg.number("count", count),
                gg.itzkatze.katze.text.TextArg.number("points", CaseLoot.PRICE * count));
    }

    @Override
    protected @NotNull Component denyHint() {
        return Component.translatable("katze.cosmetics.close");
    }

    @Override
    protected void clickSummary(@NotNull KatzePlayer clicker, @NotNull ClickType click) {
        int change = switch (click) {
            case LEFT, SHIFT_LEFT -> 1;
            case RIGHT, SHIFT_RIGHT -> -1;
            default -> 0;
        };
        Player player = clicker.getPlayer();
        if (change == 0 || player == null) return;

        int next = count + change;
        if (next < 1 || next > CaseRollGui.MAX_CASES) {
            player.playSound(ShopSounds.DENY);
            return;
        }
        count = next;
        CosmeticStore.get().rememberCaseCount(player, count);
        player.playSound(ShopSounds.CLICK);
        redraw(clicker);
    }

    @Override
    protected void accept(@NotNull KatzePlayer clicker) {
        if (!CaseRollGui.begin(clicker, count)) {
            redraw(clicker);
        }
    }

    @Override
    protected void deny(@NotNull KatzePlayer clicker) {
        Player player = clicker.getPlayer();
        if (player == null) return;

        player.playSound(ShopSounds.CLICK);
        player.closeInventory();
    }

    private void redraw(@NotNull KatzePlayer viewer) {
        setItems(viewer);
        updateInventory(viewer);
    }

    private static @NotNull Component rarities() {
        TextComponent.Builder line = Component.text();
        for (Rarity tier : Rarity.values()) {
            if (tier.ordinal() > 0) {
                line.append(Component.text(" · ", KatzeColor.LIGHT_GRAY));
            }
            line.append(tier.displayNameComponent().color(tier.color()));
        }
        return line.build();
    }
}
