package gg.itzkatze.katze.client;

import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class RunMusicPlayer {

    private static @Nullable RunMusic current;
    private static boolean awaitingCompletion;
    private static int elapsed;

    private RunMusicPlayer() {
    }

    public static void play(Identifier sound, boolean file, boolean loop) {
        stop();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getMusicManager().stopPlaying();
        current = new RunMusic(sound, file, loop);
        awaitingCompletion = !loop;
        elapsed = 0;
        minecraft.getSoundManager().play(current);
    }

    public static void fade(int ticks) {
        awaitingCompletion = false;
        if (current != null) {
            current.fadeOut(ticks);
        }
    }

    public static void stop() {
        awaitingCompletion = false;
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
    }

    public static void tick() {
        if (current == null || !awaitingCompletion || ++elapsed <= 20 || isPlaying()) return;

        stop();
        if (ClientPlayNetworking.canSend(MusicPayload.TYPE)) {
            ClientPlayNetworking.send(new MusicPayload.Finished());
        }
    }

    // Asks the sound manager, so music that a fade, the server's record stop or a world change ended lets the background music back in.
    public static boolean isPlaying() {
        return current != null && Minecraft.getInstance().getSoundManager().isActive(current);
    }
}
