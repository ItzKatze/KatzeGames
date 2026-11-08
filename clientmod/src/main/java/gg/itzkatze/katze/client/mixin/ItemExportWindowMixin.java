package gg.itzkatze.katze.client.mixin;

import com.mojang.blaze3d.platform.Window;
import gg.itzkatze.katze.client.export.NativeIconExporter;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Window.class)
public abstract class ItemExportWindowMixin {
    @ModifyArg(method = "createWindow", at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuBackend;createWindow(Ljava/lang/String;IIJ)J"), index = 3)
    private long katzegames$hideExportWindow(long flags) { return NativeIconExporter.enabled() ? flags | SDLVideo.SDL_WINDOW_HIDDEN : flags; }
}
