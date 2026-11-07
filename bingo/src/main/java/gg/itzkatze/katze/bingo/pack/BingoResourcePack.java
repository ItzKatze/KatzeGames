package gg.itzkatze.katze.bingo.pack;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.text.Texts;
import gg.itzkatze.katze.spectator.SpectatorManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

public final class BingoResourcePack implements Listener {
    private final BingoPackAdmission admission = new BingoPackAdmission();
    private final Map<UUID, GameMode> modes = new HashMap<>();
    private byte[] hash;
    private UUID id;
    private String url;
    private boolean enabled;

    public Component configurationError() {
        String configured = KatzeGames.get().getConfig().getString("bingo.resource-pack.url", "").trim();
        try {
            URI uri = URI.create(configured);
            if (!(uri.getScheme().equalsIgnoreCase("https") || uri.getScheme().equalsIgnoreCase("http"))
                    || uri.getHost() == null || uri.getUserInfo() != null) return Component.translatable("katze.bingo.error.pack_config");
        } catch (RuntimeException exception) { return Component.translatable("katze.bingo.error.pack_config"); }
        return null;
    }

    public void enable() {
        admission.clear();
        modes.clear();
        url = KatzeGames.get().getConfig().getString("bingo.resource-pack.url", "").trim();
        try (var stream = getClass().getResourceAsStream("/bingo/KatzeGames-Bingo.zip")) {
            if (stream == null) throw new IOException("Bingo resource pack is missing from the plugin");
            byte[] bytes = stream.readAllBytes();
            hash = MessageDigest.getInstance("SHA-1").digest(bytes);
            id = UUID.nameUUIDFromBytes(hash);
            Path path = KatzeGames.get().getDataDirectory().toPath().resolve("resource-packs/KatzeGames-Bingo.zip");
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
            KatzeGames.get().getLogger().info("Required Bingo pack: " + path + " (SHA-1 " + HexFormat.of().formatHex(hash) + ")");
        } catch (IOException exception) { throw new UncheckedIOException(exception); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
        enabled = true;
        for (Player player : Bukkit.getOnlinePlayers()) offer(player);
    }

    public void disable() {
        enabled = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (id != null) player.removeResourcePack(id);
            release(player);
        }
        admission.clear();
        modes.clear();
    }

    public boolean ready(UUID player) { return admission.ready(player); }

    public void offer(Player player) {
        if (!enabled) return;
        admission.begin(player.getUniqueId(), Bukkit.getCurrentTick());
        modes.putIfAbsent(player.getUniqueId(), player.getGameMode());
        player.setGameMode(GameMode.SPECTATOR);
        player.setResourcePack(id, url, hash, Texts.render(Component.translatable("katze.bingo.pack.prompt"), player), true);
    }

    public void tick() {
        if (!enabled) return;
        int timeout = Math.clamp(KatzeGames.get().getConfig().getInt("bingo.resource-pack.timeout-seconds", 120), 15, 600) * 20;
        for (UUID player : admission.expired(Bukkit.getCurrentTick(), timeout)) {
            Player online = Bukkit.getPlayer(player);
            if (online != null) reject(online);
        }
    }

    public void hold(Player player) {
        if (enabled && !ready(player.getUniqueId()) && player.getGameMode() != GameMode.SPECTATOR) player.setGameMode(GameMode.SPECTATOR);
    }

    private void release(Player player) {
        GameMode previous = modes.remove(player.getUniqueId());
        if (previous != null && !player.isDead()) {
            if (SpectatorManager.isSpectator(player.getUniqueId())) SpectatorManager.enterSpectatorMode(player);
            else player.setGameMode(previous);
        }
    }

    private void reject(Player player) {
        admission.forget(player.getUniqueId());
        modes.putIfAbsent(player.getUniqueId(), player.getGameMode());
        player.setGameMode(GameMode.SPECTATOR);
        player.kick(Texts.render(Component.translatable("katze.bingo.pack.required"), player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(KatzeGames.get(), () -> {
            if (event.getPlayer().isOnline()) offer(event.getPlayer());
        });
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        release(event.getPlayer());
        admission.forget(player);
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        if (!enabled || !event.getID().equals(id)) return;
        switch (admission.update(event.getPlayer().getUniqueId(), event.getStatus())) {
            case READY -> release(event.getPlayer());
            case REJECT -> reject(event.getPlayer());
            case WAITING, IGNORE -> {}
        }
    }
}
