package gg.itzkatze.katze.bossbar;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.player.KatzePlayer;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BossbarManager {

    private static final Map<Challenge, Map<KatzePlayer, Map<BossbarContent, BossBar>>> barsByChallenge = new HashMap<>();
    private static final Map<Challenge, List<BossbarContent>> contentsByChallenge = new HashMap<>();
    private static BukkitTask runningTask;

    private BossbarManager() {}

    public static void hideBarsForChallenge(Challenge challenge) {
        Map<KatzePlayer, Map<BossbarContent, BossBar>> bars = barsByChallenge.remove(challenge);
        if (bars != null) bars.forEach((player, shown) -> shown.values().forEach(player.getAudience()::hideBossBar));
        contentsByChallenge.remove(challenge);
    }

    public static void runTask() {
        stopTask();
        runningTask = Bukkit.getScheduler().runTaskTimer(KatzeGames.get(), BossbarManager::update, 3L, 3L);
    }

    private static void update() {
        List<KatzePlayer> online = KatzePlayer.getOnlinePlayers();
        BossbarKind kind = KatzeGames.getTimer().isRunning() ? BossbarKind.RUNNING : BossbarKind.NOT_RUNNING;
        for (Challenge challenge : ChallengeManager.getActiveChallenges()) {
            Map<KatzePlayer, Map<BossbarContent, BossBar>> players = barsByChallenge.computeIfAbsent(challenge, ignored -> new HashMap<>());
            players.entrySet().removeIf(entry -> {
                if (online.contains(entry.getKey()) && entry.getKey().isParticipating()) return false;
                entry.getValue().values().forEach(entry.getKey().getAudience()::hideBossBar);
                return true;
            });
            List<BossbarContent> contents = contentsByChallenge.computeIfAbsent(challenge, Challenge::getBossbarContents);
            for (KatzePlayer player : online) {
                if (!player.isParticipating()) continue;
                Map<BossbarContent, BossBar> shown = players.computeIfAbsent(player, ignored -> new HashMap<>());
                for (BossbarContent content : contents) {
                    BossBar previous = shown.get(content);
                    BossBar next = content.getKind() == kind ? content.updateBossbar(player, previous) : null;
                    if (previous != null && previous != next) {
                        player.getAudience().hideBossBar(previous);
                        shown.remove(content);
                    }
                    if (next != null && next != previous) {
                        shown.put(content, next);
                        player.getAudience().showBossBar(next);
                    }
                }
            }
        }
    }

    public static void stopTask() {
        if (runningTask != null) {
            runningTask.cancel();
            runningTask = null;
        }
        for (Challenge challenge : Set.copyOf(barsByChallenge.keySet())) {
            hideBarsForChallenge(challenge);
        }
        contentsByChallenge.clear();
    }
}