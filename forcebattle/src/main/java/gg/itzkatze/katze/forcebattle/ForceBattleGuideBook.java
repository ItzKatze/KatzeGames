package gg.itzkatze.katze.forcebattle;

import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.text.Texts;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class ForceBattleGuideBook {

    private static final NamespacedKey KEY = new NamespacedKey("katzegames", "forcebattle_guide_book");
    private static final int PAGE_COUNT = 8;

    private ForceBattleGuideBook() {
    }

    public static @NotNull ItemStack create(@NotNull Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Texts.render(Component.translatable("katze.forcebattle.guidebook.title"), player));
        meta.author(Texts.render(Component.translatable("katze.forcebattle.guidebook.author"), player));
        Component[] pages = java.util.stream.IntStream.rangeClosed(1, PAGE_COUNT)
                .mapToObj(page -> Texts.render(Component.translatable("katze.forcebattle.guidebook.page." + page), player))
                .toArray(Component[]::new);
        meta.addPages(pages);
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.INTEGER, 1);
        book.setItemMeta(meta);
        return book;
    }

    public static boolean isGuideBook(@NotNull ItemStack stack) {
        return ItemStackCreator.hasData(stack, KEY);
    }

    public static void give(@NotNull Player player) {
        PlayerInventory inventory = player.getInventory();
        boolean owned = Arrays.stream(inventory.getContents()).anyMatch(stack -> stack != null && isGuideBook(stack));
        if (!owned) {
            inventory.addItem(create(player));
        }
    }
}
