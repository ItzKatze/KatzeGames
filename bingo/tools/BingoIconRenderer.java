import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipFile;

final class BingoIconRenderer {
    private final MinecraftAssets assets;
    private final MinecraftItemSprites sprites;
    private final NativeItemIcons items;

    BingoIconRenderer(ZipFile minecraft, Path exports) throws IOException {
        assets = new MinecraftAssets(minecraft);
        sprites = new MinecraftItemSprites(assets);
        items = new NativeItemIcons(exports, assets.json("version.json").get("id").getAsString());
    }

    BufferedImage render(String[] entry) throws IOException {
        return items.read("minecraft:" + material(entry).toLowerCase(Locale.ROOT));
    }

    MinecraftItemSprites.Sprite sprite(String[] entry) throws IOException {
        return sprites.resolve("minecraft:" + material(entry).toLowerCase(Locale.ROOT));
    }

    String material(String[] entry) throws IOException {
        if (entry[1].equals("MOB")) return entry.length > 13 && entry[13].equals("SIMPLIFIED") ? entry[5] : entry[3] + "_SPAWN_EGG";
        if (entry[1].equals("ITEM")) return entry[3];
        if (entry[1].equals("ADVANCEMENT")) {
            String id = assets.advancement(entry[3]).getAsJsonObject("display").getAsJsonObject("icon").get("id").getAsString();
            return id.substring(id.indexOf(':') + 1).toUpperCase(Locale.ROOT);
        }
        return entry[5];
    }

}
