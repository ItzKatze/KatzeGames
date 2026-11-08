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

class NativeMobIconsTest {
    @TempDir Path directory;

    @Test void localPortraitExportIsCompleteWhenPresent() throws Exception {
        Path root = Path.of("src/main/mob-icons/26.3");
        assumeTrue(Files.exists(root.resolve("manifest.json")), "Local developer portrait cache is not present");
        var manifest = JsonParser.parseString(Files.readString(root.resolve("manifest.json"))).getAsJsonObject();
        NativeMobIcons portraits = new NativeMobIcons(root, "26.3");
        assertFalse(manifest.getAsJsonArray("items").isEmpty());
        for (var mob : manifest.getAsJsonArray("items")) portraits.read(mob.getAsString());
    }

    @Test void portraitReaderRejectsInventoryExportsAndIncorrectVersions() throws Exception {
        Files.writeString(directory.resolve("manifest.json"), """
                {"format":1,"minecraft":"26.3","renderer":"minecraft-inventory","size":64,"items":["minecraft:pig"]}
                """);
        assertThrows(IOException.class, () -> new NativeMobIcons(directory, "26.3").read("minecraft:pig"));
        Files.writeString(directory.resolve("manifest.json"), """
                {"format":1,"minecraft":"26.2","renderer":"minecraft-mob-portrait","size":128,"items":["minecraft:pig"]}
                """);
        assertThrows(IOException.class, () -> new NativeMobIcons(directory, "26.3").read("minecraft:pig"));
    }

    @Test void portraitsUseNativePixelsAndMissingExportsHaveAnActionableError() throws Exception {
        var error = assertThrows(IOException.class, () -> new NativeMobIcons(directory, "26.3").read("minecraft:pig"));
        assertTrue(error.getMessage().contains(":clientmod:exportMobIcons"));
        Files.writeString(directory.resolve("manifest.json"), """
                {"format":1,"minecraft":"26.3","renderer":"minecraft-mob-portrait","size":128,"items":["minecraft:pig"]}
                """);
        Path image = directory.resolve("minecraft/pig.png");
        Files.createDirectories(image.getParent());
        BufferedImage portrait = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        portrait.setRGB(40, 30, 0xC0806050);
        ImageIO.write(portrait, "png", image.toFile());
        assertEquals(portrait.getRGB(40, 30), new NativeMobIcons(directory, "26.3").read("minecraft:pig").getRGB(40, 30));
    }
}
