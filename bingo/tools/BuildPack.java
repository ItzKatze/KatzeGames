import team.unnamed.creative.font.FontProvider;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipFile;

public final class BuildPack {
    private static final int ROW_PITCH = 19;
    private record Icon(String texture, int advance) {}

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
        BingoPackAssets assets = new BingoPackAssets();
        Files.createDirectories(textures);
        Path icons = textures.resolve("icons");
        Files.createDirectories(icons);
        Path mobs = textures.resolve("mobs");
        Files.createDirectories(mobs);
        BufferedImage cells = new BufferedImage(90, 18, BufferedImage.TYPE_INT_ARGB);
        Graphics2D cellGraphics = cells.createGraphics();
        Color[] backgrounds = {new Color(35, 40, 51), new Color(28, 78, 145), new Color(161, 65, 29), new Color(28, 78, 145), new Color(161, 65, 29)};
        StringBuilder backgroundCharacters = new StringBuilder();
        for (int state = 0; state < 5; state++) {
            int x = state * 18;
            cellGraphics.setColor(backgrounds[state]);
            cellGraphics.fillRect(x, 0, 18, 18);
            cellGraphics.setColor(state == 0 ? new Color(91, 100, 119) : state == 1 || state == 3 ? new Color(96, 181, 255) : new Color(255, 158, 92));
            for (int edge = 0; edge < 18; edge++) {
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
        assets.texture("katzegames:bingo/cells.png", cells);
        Files.deleteIfExists(textures.resolve("objectives.png"));
        BufferedImage atlas = new BufferedImage(90, entries.size() * 18, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        List<Icon> iconSources = new ArrayList<>();
        StringBuilder iconMaterials = new StringBuilder();
        int nativeSprites = 0;
        Set<Path> usedIcons = new HashSet<>();
        Set<Path> usedMobs = new HashSet<>();
        List<String> numberCharacters;
        try (ZipFile minecraft = new ZipFile(args[1])) {
            var defaultFont = new MinecraftAssets(minecraft).json("assets/minecraft/font/include/default.json");
            numberCharacters = defaultFont.getAsJsonArray("providers").asList().stream().map(provider -> provider.getAsJsonObject())
                    .filter(provider -> provider.has("file") && provider.get("file").getAsString().equals("minecraft:font/ascii.png"))
                    .findFirst().orElseThrow().getAsJsonArray("chars").asList().stream().map(character -> character.getAsString()).toList();
            String minecraftVersion = new MinecraftAssets(minecraft).json("version.json").get("id").getAsString();
            Path exports = args.length > 2 ? Path.of(args[2]) : module.resolve("src/main/item-icons/" + minecraftVersion);
            BingoIconRenderer renderer = new BingoIconRenderer(minecraft, exports);
            Path mobExports = module.resolve("src/main/mob-icons/" + minecraftVersion);
            NativeMobIcons portraits = new NativeMobIcons(mobExports, minecraftVersion);
            for (int index = 0; index < entries.size(); index++) {
                String[] entry = entries.get(index);
                MinecraftItemSprites.Sprite sprite;
                BufferedImage icon;
                String mob = renderer.mob(entry);
                try {
                    sprite = mob == null ? renderer.sprite(entry) : null;
                    icon = mob != null ? portraits.read("minecraft:" + mob) : sprite == null ? renderer.render(entry) : sprite.image();
                } catch (Exception exception) {
                    throw new IllegalStateException("Unable to resolve Bingo objective: " + entry[0], exception);
                }
                String texture;
                if (mob != null) {
                    String filename = mob + ".png";
                    texture = "katzegames:bingo/mobs/" + filename;
                    if (usedMobs.add(mobs.resolve(filename))) assets.texture(texture, mobExports.resolve("minecraft").resolve(filename));
                } else if (sprite == null) {
                    String filename = renderer.material(entry).toLowerCase(Locale.ROOT) + ".png";
                    texture = "katzegames:bingo/icons/" + filename;
                    Path destination = icons.resolve(filename);
                    if (usedIcons.add(destination)) assets.texture(texture, exports.resolve("minecraft").resolve(filename));
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
        try (var files = Files.list(mobs)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".png") && Files.isRegularFile(path) && !usedMobs.contains(path)).toList()) Files.delete(file);
        }
        graphics.dispose();
        Path iconMetadata = module.resolve("src/main/resources/bingo/icons.properties");
        if (!Files.exists(iconMetadata) || !Files.readString(iconMetadata, StandardCharsets.UTF_8).equals(iconMaterials.toString()))
            Files.writeString(iconMetadata, iconMaterials, StandardCharsets.UTF_8);
        Files.deleteIfExists(textures.resolve("rows.png"));
        for (int row = 0; row < 5; row++) {
            List<FontProvider> providers = new ArrayList<>();
            providers.add(BingoPackAssets.bitmap("katzegames:bingo/cells.png", 18, 7 - row * ROW_PITCH, List.of(backgroundCharacters.toString())));
            providers.add(BingoPackAssets.bitmap("minecraft:font/ascii.png", 8, 2 - row * ROW_PITCH, numberCharacters));
            var spaces = FontProvider.space().advance(0xE000, -104).advance(0xE010, -18).advance(0xE011, 2).advance(0xE012, 1);
            for (int index = 0; index < iconSources.size(); index++) {
                Icon icon = iconSources.get(index);
                spaces.advance(0xF4000 + index, 18 - icon.advance());
                providers.add(BingoPackAssets.bitmap(icon.texture(), 16, 6 - row * ROW_PITCH, List.of(Character.toString(0xF0000 + index))));
            }
            providers.add(spaces.build());
            assets.font("katzegames:bingo/row_" + row, providers);
        }
        assets.font("katzegames:bingo/layout", List.of(
                BingoPackAssets.bitmap("minecraft:font/ascii.png", 8, 7, numberCharacters),
                FontProvider.space().advance(0xE001, 13).advance(0xE002, 15).build()));
        System.out.println("Direct Minecraft sprite references: " + nativeSprites + "/" + entries.size());
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
        assets.icon(preview);
        assets.write(pack);
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

