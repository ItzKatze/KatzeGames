package gg.itzkatze.katze.mobarmy;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

final class SchematicTemplate {
    private static final int LOWERCASE_BLOCK_STATES_VERSION = 5006;
    private final int width;
    private final int height;
    private final int length;
    private final int dataVersion;
    private final String fingerprint;
    private final String[] palette;
    private final int[] blocks;
    private final Map<Integer, CompoundBinaryTag> blockEntities = new HashMap<>();

    private SchematicTemplate(CompoundBinaryTag root, byte[] file) throws IOException {
        int version = root.getInt("Version");
        if (version != 2 && version != 3) throw new IOException("Unsupported Sponge schematic version: " + version);
        width = Short.toUnsignedInt(root.getShort("Width"));
        height = Short.toUnsignedInt(root.getShort("Height"));
        length = Short.toUnsignedInt(root.getShort("Length"));
        long volume = (long) width * height * length;
        if (width < 3 || length < 3 || height < 3 || volume > 8_000_000) throw new IOException("Invalid arena dimensions");
        dataVersion = root.getInt("DataVersion");
        CompoundBinaryTag blockRoot = version == 3 ? root.getCompound("Blocks") : root;
        CompoundBinaryTag encodedPalette = blockRoot.getCompound("Palette");
        if (encodedPalette.isEmpty()) throw new IOException("Missing arena block palette");
        palette = new String[encodedPalette.size()];
        for (var entry : encodedPalette) {
            int id = encodedPalette.getInt(entry.getKey(), -1);
            if (id < 0 || id >= palette.length || palette[id] != null) throw new IOException("Invalid arena palette index");
            palette[id] = entry.getKey();
        }
        blocks = decode(blockRoot.getByteArray(version == 3 ? "Data" : "BlockData"), (int) volume, palette.length);
        for (BinaryTag tag : blockRoot.getList("BlockEntities", BinaryTagTypes.COMPOUND)) {
            CompoundBinaryTag entity = (CompoundBinaryTag) tag;
            int[] pos = entity.getIntArray("Pos");
            if (pos.length != 3 || !contains(pos[0], pos[1], pos[2])) throw new IOException("Invalid arena block entity position");
            String id = entity.getString("Id");
            if (id.isBlank()) throw new IOException("Missing arena block entity type");
            CompoundBinaryTag data = version == 3 ? entity.getCompound("Data") : entity.remove("Pos").remove("Id");
            blockEntities.put(offset(pos[0], pos[1], pos[2]), data.putString("id", id));
        }
        try { fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    static SchematicTemplate read(InputStream input) throws IOException {
        if (input == null) throw new IOException("Missing arena/village.schem");
        byte[] bytes = input.readAllBytes();
        CompoundBinaryTag root = BinaryTagIO.reader(32_000_000).read(new ByteArrayInputStream(bytes), BinaryTagIO.Compression.GZIP);
        if (root.contains("Schematic", BinaryTagTypes.COMPOUND)) root = root.getCompound("Schematic");
        return new SchematicTemplate(root, bytes);
    }

    int width() { return width; }
    int height() { return height; }
    int length() { return length; }
    String fingerprint() { return fingerprint; }
    int blockEntityCount() { return blockEntities.size(); }

    String block(int x, int y, int z) { return palette[blocks[offset(x, y, z)]]; }

    int roofLayer() {
        for (int y = height - 1; y >= 2; y--) {
            boolean roof = true;
            for (int z = 0; z < length && roof; z++) for (int x = 0; x < width; x++) {
                if (!block(x, y, z).startsWith("minecraft:barrier")) { roof = false; break; }
            }
            if (roof) return y;
        }
        throw new IllegalStateException("The MobArmy schematic must include its barrier roof");
    }

    List<Part> parts() throws IOException {
        List<Part> parts = new ArrayList<>();
        ListBinaryTag.Builder<CompoundBinaryTag> states = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);
        for (String state : palette) states.add(blockState(state));
        ListBinaryTag stateList = states.build();
        for (int z = 0; z < length; z += 16) for (int x = 0; x < width; x += 16) {
            int partWidth = Math.min(16, width - x);
            int partLength = Math.min(16, length - z);
            ListBinaryTag.Builder<CompoundBinaryTag> entries = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);
            for (int y = 0; y < height; y++) for (int dz = 0; dz < partLength; dz++) for (int dx = 0; dx < partWidth; dx++) {
                int offset = offset(x + dx, y, z + dz);
                var entry = CompoundBinaryTag.builder().putInt("state", blocks[offset]).put("pos", ints(dx, y, dz));
                CompoundBinaryTag entity = blockEntities.get(offset);
                if (entity != null) entry.put("nbt", entity);
                entries.add(entry.build());
            }
            CompoundBinaryTag structure = CompoundBinaryTag.builder().putInt("DataVersion", dataVersion)
                    .put("size", ints(partWidth, height, partLength)).put("palette", stateList)
                    .put("blocks", entries.build()).put("entities", ListBinaryTag.empty()).build();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            BinaryTagIO.writer().write(structure, output, BinaryTagIO.Compression.GZIP);
            parts.add(new Part(x, z, output.toByteArray()));
        }
        return List.copyOf(parts);
    }

    private CompoundBinaryTag blockState(String state) throws IOException {
        int bracket = state.indexOf('[');
        String name = bracket < 0 ? state : state.substring(0, bracket);
        boolean lowercase = dataVersion >= LOWERCASE_BLOCK_STATES_VERSION;
        var result = CompoundBinaryTag.builder().putString(lowercase ? "id" : "Name", name);
        if (bracket >= 0) {
            if (!state.endsWith("]")) throw new IOException("Invalid block state: " + state);
            var properties = CompoundBinaryTag.builder();
            for (String property : state.substring(bracket + 1, state.length() - 1).split(",")) {
                String[] pair = property.split("=", 2);
                if (pair.length != 2) throw new IOException("Invalid block property: " + state);
                properties.putString(pair[0], pair[1]);
            }
            result.put(lowercase ? "properties" : "Properties", properties.build());
        }
        return result.build();
    }

    private boolean contains(int x, int y, int z) { return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < length; }
    private int offset(int x, int y, int z) { return (y * length + z) * width + x; }
    private static ListBinaryTag ints(int... values) {
        var list = ListBinaryTag.builder(BinaryTagTypes.INT);
        for (int value : values) list.add(IntBinaryTag.intBinaryTag(value));
        return list.build();
    }

    private static int[] decode(byte[] bytes, int count, int paletteSize) throws IOException {
        int[] result = new int[count];
        int cursor = 0;
        for (int index = 0; index < count; index++) {
            int value = 0;
            int shift = 0;
            int next;
            do {
                if (cursor >= bytes.length || shift >= 35) throw new IOException("Truncated or invalid arena block data");
                next = bytes[cursor++] & 255;
                value |= (next & 127) << shift;
                shift += 7;
            } while ((next & 128) != 0);
            if (value < 0 || value >= paletteSize) throw new IOException("Unknown arena palette entry");
            result[index] = value;
        }
        if (cursor != bytes.length) throw new IOException("Unexpected trailing arena block data");
        return result;
    }

    record Part(int x, int z, byte[] nbt) {}
}
