import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class MinecraftItemSpritesTest {
    @TempDir Path directory;

    @Test void directSpritesResolveFromArbitraryInheritedModels() throws Exception {
        Map<String, byte[]> assets = base();
        json(assets, "assets/example/items/future_item.json", """
                {"model":{"type":"minecraft:model","model":"example:item/child"}}
                """);
        json(assets, "assets/example/models/item/parent.json", """
                {"parent":"minecraft:item/generated","textures":{"layer0":"#surface"}}
                """);
        json(assets, "assets/example/models/item/child.json", """
                {"parent":"example:item/parent","textures":{"surface":{"sprite":"example:item/original"}}}
                """);
        var sprite = resolve(assets, "example:future_item");
        assertNotNull(sprite);
        assertEquals("example:item/original.png", sprite.texture());
        assertEquals(16, sprite.image().getWidth());
        assertEquals(0xFF12AB34, sprite.image().getRGB(8, 8));
    }

    @Test void threeDimensionalAndSpecialModelsAreLeftToTheNativeExporter() throws Exception {
        Map<String, byte[]> assets = base();
        json(assets, "assets/example/items/block.json", """
                {"model":{"type":"minecraft:model","model":"example:block/block"}}
                """);
        json(assets, "assets/example/models/block/block.json", "{\"elements\":[]}");
        assertNull(resolve(assets, "example:block"));
        json(assets, "assets/example/items/special.json", """
                {"model":{"type":"minecraft:special","model":{"type":"example:any_special_type"}}}
                """);
        assertNull(resolve(assets, "example:special"));
    }

    @Test void parentCyclesFailWithoutAStackOverflow() throws Exception {
        Map<String, byte[]> assets = base();
        json(assets, "assets/example/items/cycle.json", "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"example:item/a\"}}");
        json(assets, "assets/example/models/item/a.json", "{\"parent\":\"example:item/b\"}");
        json(assets, "assets/example/models/item/b.json", "{\"parent\":\"example:item/a\"}");
        assertThrows(IOException.class, () -> resolve(assets, "example:cycle"));
    }

    private Map<String, byte[]> base() throws IOException {
        Map<String, byte[]> assets = new LinkedHashMap<>();
        json(assets, "assets/minecraft/models/item/generated.json", "{\"parent\":\"builtin/generated\"}");
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(8, 8, 0xFF12AB34);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        assets.put("assets/example/textures/item/original.png", bytes.toByteArray());
        return assets;
    }

    private void json(Map<String, byte[]> assets, String path, String text) { assets.put(path, text.getBytes(StandardCharsets.UTF_8)); }

    private MinecraftItemSprites.Sprite resolve(Map<String, byte[]> assets, String id) throws IOException {
        Path path = directory.resolve("client.jar");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
            for (var entry : assets.entrySet()) { zip.putNextEntry(new ZipEntry(entry.getKey())); zip.write(entry.getValue()); zip.closeEntry(); }
        }
        try (ZipFile zip = new ZipFile(path.toFile())) { return new MinecraftItemSprites(new MinecraftAssets(zip)).resolve(id); }
    }
}
