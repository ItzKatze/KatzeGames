package gg.itzkatze.katze.client.export;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import gg.itzkatze.katze.client.mixin.GuiGraphicsStateAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.joml.Vector4f;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public abstract class NativeIconExporter<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int GUI_SCALE = 4;
    private final int iconSize;
    private final int pitch;
    private static NativeIconExporter<?> active;

    private final Path output;
    private final List<Entry<T>> entries = new ArrayList<>();
    private final List<String> written = new ArrayList<>();
    private volatile Batch batch;
    private volatile boolean capturePending;
    private volatile boolean finished;
    private int frames;
    private int offset;
    private boolean worldRequested;

    protected record Entry<T>(Identifier id, T value) {}
    private record Batch(int offset, int count, int columns, int guiWidth, int guiHeight) {}
    private record Manifest(int format, String minecraft, String renderer, int size, List<String> items) {}

    protected NativeIconExporter(Path output, int size) {
        this.output = output.toAbsolutePath().normalize();
        iconSize = size * GUI_SCALE;
        pitch = size + 8;
    }

    protected abstract List<Entry<T>> entries(Minecraft client);
    protected abstract void drawEntry(GuiGraphicsExtractor graphics, T value, int x, int y);
    protected abstract String renderer();
    protected boolean writeIcon(NativeImage icon, Path file) throws Exception { icon.writeToFile(file); return true; }
    protected int exportedSize() { return iconSize; }
    protected int batchLimit() { return 49; }

    public static boolean enabled() { return System.getProperty(ItemIconExporter.OUTPUT_PROPERTY) != null || System.getProperty(MobIconExporter.OUTPUT_PROPERTY) != null; }
    public static boolean rendering() { return active != null && active.batch != null && !active.finished; }

    public static void initialize() {
        if (!enabled()) return;
        String mobs = System.getProperty(MobIconExporter.OUTPUT_PROPERTY);
        active = mobs == null ? new ItemIconExporter(Path.of(System.getProperty(ItemIconExporter.OUTPUT_PROPERTY))) : new MobIconExporter(Path.of(mobs));
        ClientTickEvents.END_CLIENT_TICK.register(active::tick);
    }

    private void tick(Minecraft client) {
        if (finished || !client.isGameLoadFinished() || client.gui.overlay() != null || client.gui.screen() instanceof ExportScreen) return;
        try {
            if (client.level == null || client.player == null) {
                if (!worldRequested) {
                    worldRequested = true;
                    String world = "KatzeGamesIconExport-" + System.currentTimeMillis();
                    LevelSettings settings = new LevelSettings(world, GameType.CREATIVE,
                            new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
                    client.createWorldOpenFlows().createFreshLevel(world, settings, new WorldOptions(1, false, false),
                            WorldPresets::createTestWorldDimensions, client.gui.screen());
                }
                return;
            }
            Files.createDirectories(output);
            Files.deleteIfExists(output.resolve("manifest.json"));
            Files.deleteIfExists(output.resolve("failed.txt"));
            client.options.guiScale().set(GUI_SCALE);
            entries.clear();
            entries.addAll(entries(client));
            LOGGER.info("Exporting {} native Minecraft icons to {}", entries.size(), output);
            client.setScreenAndShow(new ExportScreen(this));
        } catch (Exception exception) { fail(exception); }
    }

    private void extract(GuiGraphicsExtractor graphics, int width, int height) {
        var state = ((GuiGraphicsStateAccessor) graphics).katzegames$getRenderState();
        state.reset();
        state.clearColorOverride = new Vector4f(0, 0, 0, 0);
        if (capturePending || finished) {
            draw(graphics, batch);
            return;
        }
        int columns = Math.max(1, (width - 8) / pitch);
        int rows = Math.max(1, (height - 8) / pitch);
        batch = new Batch(offset, Math.min(batchLimit(), Math.min(columns * rows, entries.size() - offset)), columns, width, height);
        draw(graphics, batch);
    }

    private void draw(GuiGraphicsExtractor graphics, Batch page) {
        if (page == null || finished) return;
        for (int index = 0; index < page.count(); index++) {
            drawEntry(graphics, entries.get(page.offset() + index).value(), 4 + index % page.columns() * pitch, 4 + index / page.columns() * pitch);
        }
    }

    public static void afterRender(GameRenderer renderer) {
        NativeIconExporter<?> exporter = active;
        if (exporter != null) exporter.capture(renderer);
    }

    private void capture(GameRenderer renderer) {
        Batch page = batch;
        if (finished || capturePending || page == null || ++frames < 4) return;
        capturePending = true;
        RenderTarget target = renderer.mainRenderTarget();
        var texture = target.getColorTexture();
        if (texture == null || texture.getFormat().blockSize() != 4) { fail(new IllegalStateException("Icon export needs a four-channel render target")); return; }
        int width = target.width, height = target.height;
        GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "KatzeGames icon export", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, (long) width * height * 4);
        RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(texture, buffer, 0, () -> {
            try (buffer; var mapped = buffer.map(true, false); NativeImage image = new NativeImage(width, height, false)) {
                var pixels = mapped.data();
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) image.setPixelABGR(x, height - y - 1, pixels.getInt((y * width + x) * 4));
                save(page, image);
            } catch (Exception exception) { fail(exception); }
        }, 0);
    }

    private void save(Batch page, NativeImage image) throws Exception {
        double scaleX = image.getWidth() / (double) page.guiWidth(), scaleY = image.getHeight() / (double) page.guiHeight();
        if (scaleX != GUI_SCALE || scaleY != GUI_SCALE) throw new IllegalStateException("Icon export requires GUI scale " + GUI_SCALE + "; got " + scaleX + " x " + scaleY);
        for (int index = 0; index < page.count(); index++) {
            Entry<T> entry = entries.get(page.offset() + index);
            Path file = output.resolve(entry.id().getNamespace()).resolve(entry.id().getPath() + ".png").normalize();
            if (!file.startsWith(output)) throw new IllegalStateException("Invalid icon export path: " + entry.id());
            Files.createDirectories(file.getParent());
            int x = (4 + index % page.columns() * pitch) * GUI_SCALE, y = (4 + index / page.columns() * pitch) * GUI_SCALE;
            try (NativeImage icon = new NativeImage(iconSize, iconSize, false)) {
                for (int iy = 0; iy < iconSize; iy++) for (int ix = 0; ix < iconSize; ix++) icon.setPixel(ix, iy, image.getPixel(x + ix, y + iy));
                Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
                try {
                    if (!writeIcon(icon, temporary)) {
                        frames = 0;
                        batch = null;
                        capturePending = false;
                        return;
                    }
                }
                catch (Exception exception) { throw new IllegalStateException("Could not export " + entry.id(), exception); }
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            written.add(entry.id().toString());
        }
        offset = page.offset() + page.count();
        LOGGER.info("Exported {}/{} icons", offset, entries.size());
        if (offset >= entries.size()) {
            Manifest manifest = new Manifest(1, SharedConstants.getCurrentVersion().id(), renderer(), exportedSize(), List.copyOf(written));
            Files.writeString(output.resolve("manifest.json"), new GsonBuilder().setPrettyPrinting().create().toJson(manifest));
            finished = true;
            LOGGER.info("Native icon export complete: {} icons", written.size());
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
        } else {
            frames = 0;
            batch = null;
            capturePending = false;
        }
    }

    private void fail(Exception exception) {
        if (finished) return;
        finished = true;
        LOGGER.error("Native icon export failed", exception);
        try { Files.createDirectories(output); Files.writeString(output.resolve("failed.txt"), exception.toString()); }
        catch (Exception writeError) { LOGGER.error("Could not save icon export failure", writeError); }
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
    }

    private static final class ExportScreen extends Screen {
        private final NativeIconExporter<?> exporter;
        private ExportScreen(NativeIconExporter<?> exporter) { super(Component.literal("KatzeGames native icon export")); this.exporter = exporter; }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) { exporter.extract(graphics, width, height); }
        @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {}
        @Override public boolean shouldCloseOnEsc() { return false; }
    }
}
