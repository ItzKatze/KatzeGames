package gg.itzkatze.katze.randomizer;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RandomizerDropsTest {

    private final RandomizerGameListener listener = new RandomizerGameListener(List.of(Material.DIAMOND), Stack::new);

    @Test void blockLootMobLootAndChestLootUseTheSameReplacementAndKeepCounts() {
        ItemStack mined = listener.randomizeBlockDrop(new Stack(Material.COAL, 4), List.of(), null);
        List<ItemStack> chest = new ArrayList<>(List.of(new Stack(Material.COAL, 17)));
        List<ItemStack> mob = new ArrayList<>(List.of(new Stack(Material.COAL, 2)));

        listener.randomize(chest, null);
        listener.randomize(mob, null);

        assertEquals(Material.DIAMOND, mined.getType());
        assertEquals(mined.getType(), chest.getFirst().getType());
        assertEquals(mined.getType(), mob.getFirst().getType());
        assertEquals(4, mined.getAmount());
        assertEquals(17, chest.getFirst().getAmount());
        assertEquals(2, mob.getFirst().getAmount());
    }

    @Test void dirtAndGrassUseTheirActualDirtDrop() {
        ItemStack dirt = listener.randomizeBlockDrop(new Stack(Material.DIRT, 1), List.of(), null);
        ItemStack grass = listener.randomizeBlockDrop(new Stack(Material.DIRT, 1), List.of(), null);

        assertEquals(Material.DIAMOND, dirt.getType());
        assertEquals(dirt.getType(), grass.getType());
    }

    @Test void brokenContainersPreserveStoredLootButRandomizeTheContainerItem() {
        ItemStack stored = new Stack(Material.EMERALD, 12);
        List<ItemStack> contents = List.of(stored.clone());

        assertSame(stored, listener.randomizeBlockDrop(stored, contents, null));
        ItemStack chest = listener.randomizeBlockDrop(new Stack(Material.CHEST, 1), contents, null);

        assertEquals(Material.EMERALD, stored.getType());
        assertEquals(12, stored.getAmount());
        assertEquals(Material.DIAMOND, chest.getType());
        assertEquals(1, chest.getAmount());
    }

    @Test void mergedAndSplitContainerStacksArePreserved() {
        List<ItemStack> contents = List.of(new Stack(Material.EMERALD, 3), new Stack(Material.EMERALD, 5));
        ItemStack first = new Stack(Material.EMERALD, 6);
        ItemStack second = new Stack(Material.EMERALD, 2);

        assertSame(first, listener.randomizeBlockDrop(first, contents, null));
        assertSame(second, listener.randomizeBlockDrop(second, contents, null));
        assertEquals(Material.DIAMOND,
                listener.randomizeBlockDrop(new Stack(Material.EMERALD, 1), contents, null).getType());
    }

    private static final class Stack extends ItemStack {
        private final Material material;
        private int amount;

        private Stack(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        @Override public Material getType() { return material; }
        @Override public int getAmount() { return amount; }
        @Override public boolean isEmpty() { return amount <= 0 || material == Material.AIR; }
        @Override public void setAmount(int amount) { this.amount = amount; }
        @Override public boolean isSimilar(ItemStack other) { return other != null && material == other.getType(); }
        @Override public ItemStack clone() { return new Stack(material, amount); }
    }
}
