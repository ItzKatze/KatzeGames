package gg.itzkatze.katze.bingo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.itzkatze.katze.bingo.board.BingoObjective;
import gg.itzkatze.katze.bingo.board.BingoObjectiveCatalog;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Locale;
import java.util.zip.ZipInputStream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BingoPackTest {
    @Test void nativeSpritesAreReferencedDirectlyAndOwnershipDoesNotReplaceTheItemPictures() throws Exception {
        ImageIO.setUseCache(false);
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(getClass().getResourceAsStream("/bingo/KatzeGames-Bingo.zip"))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) if (!entry.isDirectory()) files.put(entry.getName(), zip.readAllBytes());
        }
        JsonObject pack = json(files.get("pack.mcmeta")).getAsJsonObject("pack");
        assertEquals(97, pack.getAsJsonArray("min_format").get(0).getAsInt());
        assertEquals(1, pack.getAsJsonArray("min_format").get(1).getAsInt());
        assertTrue(files.keySet().stream().noneMatch(path -> path.startsWith("assets/minecraft/")));
        assertTrue(files.keySet().stream().noneMatch(path -> path.startsWith("assets/katzegames/items/") || path.startsWith("assets/katzegames/models/item/")));
        assertFalse(files.containsKey("assets/katzegames/textures/bingo/objectives.png"));
        var cells = ImageIO.read(new ByteArrayInputStream(files.get("assets/katzegames/textures/bingo/cells.png")));
        assertEquals(5 * BingoHudLayout.CELL_SIZE, cells.getWidth());
        assertEquals(BingoHudLayout.CELL_SIZE, cells.getHeight());
        assertFalse(files.containsKey("assets/katzegames/textures/bingo/rows.png"));
        var columnLetters = json(files.get("assets/katzegames/font/bingo/layout.json")).getAsJsonArray("providers").get(0).getAsJsonObject();
        assertEquals("minecraft:font/ascii.png", texture(columnLetters));
        assertEquals(8, columnLetters.has("height") ? columnLetters.get("height").getAsInt() : 8);
        assertEquals(7, columnLetters.get("ascent").getAsInt());
        BingoObjectiveCatalog catalog = new BingoObjectiveCatalog();
        for (int row = 0; row < 5; row++) {
            var providers = json(files.get("assets/katzegames/font/bingo/row_" + row + ".json")).getAsJsonArray("providers");
            assertEquals(catalog.all().size() + 3, providers.size());
            assertEquals(BingoHudLayout.ASCENT - row * BingoHudLayout.ROW_PITCH, providers.get(0).getAsJsonObject().get("ascent").getAsInt());
            var numbers = providers.get(1).getAsJsonObject();
            assertEquals("minecraft:font/ascii.png", texture(numbers));
            assertEquals(8, numbers.has("height") ? numbers.get("height").getAsInt() : 8);
            assertEquals(BingoHudLayout.ASCENT - 5 - row * BingoHudLayout.ROW_PITCH, numbers.get("ascent").getAsInt());
            var advances = providers.get(providers.size() - 1).getAsJsonObject().getAsJsonObject("advances");
            assertEquals(-BingoHudLayout.ROW_ADVANCE, advances.get("\uE000").getAsInt());
            assertEquals(-BingoHudLayout.CELL_SIZE, advances.get("\uE010").getAsInt());
            assertEquals(3, advances.get("\uE011").getAsInt() + advances.get("\uE012").getAsInt());
            int originalSprites = 0;
            var referencedIcons = new HashSet<String>();
            var referencedMobs = new HashSet<String>();
            for (int index = 0; index < catalog.all().size(); index++) {
                BingoObjective objective = catalog.all().get(index);
                JsonObject provider = providers.get(index + 2).getAsJsonObject();
                String texture = texture(provider);
                assertEquals(BingoHudLayout.ICON_SIZE, provider.get("height").getAsInt());
                assertEquals(BingoHudLayout.ASCENT - 1 - row * BingoHudLayout.ROW_PITCH, provider.get("ascent").getAsInt());
                assertEquals(0xF0000 + index, provider.getAsJsonArray("chars").get(0).getAsString().codePointAt(0));
                int padding = advances.get(new String(Character.toChars(0xF4000 + index))).getAsInt();
                assertTrue(padding >= 1 && padding <= BingoHudLayout.CELL_SIZE);
                if (texture.startsWith("minecraft:")) {
                    originalSprites++;
                    assertFalse(files.containsKey("assets/katzegames/textures/bingo/icons/" + objective.id() + ".png"));
                } else if (objective.category() == BingoObjective.Category.MOB) {
                    String mob = catalog.icon(objective.id()).name().toLowerCase(Locale.ROOT).replace("_spawn_egg", "");
                    assertEquals("katzegames:bingo/mobs/" + mob + ".png", texture);
                    String packedPath = "assets/katzegames/textures/bingo/mobs/" + mob + ".png";
                    referencedMobs.add(packedPath);
                    byte[] picture = files.get(packedPath);
                    assertNotNull(picture, "Missing mob portrait: " + texture);
                    var packed = ImageIO.read(new ByteArrayInputStream(picture));
                    assertEquals(128, packed.getWidth());
                    assertEquals(128, packed.getHeight());
                    assertTrue(packed.getColorModel().hasAlpha());
                    assertEquals(0, packed.getRGB(0, 0) >>> 24);
                    boolean visible = false;
                    for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) visible |= packed.getRGB(x, y) >>> 24 != 0;
                    assertTrue(visible, "Empty mob portrait: " + mob);
                    Path exported = Path.of("src/main/mob-icons/26.3/minecraft/" + mob + ".png");
                    if (Files.exists(exported)) assertArrayEquals(Files.readAllBytes(exported), picture);
                } else {
                    String packedPath = "assets/katzegames/textures/" + texture.substring(texture.indexOf(':') + 1);
                    referencedIcons.add(packedPath);
                    byte[] picture = files.get(packedPath);
                    assertNotNull(picture, "Missing fallback image: " + texture);
                    var packed = ImageIO.read(new ByteArrayInputStream(picture));
                    String material = catalog.icon(objective.id()).name().toLowerCase(Locale.ROOT);
                    assertEquals("katzegames:bingo/icons/" + material + ".png", texture);
                    if (Files.isDirectory(Path.of("src/main/item-icons/26.3")))
                        assertArrayEquals(Files.readAllBytes(Path.of("src/main/item-icons/26.3/minecraft/" + material + ".png")), picture);
                    assertEquals(64, packed.getWidth());
                    assertEquals(64, packed.getHeight());
                    assertTrue(packed.getColorModel().hasAlpha());
                }
                if (objective.id().equals("diamond")) assertEquals("minecraft:item/diamond.png", texture);
                if (objective.category() == BingoObjective.Category.MOB) {
                    assertTrue(catalog.icon(objective.id()).name().endsWith("_SPAWN_EGG"));
                    assertTrue(texture.startsWith("katzegames:bingo/mobs/") && !texture.endsWith("_spawn_egg.png"));
                }
            }
            assertTrue(originalSprites > 0);
            assertFalse(referencedMobs.isEmpty());
            assertEquals(referencedMobs, files.keySet().stream().filter(path -> path.startsWith("assets/katzegames/textures/bingo/mobs/")).collect(Collectors.toSet()));
            assertEquals(referencedIcons, files.keySet().stream().filter(path -> path.startsWith("assets/katzegames/textures/bingo/icons/")).collect(Collectors.toSet()));
        }
    }

    private JsonObject json(byte[] bytes) { return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject(); }

    private String texture(JsonObject provider) {
        String location = provider.get("file").getAsString();
        return location.contains(":") ? location : "minecraft:" + location;
    }
}
