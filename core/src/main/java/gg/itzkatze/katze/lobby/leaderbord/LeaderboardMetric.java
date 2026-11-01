package gg.itzkatze.katze.lobby.leaderbord;

import gg.itzkatze.katze.lobby.cosmetics.CosmeticProfile;
import org.jetbrains.annotations.NotNull;

import java.util.function.ToIntFunction;

public enum LeaderboardMetric {

    LIFETIME_POINTS(CosmeticProfile::getEarned),
    BEST_RUN(CosmeticProfile::getBest),
    CURRENT_POINTS(CosmeticProfile::getPoints),
    CASES_OPENED(CosmeticProfile::getCasesOpened);

    private final ToIntFunction<CosmeticProfile> reader;

    LeaderboardMetric(ToIntFunction<CosmeticProfile> reader) {
        this.reader = reader;
    }

    public @NotNull net.kyori.adventure.text.Component title() {
        return net.kyori.adventure.text.Component.translatable("katze.cosmetics.leaderboard." + name().toLowerCase(java.util.Locale.ROOT));
    }

    public @NotNull LeaderboardMetric next() {
        LeaderboardMetric[] metrics = values();
        return metrics[(ordinal() + 1) % metrics.length];
    }

    public int read(@NotNull CosmeticProfile profile) {
        return reader.applyAsInt(profile);
    }
}
