import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class BingoBoardPreview {
    private BingoBoardPreview() {}

    static void write(Path module, BufferedImage atlas, List<String[]> entries, Map<String, BufferedImage> badges, int cellSize) throws Exception {
        BufferedImage image = new BufferedImage(760, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(17, 23, 34));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(235, 240, 249));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        graphics.drawString("BINGO · CATEGORY BADGES", 28, 36);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        graphics.setColor(new Color(169, 183, 205));
        graphics.drawString("Board at GUI scale 3 · original item and mob pictures", 28, 62);
        String[] samples = {"diamond", "zombie", "forest", "village", "iron_ingot"};
        int[] indexes = new int[5];
        for (int category = 0; category < 5; category++) {
            String name = BingoCategoryBadges.CATEGORIES.get(category);
            indexes[category] = -1;
            for (int index = 0; index < entries.size(); index++) {
                String[] entry = entries.get(index);
                if (entry[1].equals(name) && (indexes[category] < 0 || entry[0].equals(samples[category]))) {
                    indexes[category] = index;
                    if (entry[0].equals(samples[category])) break;
                }
            }
        }
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int row = 0; row < 5; row++) for (int column = 0; column < 5; column++) {
            int state = row == 2 ? 1 : row == 3 ? 2 : row == 4 ? 3 : 0;
            int sourceY = indexes[(column + row) % 5] * cellSize;
            int x = 28 + column * (cellSize + 1) * 3;
            int y = 86 + row * (cellSize + 1) * 3;
            graphics.drawImage(atlas, x, y, x + cellSize * 3, y + cellSize * 3,
                    state * cellSize, sourceY, (state + 1) * cellSize, sourceY + cellSize, null);
        }
        String[] labels = {"Collect an item", "Kill a mob", "Visit a biome", "Find a structure", "Complete an advancement"};
        for (int category = 0; category < 5; category++) {
            int y = 90 + category * 60;
            graphics.drawImage(badges.get(BingoCategoryBadges.CATEGORIES.get(category)), 384, y, 40, 40, null);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
            graphics.setColor(new Color(235, 240, 249));
            graphics.drawString(labels[category], 438, y + 26);
        }
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        graphics.setColor(new Color(169, 183, 205));
        graphics.drawString("64 × 64 source textures · 8 × 8 in-game badges · ownership frames stay visible", 28, 442);
        graphics.drawImage(badges.get("STRUCTURE"), 28, 472, 96, 96, null);
        graphics.drawString("Custom stone tower", 144, 510);
        graphics.drawString("Battlements, masonry, window and arched doorway", 144, 535);
        graphics.dispose();
        Path directory = module.resolve("build/previews");
        Files.createDirectories(directory);
        ImageIO.write(image, "png", directory.resolve("category-board.png").toFile());
    }
}
