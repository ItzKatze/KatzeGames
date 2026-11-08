import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import team.unnamed.creative.font.FontProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreativePackCompatibilityTest {
    @TempDir Path directory;

    @Test void writesBingoGlyphsSpacingTexturesAndExactMinorPackVersion() throws Exception {
        Path source = directory.resolve("native.png");
        BufferedImage icon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        icon.setRGB(11, 17, 0x8945ABCD);
        ImageIO.write(icon, "png", source.toFile());
        Path target = directory.resolve("pack");
        Files.createDirectories(target);
        Files.writeString(target.resolve("keep.txt"), "preserve existing generated assets");

        BingoPackAssets assets = new BingoPackAssets();
        assets.texture("katzegames:bingo/icons/native.png", source);
        assets.texture("katzegames:bingo/cells.png", new BufferedImage(90, 18, BufferedImage.TYPE_INT_ARGB));
        assets.font("katzegames:bingo/row_4", List.of(
                BingoPackAssets.bitmap("katzegames:bingo/icons/native.png", 16, -70, List.of(Character.toString(0xF0000 + 2050))),
                FontProvider.space().advance(0xE000, -104).advance(0xF4000 + 2050, -17).build()));
        assets.font("katzegames:bingo/layout", List.of(BingoPackAssets.bitmap("minecraft:font/ascii.png", 8, 7, List.of("ABCDE"))));
        assets.write(target);

        var metadata = json(target.resolve("pack.mcmeta")).getAsJsonObject("pack");
        for (String bound : List.of("min_format", "max_format")) {
            assertEquals(97, metadata.getAsJsonArray(bound).get(0).getAsInt());
            assertEquals(1, metadata.getAsJsonArray(bound).get(1).getAsInt());
        }
        var providers = json(target.resolve("assets/katzegames/font/bingo/row_4.json")).getAsJsonArray("providers");
        var glyph = providers.get(0).getAsJsonObject();
        assertEquals(-70, glyph.get("ascent").getAsInt());
        assertEquals(16, glyph.get("height").getAsInt());
        String character = glyph.getAsJsonArray("chars").get(0).getAsString();
        assertEquals(1, character.codePointCount(0, character.length()));
        assertEquals(0xF0000 + 2050, character.codePointAt(0));
        var advances = providers.get(1).getAsJsonObject().getAsJsonObject("advances");
        assertEquals(-104, advances.get("\uE000").getAsInt());
        assertEquals(-17, advances.get(Character.toString(0xF4000 + 2050)).getAsInt());
        assertArrayEquals(Files.readAllBytes(source), Files.readAllBytes(target.resolve("assets/katzegames/textures/bingo/icons/native.png")));
        assertEquals(90, ImageIO.read(target.resolve("assets/katzegames/textures/bingo/cells.png").toFile()).getWidth());
        assertFalse(Files.exists(target.resolve("assets/minecraft")));
        assertTrue(Files.exists(target.resolve("keep.txt")));
    }

    private JsonObject json(Path file) throws Exception { return JsonParser.parseString(Files.readString(file)).getAsJsonObject(); }
}
