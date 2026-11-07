package gg.itzkatze.katze.client.mixin;

import gg.itzkatze.katze.client.export.ItemIconExporter;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class ItemExportRenderMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void katzegames$exportItems(CallbackInfo info) { ItemIconExporter.afterRender((GameRenderer) (Object) this); }

    @Inject(method = "renderLevel", at = @At("HEAD"), cancellable = true)
    private void katzegames$hideExportWorld(CallbackInfo info) { if (ItemIconExporter.rendering()) info.cancel(); }
}
