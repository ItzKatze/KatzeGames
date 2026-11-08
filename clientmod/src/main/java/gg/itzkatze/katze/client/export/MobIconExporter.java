package gg.itzkatze.katze.client.export;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class MobIconExporter extends NativeIconExporter<LivingEntity> {
    public static final String OUTPUT_PROPERTY = "katzegames.mobExport.output";
    private int framingAttempts;

    MobIconExporter(Path output) { super(output, 64); }

    @Override protected List<Entry<LivingEntity>> entries(Minecraft client) {
        return BuiltInRegistries.ENTITY_TYPE.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().identifier().toString()))
                .filter(entry -> BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath(entry.getKey().identifier().getNamespace(), entry.getKey().identifier().getPath() + "_spawn_egg")))
                .map(entry -> {
                    var entity = entry.getValue().create(client.level, new EntitySpawnRequest(EntitySpawnReason.COMMAND, true));
                    if (!(entity instanceof LivingEntity living)) throw new IllegalStateException("No living mob for " + entry.getKey().identifier());
                    living.setId(BuiltInRegistries.ENTITY_TYPE.getId(entry.getValue()) + 1);
                    return new Entry<>(entry.getKey().identifier(), living);
                }).toList();
    }

    @Override protected void drawEntry(GuiGraphicsExtractor graphics, LivingEntity entity, int x, int y) {
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var state = dispatcher.getRenderer(entity).createRenderState(entity, 1);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.nameTag = null;
        state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 210;
            living.yRot = 0;
            living.xRot = 0;
        }
        float extent = Math.max(state.boundingBoxHeight, state.boundingBoxWidth * 1.5f);
        float scale = 44 * (float) Math.pow(0.65, framingAttempts) / Math.max(extent, 0.1f);
        boolean flying = entity.getType() == EntityTypes.ENDER_DRAGON || entity.getType() == EntityTypes.PHANTOM;
        Quaternionf tilt = new Quaternionf().rotateX((float) Math.toRadians(flying ? -30 : -10));
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(tilt);
        if (!(state instanceof LivingEntityRenderState)) rotation.rotateY((float) Math.toRadians(30));
        graphics.entity(state, scale, new Vector3f(0, state.boundingBoxHeight / 2, 0),
                rotation, tilt, x, y, x + 64, y + 64);
    }

    @Override protected String renderer() { return "minecraft-mob-portrait"; }
    @Override protected int exportedSize() { return 128; }
    @Override protected int batchLimit() { return 1; }

    @Override protected boolean writeIcon(NativeImage source, Path file) throws Exception {
        int left = source.getWidth(), top = source.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
            if ((source.getPixel(x, y) >>> 24) == 0) continue;
            left = Math.min(left, x);
            top = Math.min(top, y);
            right = Math.max(right, x);
            bottom = Math.max(bottom, y);
        }
        if (right < left) throw new IllegalStateException("Minecraft rendered an empty mob portrait");
        if (left == 0 || top == 0 || right == source.getWidth() - 1 || bottom == source.getHeight() - 1) {
            if (++framingAttempts <= 6) return false;
            source.writeToFile(file.resolveSibling("clipped.png"));
            throw new IllegalStateException("Mob portrait remains clipped after automatic framing");
        }
        BufferedImage cropped = new BufferedImage(right - left + 1, bottom - top + 1, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < cropped.getHeight(); y++) for (int x = 0; x < cropped.getWidth(); x++) cropped.setRGB(x, y, source.getPixel(left + x, top + y));
        BufferedImage portrait = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        double ratio = 120.0 / Math.max(cropped.getWidth(), cropped.getHeight());
        int width = Math.max(1, (int) Math.round(cropped.getWidth() * ratio)), height = Math.max(1, (int) Math.round(cropped.getHeight() * ratio));
        var graphics = portrait.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(cropped, (128 - width) / 2, (128 - height) / 2, width, height, null);
        graphics.dispose();
        ImageIO.write(portrait, "png", file.toFile());
        framingAttempts = 0;
        return true;
    }
}
