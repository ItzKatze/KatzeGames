import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BingoCategoryBadges {
    static final List<String> CATEGORIES = List.of("ITEM", "MOB", "BIOME", "STRUCTURE", "ADVANCEMENT");
    static final int RESOLUTION = 64;

    private BingoCategoryBadges() {}

    static Map<String, BufferedImage> generate(Path exports) throws Exception {
        Map<String, BufferedImage> badges = new LinkedHashMap<>();
        Map<String, String> materials = Map.of("ITEM", "chest", "MOB", "diamond_sword", "BIOME", "oak_sapling", "ADVANCEMENT", "nether_star");
        for (String category : CATEGORIES) {
            BufferedImage badge = new BufferedImage(RESOLUTION, RESOLUTION, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = badge.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.setColor(new Color(10, 14, 23, 245));
            graphics.fillRoundRect(0, 0, 64, 64, 12, 12);
            graphics.setColor(new Color(116, 132, 154));
            graphics.setStroke(new BasicStroke(2));
            graphics.drawRoundRect(1, 1, 61, 61, 10, 10);
            if (category.equals("STRUCTURE")) {
                tower(graphics);
            } else {
                Path source = exports.resolve("minecraft/" + materials.get(category) + ".png");
                BufferedImage nativeIcon = ImageIO.read(source.toFile());
                graphics.drawImage(nativeIcon, 8, 8, 48, 48, null);
            }
            graphics.dispose();
            badges.put(category, badge);
        }
        return badges;
    }

    private static void tower(Graphics2D graphics) {
        int[] x = {12, 20, 20, 28, 28, 36, 36, 44, 44, 52, 52, 48, 48, 52, 52, 12, 12, 16, 16, 12};
        int[] y = {10, 10, 18, 18, 10, 10, 18, 18, 10, 10, 28, 28, 52, 52, 58, 58, 52, 52, 28, 28};
        graphics.setColor(new Color(205, 215, 228));
        graphics.fillPolygon(x, y, x.length);
        graphics.setColor(new Color(239, 245, 251));
        graphics.fillRect(12, 10, 8, 4);
        graphics.fillRect(28, 10, 8, 4);
        graphics.fillRect(44, 10, 8, 4);
        graphics.fillRect(12, 22, 40, 4);
        graphics.fillRect(16, 28, 4, 24);
        graphics.fillRect(12, 54, 40, 4);
        graphics.setColor(new Color(127, 145, 169));
        graphics.fillRect(44, 28, 4, 24);
        graphics.fillRect(18, 38, 26, 2);
        graphics.fillRect(18, 48, 26, 2);
        graphics.fillRect(24, 28, 2, 10);
        graphics.fillRect(38, 40, 2, 8);
        graphics.setColor(new Color(24, 35, 51));
        graphics.fillRect(28, 28, 8, 8);
        graphics.fillRect(28, 44, 12, 12);
        graphics.fillRect(30, 42, 8, 2);
        graphics.setColor(new Color(112, 182, 226));
        graphics.fillRect(28, 28, 2, 8);
        graphics.setColor(new Color(82, 58, 46));
        graphics.fillRect(30, 46, 8, 10);
        graphics.setColor(new Color(211, 169, 96));
        graphics.fillRect(34, 50, 2, 2);
    }
}
