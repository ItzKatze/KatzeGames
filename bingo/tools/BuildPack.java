import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

public final class BuildPack {
    private static final int ROW_PITCH = 19;
    private record Icon(String texture, int advance) {}

    private static JsonObject bitmap(String texture, int height, int ascent, JsonArray characters) {
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "bitmap");
        provider.addProperty("file", texture);
        provider.addProperty("height", height);
        provider.addProperty("ascent", ascent);
        provider.add("chars", characters);
        return provider;
    }

    private static JsonArray characters(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) array.add(value);
        return array;
    }

    private static int advance(BufferedImage image) {
        for (int x = image.getWidth() - 1; x >= 0; x--) for (int y = 0; y < image.getHeight(); y++) {
            if ((image.getRGB(x, y) >>> 24) != 0) return Math.round((x + 1) * 16.0f / image.getHeight()) + 1;
        }
        return 1;
    }

    public static void main(String[] args) throws Exception {
        Path module = Path.of(args[0]);
        try (ZipFile minecraft = new ZipFile(args[1])) {
            String version = new MinecraftAssets(minecraft).json("version.json").get("id").getAsString();
            BuildObjectives.generate(module, minecraft, args.length > 2 ? Path.of(args[2]) : module.resolve("src/main/item-icons/" + version));
        }
        List<String[]> entries = Files.readAllLines(module.resolve("src/main/resources/bingo/objectives.tsv"), StandardCharsets.UTF_8)
                .stream().map(line -> line.split("\t", -1)).toList();
        Path pack = module.resolve("src/main/pack");
        Path textures = pack.resolve("assets/katzegames/textures/bingo");
        Path fonts = pack.resolve("assets/katzegames/font/bingo");
        Files.createDirectories(textures);
        Files.createDirectories(fonts);
        Path icons = textures.resolve("icons");
        Files.createDirectories(icons);
        BufferedImage cells = new BufferedImage(90, 18, BufferedImage.TYPE_INT_ARGB);
        Graphics2D cellGraphics = cells.createGraphics();
        Color[] backgrounds = {new Color(35, 40, 51), new Color(28, 78, 145), new Color(161, 65, 29), new Color(28, 78, 145), new Color(161, 65, 29)};
        StringBuilder backgroundCharacters = new StringBuilder();
        for (int state = 0; state < 5; state++) {
            int x = state * 18;
            cellGraphics.setColor(backgrounds[state]);
            cellGraphics.fillRect(x, 0, 18, 18);
            cellGraphics.setColor(state == 0 ? new Color(91, 100, 119) : state == 1 || state == 3 ? new Color(96, 181, 255) : new Color(255, 158, 92));
            for (int edge = 0; edge < 18; edge++) if (state != 2 && state != 4 || edge % 3 != 2) {
                cellGraphics.fillRect(x + edge, 0, 1, 1);
                cellGraphics.fillRect(x + edge, 17, 1, 1);
                cellGraphics.fillRect(x, edge, 1, 1);
                cellGraphics.fillRect(x + 17, edge, 1, 1);
            }
            if (state >= 3) {
                cellGraphics.setColor(new Color(255, 218, 73));
                cellGraphics.drawRect(x, 0, 17, 17);
            }
            backgroundCharacters.append((char) (0xE020 + state));
        }
        cellGraphics.dispose();
        ImageIO.write(cells, "png", textures.resolve("cells.png").toFile());
        Files.deleteIfExists(textures.resolve("objectives.png"));
        BufferedImage atlas = new BufferedImage(90, entries.size() * 18, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        List<Icon> iconSources = new ArrayList<>();
        StringBuilder iconMaterials = new StringBuilder();
        int nativeSprites = 0;
        Set<Path> usedIcons = new HashSet<>();
        JsonArray numberCharacters;
        try (ZipFile minecraft = new ZipFile(args[1])) {
            var defaultFont = new MinecraftAssets(minecraft).json("assets/minecraft/font/include/default.json");
            numberCharacters = defaultFont.getAsJsonArray("providers").asList().stream().map(provider -> provider.getAsJsonObject())
                    .filter(provider -> provider.has("file") && provider.get("file").getAsString().equals("minecraft:font/ascii.png"))
                    .findFirst().orElseThrow().getAsJsonArray("chars");
            String minecraftVersion = new MinecraftAssets(minecraft).json("version.json").get("id").getAsString();
            Path exports = args.length > 2 ? Path.of(args[2]) : module.resolve("src/main/item-icons/" + minecraftVersion);
            BingoIconRenderer renderer = new BingoIconRenderer(minecraft, exports);
            for (int index = 0; index < entries.size(); index++) {
                String[] entry = entries.get(index);
                MinecraftItemSprites.Sprite sprite;
                BufferedImage icon;
                try {
                    sprite = renderer.sprite(entry);
                    icon = sprite == null ? renderer.render(entry) : sprite.image();
                } catch (Exception exception) {
                    throw new IllegalStateException("Unable to resolve Bingo objective: " + entry[0], exception);
                }
                String texture;
                if (sprite == null) {
                    String filename = renderer.material(entry).toLowerCase(Locale.ROOT) + ".png";
                    texture = "katzegames:bingo/icons/" + filename;
                    Path destination = icons.resolve(filename);
                    if (usedIcons.add(destination)) Files.copy(exports.resolve("minecraft").resolve(filename), destination, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    texture = sprite.texture();
                    nativeSprites++;
                }
                iconSources.add(new Icon(texture, advance(icon)));
                iconMaterials.append(entry[0]).append('=').append(renderer.material(entry)).append('\n');
                for (int state = 0; state < 5; state++) {
                    graphics.drawImage(cells, state * 18, index * 18, state * 18 + 18, index * 18 + 18, state * 18, 0, state * 18 + 18, 18, null);
                    graphics.drawImage(icon, state * 18 + 1, index * 18 + 1, 16, 16, null);
                }
            }
        }
        try (var files = Files.list(icons)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".png") && Files.isRegularFile(path) && !usedIcons.contains(path)).toList()) Files.delete(file);
        }
        graphics.dispose();
        Path iconMetadata = module.resolve("src/main/resources/bingo/icons.properties");
        if (!Files.exists(iconMetadata) || !Files.readString(iconMetadata, StandardCharsets.UTF_8).equals(iconMaterials.toString()))
            Files.writeString(iconMetadata, iconMaterials, StandardCharsets.UTF_8);
        Files.deleteIfExists(textures.resolve("rows.png"));
        var gson = new GsonBuilder().setPrettyPrinting().create();
        for (int row = 0; row < 5; row++) {
            JsonArray providers = new JsonArray();
            providers.add(bitmap("katzegames:bingo/cells.png", 18, 7 - row * ROW_PITCH, characters(backgroundCharacters.toString())));
            providers.add(bitmap("minecraft:font/ascii.png", 8, 2 - row * ROW_PITCH, numberCharacters));
            JsonObject spaces = new JsonObject();
            spaces.addProperty("type", "space");
            JsonObject advances = new JsonObject();
            advances.addProperty("\uE000", -104);
            advances.addProperty("\uE010", -18);
            advances.addProperty("\uE011", 2);
            advances.addProperty("\uE012", 1);
            for (int index = 0; index < iconSources.size(); index++) {
                Icon icon = iconSources.get(index);
                advances.addProperty(new String(Character.toChars(0xF4000 + index)), 18 - icon.advance());
                providers.add(bitmap(icon.texture(), 16, 6 - row * ROW_PITCH, characters(new String(Character.toChars(0xF0000 + index)))));
            }
            spaces.add("advances", advances);
            providers.add(spaces);
            JsonObject font = new JsonObject();
            font.add("providers", providers);
            Files.writeString(fonts.resolve("row_" + row + ".json"), gson.toJson(font));
        }
        JsonArray layoutProviders = new JsonArray();
        layoutProviders.add(bitmap("minecraft:font/ascii.png", 8, 7, numberCharacters));
        JsonObject layoutSpaces = new JsonObject();
        layoutSpaces.addProperty("type", "space");
        JsonObject layoutAdvances = new JsonObject();
        layoutAdvances.addProperty("\uE001", 13);
        layoutAdvances.addProperty("\uE002", 15);
        layoutSpaces.add("advances", layoutAdvances);
        layoutProviders.add(layoutSpaces);
        JsonObject layoutFont = new JsonObject();
        layoutFont.add("providers", layoutProviders);
        Files.writeString(fonts.resolve("layout.json"), gson.toJson(layoutFont));
        System.out.println("Direct Minecraft sprite references: " + nativeSprites + "/" + entries.size());
        Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"KatzeGames Bingo board (required)\",\"min_format\":[97,1],\"max_format\":[97,1]}}\n");
        BufferedImage preview = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D cover = preview.createGraphics();
        cover.setColor(new Color(20, 25, 36));
        cover.fillRect(0, 0, 128, 128);
        cover.setColor(new Color(255, 218, 73));
        cover.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        cover.drawString("BINGO", 31, 23);
        for (int row = 0; row < 5; row++) for (int col = 0; col < 5; col++) {
            int index = row * 5 + col;
            cover.drawImage(atlas, 11 + col * 21, 29 + row * 19, 31 + col * 21, 47 + row * 19,
                    (row == col ? 3 : 0) * 18, index * 18, (row == col ? 4 : 1) * 18, index * 18 + 18, null);
        }
        cover.dispose();
        ImageIO.write(preview, "png", pack.resolve("pack.png").toFile());
        Path language = module.resolve("src/main/resources/lang/bingo/objectives");
        Files.createDirectories(language);
        for (int lang = 0; lang < 2; lang++) {
            StringBuilder content = new StringBuilder();
            for (String[] entry : entries) {
                String target = entry[11 + lang];
                if (target.startsWith("@")) target = "<target>";
                String value = switch (entry[1]) {
                    case "ITEM" -> (lang == 0 ? "Collect " : "Sammle ") + entry[4] + " " + target;
                    case "MOB" -> (lang == 0 ? "Kill " : "Töte ") + entry[4] + " " + target;
                    case "BIOME", "STRUCTURE" -> (lang == 0 ? "Visit: " : "Besuche: ") + target;
                    default -> (lang == 0 ? "Complete: " : "Erreiche: ") + target;
                };
                content.append("katze.bingo.objective.").append(entry[0]).append('=').append(value).append('\n');
            }
            Files.writeString(language.resolve(lang == 0 ? "en.properties" : "de.properties"), content, StandardCharsets.UTF_8);
        }
        System.out.println("Generated " + entries.size() + " objective icons, five ownership states, five row fonts, and both translations.");
    }
}

