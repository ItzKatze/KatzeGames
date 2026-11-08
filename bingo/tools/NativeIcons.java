import com.google.gson.Gson;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class NativeIcons {
    private final Path directory;
    private final String minecraft;
    private Set<String> items;
    private final String renderer;
    private final int size;
    private final String task;

    private record Manifest(int format, String minecraft, String renderer, int size, List<String> items) {}

    NativeIcons(Path directory, String minecraft, String renderer, int size, String task) {
        this.directory = directory.toAbsolutePath().normalize();
        this.minecraft = minecraft;
        this.renderer = renderer;
        this.size = size;
        this.task = task;
    }

    BufferedImage read(String id) throws IOException {
        loadManifest();
        id = MinecraftAssets.location(id);
        if (!items.contains(id)) throw new IOException("Native icon not exported: " + id + ". Run " + task + ".");
        String[] parts = id.split(":", 2);
        Path file = directory.resolve(parts[0]).resolve(parts[1] + ".png").normalize();
        if (!file.startsWith(directory)) throw new IOException("Invalid native icon path: " + id);
        if (!Files.isRegularFile(file)) throw new IOException("Missing native PNG: " + file);
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null || image.getWidth() != size || image.getHeight() != size || !image.getColorModel().hasAlpha())
            throw new IOException("Native icon must be a transparent " + size + "x" + size + " PNG: " + file);
        return image;
    }

    private void loadManifest() throws IOException {
        if (items != null) return;
        Path file = directory.resolve("manifest.json");
        if (!Files.isRegularFile(file)) throw new IOException("Missing native Minecraft export: " + file + ". Run " + task + " first.");
        try (var reader = Files.newBufferedReader(file)) {
            Manifest manifest = new Gson().fromJson(reader, Manifest.class);
            if (manifest == null || manifest.format() != 1 || !minecraft.equals(manifest.minecraft())
                    || !renderer.equals(manifest.renderer()) || manifest.size() != size || manifest.items() == null)
                throw new IOException("Native icons do not match Minecraft " + minecraft + "; regenerate with " + task + ".");
            items = new HashSet<>(manifest.items());
        } catch (RuntimeException exception) { throw new IOException("Invalid native export manifest: " + file, exception); }
    }
}
