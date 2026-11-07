import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

final class MinecraftAssets {
    private final ZipFile archive;
    private final Map<String, JsonObject> json = new HashMap<>();
    private final Map<String, BufferedImage> textures = new HashMap<>();

    MinecraftAssets(ZipFile archive) { this.archive = archive; }

    static String location(String value) { return value.contains(":") ? value : "minecraft:" + value; }

    private String path(String directory, String location, String suffix) {
        String[] parts = location(location).split(":", 2);
        return "assets/" + parts[0] + "/" + directory + "/" + parts[1] + suffix;
    }

    JsonObject item(String id) throws IOException { return json(path("items", id, ".json")); }
    JsonObject model(String id) throws IOException { return json(path("models", id, ".json")); }
    JsonObject advancement(String id) throws IOException {
        String[] parts = location(id).split(":", 2);
        return json("data/" + parts[0] + "/advancement/" + parts[1] + ".json");
    }

    BufferedImage staticSprite(String location) throws IOException {
        String metadata = path("textures", location, ".png.mcmeta");
        if (archive.getEntry(metadata) != null && json(metadata).has("animation")) return null;
        BufferedImage image = texture(location);
        return image.getWidth() == image.getHeight() ? image : null;
    }

    JsonObject json(String path) throws IOException {
        if (json.containsKey(path)) return json.get(path);
        var entry = archive.getEntry(path);
        if (entry == null) throw new IOException("Missing Minecraft asset: " + path);
        try (var reader = new InputStreamReader(archive.getInputStream(entry), StandardCharsets.UTF_8)) {
            JsonObject value = JsonParser.parseReader(reader).getAsJsonObject();
            json.put(path, value);
            return value;
        }
    }

    BufferedImage texture(String location) throws IOException {
        String path = path("textures", location, ".png");
        if (textures.containsKey(path)) return textures.get(path);
        var entry = archive.getEntry(path);
        if (entry == null) throw new IOException("Missing Minecraft texture: " + path);
        try (var stream = archive.getInputStream(entry)) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) throw new IOException("Invalid Minecraft texture: " + path);
            int width = image.getWidth();
            int height = image.getHeight();
            var metadata = archive.getEntry(path + ".mcmeta");
            if (metadata != null) {
                JsonObject document = json(path + ".mcmeta");
                if (document.has("animation")) {
                    JsonObject animation = document.getAsJsonObject("animation");
                    int frameWidth = animation.has("width") ? animation.get("width").getAsInt() : animation.has("height") ? width : Math.min(width, height);
                    int frameHeight = animation.has("height") ? animation.get("height").getAsInt() : animation.has("width") ? height : Math.min(width, height);
                    int frame = 0;
                    if (animation.has("frames") && !animation.getAsJsonArray("frames").isEmpty()) {
                        var first = animation.getAsJsonArray("frames").get(0);
                        frame = first.isJsonObject() ? first.getAsJsonObject().get("index").getAsInt() : first.getAsInt();
                    }
                    image = image.getSubimage(frame % (width / frameWidth) * frameWidth, frame / (width / frameWidth) * frameHeight, frameWidth, frameHeight);
                }
            }
            textures.put(path, image);
            return image;
        }
    }
}
