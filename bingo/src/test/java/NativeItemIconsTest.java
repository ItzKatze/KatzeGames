import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NativeItemIconsTest {
    @TempDir Path directory;

    @Test void localNativeExportsAreCompleteAndValidWhenPresent() throws Exception {
        Path root = Path.of("src/main/item-icons/26.3");
        assumeTrue(Files.exists(root.resolve("manifest.json")), "Local developer export cache is not present");
        var manifest = JsonParser.parseString(Files.readString(root.resolve("manifest.json"))).getAsJsonObject();
        assertEquals("26.3", manifest.get("minecraft").getAsString());
        assertEquals(64, manifest.get("size").getAsInt());
        NativeItemIcons icons = new NativeItemIcons(root, "26.3");
        var items = manifest.getAsJsonArray("items");
        assertFalse(items.isEmpty());
        for (var item : items) icons.read(item.getAsString());
        assertTrue(manifest.get("renderer").getAsString().equals("minecraft-inventory"));
    }

    @Test void nativePixelsAreUsedUnchangedForAnyItemId() throws Exception {
        manifest("26.3", "minecraft-inventory", "example:future_block");
        Path path = directory.resolve("example/future_block.png");
        Files.createDirectories(path.getParent());
        BufferedImage original = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        original.setRGB(5, 9, 0x7345ABCD);
        original.setRGB(7, 7, 0xFF123456);
        ImageIO.write(original, "png", path.toFile());
        BufferedImage exported = new NativeItemIcons(directory, "26.3").read("example:future_block");
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) assertEquals(original.getRGB(x, y), exported.getRGB(x, y));
    }

    @Test void missingExportsHaveAnActionableErrorInsteadOfAnApproximateFallback() {
        IOException error = assertThrows(IOException.class, () -> new NativeItemIcons(directory, "26.3").read("minecraft:chest"));
        assertTrue(error.getMessage().contains(":clientmod:exportItemIcons"));
    }

    @Test void exportsFromOtherMinecraftVersionsAndSoftwareRenderersAreRejected() throws Exception {
        manifest("26.2", "minecraft-inventory", "minecraft:chest");
        assertThrows(IOException.class, () -> new NativeItemIcons(directory, "26.3").read("minecraft:chest"));
        manifest("26.3", "approximate", "minecraft:chest");
        assertThrows(IOException.class, () -> new NativeItemIcons(directory, "26.3").read("minecraft:chest"));
    }

    @Test void missingAndIncorrectlySizedPngsAreRejected() throws Exception {
        manifest("26.3", "minecraft-inventory", "minecraft:chest");
        NativeItemIcons icons = new NativeItemIcons(directory, "26.3");
        assertThrows(IOException.class, () -> icons.read("minecraft:chest"));
        assertThrows(IOException.class, () -> icons.read("minecraft:bedrock"));
        Path path = directory.resolve("minecraft/chest.png");
        Files.createDirectories(path.getParent());
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), "png", path.toFile());
        assertThrows(IOException.class, () -> icons.read("minecraft:chest"));
    }

    private void manifest(String version, String renderer, String item) throws IOException {
        Files.writeString(directory.resolve("manifest.json"), """
                {"format":1,"minecraft":"%s","renderer":"%s","size":64,"items":["%s"]}
                """.formatted(version, renderer, item));
    }
}
