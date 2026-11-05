package gg.itzkatze.katze.game;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.achievements.AchievementService;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.level.GameXp;
import gg.itzkatze.katze.lobby.LobbyManager;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.spectator.SpectatorManager;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.StructureScanner;
import gg.itzkatze.katze.game.seed.SeedGenerator;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.NotNull;

public class GameLifecycleManager implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        if (ChallengeManager.isGameStarted()) {
            if (event.getPlayer().isOp() && SessionPersistence.isAwaitingResume()) {
                event.getPlayer().sendMessage(Component.translatable("katze.game.restored")
                        .color(KatzeColor.YELLOW));
            }
            return;
        }

        KatzePlayer player = KatzePlayer.adapt(event.getPlayer());
        sendToLobby(player);
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (!ChallengeManager.isGameStarted()) {
            KatzePlayer.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onWorldSave(@NotNull WorldSaveEvent event) {
        if (Bukkit.getWorlds().isEmpty() || !event.getWorld().equals(Bukkit.getWorlds().getFirst())) {
            return;
        }
        SessionPersistence.saveNow();
    }

    public static void sendToLobby(@NotNull KatzePlayer player) {
        if (player.getPlayer() == null) return;

        LobbyManager.createLobbyIfNeeded();
        player.getPlayer().setFoodLevel(20);
        player.getPlayer().setHealth(20);
        player.getPlayer().setSaturation(20);
        player.getPlayer().setFireTicks(0);
        player.getPlayer().getInventory().clear();
        SpectatorManager.leaveSpectatorMode(player.getPlayer());
        player.getPlayer().setGameMode(GameMode.ADVENTURE);
        Location spawn = LobbyManager.getSpawn();
        if (spawn != null) {
            player.getPlayer().teleport(spawn);
        }
        LobbyManager.onPlayerJoin(player);
        ChallengeManager.getActiveChallenges().forEach(challenge -> challenge.onLobbyJoin(player));
    }

    public static void startGame() {
        GameXp.begin();
        KatzePlayer.removeOfflinePlayers();
        LobbyManager.destroyLobby();
        prepareWorlds();

        World world = GameWorldManager.getWorld();
        for (KatzePlayer player : KatzePlayer.getOnlinePlayers()) {
            if (player.getPlayer() == null) continue;

            if (player.isSpectator()) {
                SpectatorManager.enterSpectatorMode(player.getPlayer());
            } else {
                player.getPlayer().setGameMode(GameMode.SURVIVAL);
            }
            player.getPlayer().getInventory().clear();
            player.getPlayer().setHealth(20);
            player.getPlayer().setSaturation(20);
            player.getPlayer().setFoodLevel(20);
            player.getPlayer().setFireTicks(0);

            if (world != null) {
                player.getPlayer().teleport(world.getSpawnLocation());
                player.getPlayer().setRespawnLocation(world.getSpawnLocation(), true);
            }

            player.getPlayer().playSound(player.getPlayer(), Sound.EVENT_RAID_HORN, SoundCategory.MASTER, 100, 1);
        }
        AchievementService.get().begin();
    }

    private static void prepareWorlds() {
        for (World world : GameWorldManager.getWorlds()) {

            if (world.getEnvironment() == World.Environment.NORMAL) {
                world.setTime(0);
            }
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(0);
            world.setDifficulty(Difficulty.NORMAL);
            world.setGameRule(GameRules.LOCATOR_BAR, false);
        }
    }

    public static void resetGame() {
        resetGame(() -> KatzeGames.broadcast(Component.translatable("katze.game.reset.success").color(KatzeColor.LIME)),
                () -> KatzeGames.broadcast(Component.translatable("katze.game.reset.failed").color(KatzeColor.RED)));
    }

    public static void resetGame(Runnable onSuccess, Runnable onFailure) {
        SeedGenerator.cancel();
        if (GameWorldManager.isResetting()) return;
        StartCountdown.cancel();
        KatzeGames.get().releasePause();
        KatzeGames.getTimer().stop();
        GameXp.onReset();
        AchievementService.get().reset();
        ChallengeManager.resetGameState();
        SessionPersistence.delete();
        KatzePlayer.removeOfflinePlayers();
        StructureScanner.clear();

        for (KatzePlayer player : KatzePlayer.getOnlinePlayers()) {
            sendToLobby(player);
            if (player.getPlayer() != null) {
                player.getPlayer().setRespawnLocation(LobbyManager.getSpawn(), true);
            }
        }
        SpectatorManager.onReset();
        GameWorldManager.reset(onSuccess, onFailure);
    }
}
