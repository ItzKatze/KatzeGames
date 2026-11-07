import com.google.gson.Gson;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class NativeItemIcons {
    private final Path directory;
    private final String minecraft;
    private Set<String> items;

    private record Manifest(int format, String minecraft, String renderer, int size, List<String> items) {}

    NativeItemIcons(Path directory, String minecraft) {
        this.directory = directory.toAbsolutePath().normalize();
        this.minecraft = minecraft;
    }

    BufferedImage read(String id) throws IOException {
        loadManifest();
        id = MinecraftAssets.location(id);
        if (!items.contains(id)) throw new IOException("Native item icon not exported: " + id + ". Run :clientmod:exportItemIcons.");
        String[] parts = id.split(":", 2);
        Path file = directory.resolve(parts[0]).resolve(parts[1] + ".png").normalize();
        if (!file.startsWith(directory)) throw new IOException("Invalid item icon path: " + id);
        if (!Files.isRegularFile(file)) throw new IOException("Missing native item PNG: " + file);
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null || image.getWidth() != 64 || image.getHeight() != 64 || !image.getColorModel().hasAlpha())
            throw new IOException("Native item icon must be a transparent 64x64 PNG: " + file);
        return image;
    }

    private void loadManifest() throws IOException {
        if (items != null) return;
        Path file = directory.resolve("manifest.json");
        if (!Files.isRegularFile(file)) throw new IOException("Missing native Minecraft item export: " + file + ". Run :clientmod:exportItemIcons first.");
        try (var reader = Files.newBufferedReader(file)) {
            Manifest manifest = new Gson().fromJson(reader, Manifest.class);
            if (manifest == null || manifest.format() != 1 || !minecraft.equals(manifest.minecraft())
                    || !"minecraft-inventory".equals(manifest.renderer()) || manifest.size() != 64 || manifest.items() == null)
                throw new IOException("Item icons do not match Minecraft " + minecraft + "; regenerate with :clientmod:exportItemIcons.");
            items = new HashSet<>(manifest.items());
        } catch (RuntimeException exception) { throw new IOException("Invalid native item export manifest: " + file, exception); }
    }
}
