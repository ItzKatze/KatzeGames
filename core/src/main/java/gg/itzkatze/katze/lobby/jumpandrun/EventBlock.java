package gg.itzkatze.katze.lobby.jumpandrun;

import gg.itzkatze.katze.lobby.cosmetics.PointsText;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ThreadLocalRandom;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;
import java.util.function.IntSupplier;
import java.time.Duration;
import net.kyori.adventure.title.Title;

public final class EventBlock {

    static final Material MATERIAL = Material.GOLD_BLOCK;
    static final double CHANCE = 0.025;
    private static final int GOLD_FROM = 50;
    private static final int LEVEL_UP_FROM = 16;
    private static final Set<PayoutRoll> ROLLING = new HashSet<>();

    // Uniform within each range, chances in percent: an Event Block pays +6.9 points on average and takes some in 15% of the cases.
    private record Payout(int min, int max, double chance) {
    }

    private static final List<Payout> PAYOUTS = List.of(
            new Payout(-10, -3, 15),
            new Payout(1, 5, 50),
            new Payout(6, 15, 25),
            new Payout(16, 40, 8),
            new Payout(50, 100, 2));
    private static final double TOTAL = PAYOUTS.stream().mapToDouble(Payout::chance).sum();

    private static final Sound LOSS = Sound.sound(Key.key("entity.villager.no"), Sound.Source.PLAYER, 1f, 1f);
    private static final Sound WIN = Sound.sound(Key.key("block.note_block.pling"), Sound.Source.PLAYER, 1f, 2f);
    private static final Sound BIG_WIN = Sound.sound(Key.key("entity.player.levelup"), Sound.Source.PLAYER, 1f, 1f);

    private EventBlock() {
    }

    static boolean rolls(boolean afterEvent, @NotNull Random random) {
        return !afterEvent && random.nextDouble() < CHANCE;
    }

    static int payout(@NotNull Random random) {
        double roll = random.nextDouble() * TOTAL;
        Payout picked = PAYOUTS.getLast();
        for (Payout payout : PAYOUTS) {
            roll -= payout.chance();
            if (roll < 0) {
                picked = payout;
                break;
            }
        }
        return picked.min() + random.nextInt(picked.max() - picked.min() + 1);
    }

    static void animatePayout(Plugin plugin, Player player, IntSupplier payout) {
        PayoutRoll roll = new PayoutRoll(plugin, player, payout);
        ROLLING.add(roll);
        roll.runTaskTimer(plugin, 0L, 1L);
    }

    static void finishSilently(Player player) {
        for (PayoutRoll roll : List.copyOf(ROLLING)) {
            if (roll.player.getUniqueId().equals(player.getUniqueId())) roll.finish(true);
        }
    }

    static void afterPayouts(Player player, Runnable action) {
        ROLLING.stream().filter(roll -> roll.player.getUniqueId().equals(player.getUniqueId())).findFirst()
                .ifPresentOrElse(roll -> roll.after.add(() -> afterPayouts(player, action)), action);
    }

    public static void finishAllSilently() {
        for (PayoutRoll roll : List.copyOf(ROLLING)) {
            roll.finish(true);
        }
    }

    private static final class PayoutRoll extends BukkitRunnable {
        private final Plugin plugin;
        private final Player player;
        private final IntSupplier payout;
        private final List<Runnable> after = new ArrayList<>();
        private final int changes = 5;
        private final int durationTicks = ThreadLocalRandom.current().nextInt(24, 37);
        private int step = 0;
        private int ticks = 0;
        private int nextChange = 0;

        private PayoutRoll(Plugin plugin, Player player, IntSupplier payout) {
            this.plugin = plugin;
            this.player = player;
            this.payout = payout;
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                finish(true);
                return;
            }

            int elapsed = ticks++;
            if (elapsed < nextChange) return;

            if (step < changes) {
                double progress = ++step / (double) changes;
                nextChange = (int) Math.round(durationTicks * progress * progress);
                int preview = payout(ThreadLocalRandom.current());
                showPayout(player, preview, nextChange - elapsed + 1);
                return;
            }

            finish(false);
        }

        private void finish(boolean silent) {
            if (!ROLLING.remove(this)) return;

            int applied = payout.getAsInt();
            cancel();
            after.forEach(Runnable::run);
            if (silent) return;

            showPayout(player, applied, 20);
            player.playSound(EventBlock.sound(applied));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(message(applied));
                }
            }, 20L);
        }
    }

    static @NotNull Component message(int applied) {
        TextColor color = applied < 0 ? KatzeColor.RED : applied >= GOLD_FROM ? KatzeColor.GOLD : KatzeColor.LIME;
        return Component.translatable("katze.cosmetics.event_block_payout", TextArg.component("points", PointsText.signedComponent(applied).color(color))).color(KatzeColor.GOLD).decorate(TextDecoration.BOLD);
    }

    private static void showPayout(@NotNull Player player, int points, int stayTicks) {
        Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(stayTicks * 50L), Duration.ZERO);
        player.showTitle(Title.title(Component.translatable("katze.cosmetics.event_block").color(KatzeColor.GOLD),
                PointsText.signedComponent(points).color(KatzeColor.LIME), times));
    }

    static @NotNull Sound sound(int applied) {
        if (applied < 0) return LOSS;
        return applied >= LEVEL_UP_FROM ? BIG_WIN : WIN;
    }
}

