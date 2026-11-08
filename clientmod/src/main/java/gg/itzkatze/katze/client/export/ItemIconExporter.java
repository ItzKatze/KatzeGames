package gg.itzkatze.katze.client.export;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class ItemIconExporter extends NativeIconExporter<ItemStack> {
    public static final String OUTPUT_PROPERTY = "katzegames.itemExport.output";

    ItemIconExporter(Path output) { super(output, 16); }

    @Override protected List<Entry<ItemStack>> entries(Minecraft client) {
        return BuiltInRegistries.ITEM.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().identifier().toString()))
                .map(entry -> new Entry<>(entry.getKey().identifier(), entry.getValue().getDefaultInstance())).filter(entry -> !entry.value().isEmpty()).toList();
    }

    @Override protected void drawEntry(GuiGraphicsExtractor graphics, ItemStack value, int x, int y) { graphics.fakeItem(value, x, y); }
    @Override protected String renderer() { return "minecraft-inventory"; }
}
