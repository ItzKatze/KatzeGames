import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.font.Font;
import team.unnamed.creative.font.FontProvider;
import team.unnamed.creative.metadata.pack.FormatVersion;
import team.unnamed.creative.metadata.pack.PackFormat;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter;
import team.unnamed.creative.serialize.minecraft.fs.FileTreeWriter;
import team.unnamed.creative.texture.Texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class BingoPackAssets {
    private static final FormatVersion VERSION = FormatVersion.of(97, 1);
    private final ResourcePack pack = ResourcePack.resourcePack();

    BingoPackAssets() { pack.packMeta(VERSION, Component.text("KatzeGames Bingo board (required)")); }

    static FontProvider bitmap(String texture, int height, int ascent, List<String> characters) {
        return FontProvider.bitMap().file(Key.key(texture)).height(height).ascent(ascent).characters(characters).build();
    }

    void font(String key, List<FontProvider> providers) { pack.font(Font.font(Key.key(key), providers)); }
    void texture(String key, Path image) { pack.texture(Texture.texture(Key.key(key), Writable.path(image))); }
    void texture(String key, BufferedImage image) throws IOException { pack.texture(Texture.texture(Key.key(key), png(image))); }
    void icon(BufferedImage image) throws IOException { pack.icon(png(image)); }

    void write(Path directory) throws IOException {
        try (FileTreeWriter target = FileTreeWriter.directory(directory.toFile(), false)) {
            MinecraftResourcePackWriter.builder().targetPackFormat(PackFormat.format(VERSION)).prettyPrinting(true).build().write(target, pack);
        }
        preserveMinorFormat(directory.resolve("pack.mcmeta"));
    }

    private void preserveMinorFormat(Path metadata) throws IOException {
        // Creative 1.15.4 drops minor versions when serializing pack metadata.
        var root = JsonParser.parseString(Files.readString(metadata, StandardCharsets.UTF_8)).getAsJsonObject();
        var section = root.getAsJsonObject("pack");
        JsonArray version = new JsonArray();
        version.add(VERSION.major());
        version.add(VERSION.minor());
        section.add("min_format", version);
        section.add("max_format", version.deepCopy());
        Files.writeString(metadata, new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
    }

    private Writable png(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return Writable.bytes(output.toByteArray());
    }
}
