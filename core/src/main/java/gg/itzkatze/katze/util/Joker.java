package gg.itzkatze.katze.util;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.text.Texts;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Joker {
    private static final NamespacedKey PLAYER_SKIP_KEY = new NamespacedKey(KatzeGames.get(), "playerSkipItem");
    private static final NamespacedKey TEAM_SKIP_KEY = new NamespacedKey(KatzeGames.get(), "teamSkipItem");
    private static final NamespacedKey SWAP_KEY = new NamespacedKey(KatzeGames.get(), "swapItem");

    public static boolean isSwapItem(ItemStack stack) {
        return ItemStackCreator.hasData(stack, SWAP_KEY);
    }

    public static boolean isPlayerSkipItem(ItemStack stack) {
        return ItemStackCreator.hasData(stack, PLAYER_SKIP_KEY);
    }

    public static boolean isTeamSkipItem(ItemStack stack) {
        return ItemStackCreator.hasData(stack, TEAM_SKIP_KEY);
    }

    public static ItemStack getSwapItem(int amount) {
        return ItemStackCreator.createSpecialItem(
                Material.STRUCTURE_VOID,
                Texts.client("katze.client.joker.swap.title").color(KatzeColor.BLUE),
                List.of(Texts.client("katze.client.joker.swap").color(KatzeColor.LIGHT_GRAY)),
                SWAP_KEY,
                amount
        );
    }

    public static ItemStack getPlayerSkipItem(int amount) {
        return ItemStackCreator.createSpecialItem(
                Material.BARRIER,
                Texts.client("katze.client.joker.skip.title").color(KatzeColor.RED),
                List.of(Texts.client("katze.client.joker.skip").color(KatzeColor.LIGHT_GRAY)),
                PLAYER_SKIP_KEY,
                amount
        );
    }

    public static ItemStack getTeamSkipItem(int amount) {
        return ItemStackCreator.createSpecialItem(
                Material.KNOWLEDGE_BOOK,
                Texts.client("katze.client.joker.team_skip.title").color(KatzeColor.RED),
                List.of(Texts.client("katze.client.joker.team_skip").color(KatzeColor.LIGHT_GRAY)),
                TEAM_SKIP_KEY,
                amount
        );
    }
}
