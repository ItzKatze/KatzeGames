package gg.itzkatze.katze.mobarmy;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SchematicTemplateTest {
    @Test void suppliedVillageHasTheExpectedDimensionsAndExistingBarrierRoof() throws Exception {
        try (var input = getClass().getResourceAsStream("/arena/village.schem")) {
            var template = SchematicTemplate.read(input);
            assertEquals(87, template.width());
            assertEquals(97, template.length());
            assertEquals(47, template.height());
            assertEquals(43, template.roofLayer());
            assertEquals(1, template.blockEntityCount());
            assertEquals("minecraft:dirt_path", template.block(43, 13, 48));
            assertEquals("minecraft:air", template.block(43, 14, 48));
            assertEquals(64, template.fingerprint().length());
        }
    }

    @Test void nativePartsPreserveEveryBlockIncludingAirAndChestData() throws Exception {
        try (var input = getClass().getResourceAsStream("/arena/village.schem")) {
            var template = SchematicTemplate.read(input);
            var parts = template.parts();
            assertEquals(42, parts.size());
            int count = 0;
            int chests = 0;
            for (var part : parts) {
                var root = BinaryTagIO.reader(8_000_000).read(new ByteArrayInputStream(part.nbt()), BinaryTagIO.Compression.GZIP);
                var size = root.getList("size");
                assertTrue(size.getInt(0) > 0 && size.getInt(0) <= 16);
                assertEquals(47, size.getInt(1));
                assertTrue(size.getInt(2) > 0 && size.getInt(2) <= 16);
                assertEquals(130, root.getList("palette").size());
                for (BinaryTag tag : root.getList("blocks", BinaryTagTypes.COMPOUND)) {
                    var block = (CompoundBinaryTag) tag;
                    var pos = block.getList("pos");
                    assertTrue(pos.getInt(0) >= 0 && pos.getInt(0) < size.getInt(0));
                    assertTrue(pos.getInt(2) >= 0 && pos.getInt(2) < size.getInt(2));
                    int state = block.getInt("state");
                    var palette = root.getList("palette").getCompound(state);
                    String source = template.block(part.x() + pos.getInt(0), pos.getInt(1), part.z() + pos.getInt(2));
                    assertEquals(source.split("\\[", 2)[0], palette.getString("id"));
                    assertFalse(palette.contains("Name"));
                    assertFalse(palette.contains("Properties"));
                    var nbt = block.getCompound("nbt");
                    if (nbt.getString("id").equals("minecraft:chest")) {
                        chests++;
                        assertEquals(78, part.x() + pos.getInt(0));
                        assertEquals(21, pos.getInt(1));
                        assertEquals(24, part.z() + pos.getInt(2));
                        assertTrue(nbt.contains("Items"));
                    }
                    count++;
                }
            }
            assertEquals(87 * 47 * 97, count);
            assertEquals(1, chests);
        }
    }

    @Test void blockStateFieldNamesMatchTheSchematicDataVersion() throws Exception {
        for (int version : new int[]{4325, 5005, 5006, 5023}) {
            var root = CompoundBinaryTag.builder().putInt("Version", 2).putInt("DataVersion", version)
                    .putShort("Width", (short) 3).putShort("Height", (short) 3).putShort("Length", (short) 3)
                    .put("Palette", CompoundBinaryTag.builder().putInt("minecraft:oak_log[axis=x]", 0).build())
                    .putByteArray("BlockData", new byte[27]).build();
            var template = SchematicTemplate.read(encoded(root));
            var part = template.parts().getFirst();
            var nativeRoot = BinaryTagIO.reader(8_000_000).read(new ByteArrayInputStream(part.nbt()), BinaryTagIO.Compression.GZIP);
            var state = nativeRoot.getList("palette").getCompound(0);
            String name = version >= 5006 ? "id" : "Name";
            String properties = version >= 5006 ? "properties" : "Properties";
            assertEquals(version, nativeRoot.getInt("DataVersion"));
            assertEquals("minecraft:oak_log", state.getString(name));
            assertEquals("x", state.getCompound(properties).getString("axis"));
        }
    }

    @Test void spongeThreeLoadsButDoesNotInventAMissingRoof() throws Exception {
        var blocks = CompoundBinaryTag.builder().put("Palette", CompoundBinaryTag.builder().putInt("minecraft:stone", 0).build())
                .putByteArray("Data", new byte[27]).build();
        var root = CompoundBinaryTag.builder().putInt("Version", 3).putInt("DataVersion", 5023)
                .putShort("Width", (short) 3).putShort("Height", (short) 3).putShort("Length", (short) 3).put("Blocks", blocks).build();
        var template = SchematicTemplate.read(encoded(root));
        assertThrows(IllegalStateException.class, template::roofLayer);
        assertEquals("minecraft:stone", template.block(2, 2, 2));
    }

    @Test void corruptBlockDataAndMissingAssetsFailBeforePlacement() throws Exception {
        assertThrows(IOException.class, () -> SchematicTemplate.read(null));
        var root = CompoundBinaryTag.builder().putInt("Version", 2).putShort("Width", (short) 3)
                .putShort("Height", (short) 3).putShort("Length", (short) 3)
                .put("Palette", CompoundBinaryTag.builder().putInt("minecraft:air", 0).build()).putByteArray("BlockData", new byte[2]).build();
        assertThrows(IOException.class, () -> SchematicTemplate.read(encoded(root)));
    }

    private static ByteArrayInputStream encoded(CompoundBinaryTag root) throws IOException {
        var output = new ByteArrayOutputStream();
        BinaryTagIO.writer().write(root, output, BinaryTagIO.Compression.GZIP);
        return new ByteArrayInputStream(output.toByteArray());
    }
}
