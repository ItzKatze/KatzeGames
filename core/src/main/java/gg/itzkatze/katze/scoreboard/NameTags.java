package gg.itzkatze.katze.scoreboard;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.level.LevelStore;
import gg.itzkatze.katze.level.LevelTag;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.Texts;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * The name above every player's head and in the tab list: a team per online player on each viewer's own scoreboard, with the level as prefix and the
 * active challenge's suffix.
 */
public final class NameTags {

    private static final long PERIOD_TICKS = 3;
    private static final Map<UUID, CachedDecoration> DECORATIONS = new HashMap<>();

    private record Decoration(Component prefix, Component suffix) {}

    private static final class CachedDecoration {
        private final Decoration original;
        private final Map<Locale, Decoration> rendered = new HashMap<>();

        private CachedDecoration(Decoration original) {
            this.original = original;
        }

        private Decoration forViewer(Player viewer) {
            return rendered.computeIfAbsent(viewer.locale(), ignored ->
                    new Decoration(Texts.render(original.prefix(), viewer), Texts.render(original.suffix(), viewer)));
        }
    }

    private NameTags() {
    }

    public static void start() {
        Bukkit.getScheduler().runTaskTimer(KatzeGames.get(), NameTags::update, 1, PERIOD_TICKS);
    }

    public static void update() {
        List<KatzePlayer> online = KatzePlayer.getOnlinePlayers();
        Set<String> onlineTeams = new HashSet<>();
        online.forEach(target -> onlineTeams.add(target.getUuid().toString()));
        Set<UUID> onlineIds = new HashSet<>();
        List<Challenge> challenges = ChallengeManager.getActiveChallenges().stream()
                .sorted(Comparator.comparing(Challenge::getType)).toList();
        for (KatzePlayer target : online) {
            onlineIds.add(target.getUuid());
            Player listed = target.getPlayer();
            if (listed != null) listed.setPlayerListOrder(target.isSpectator() ? 0 : target.isInTeam() ? target.getTeam().getId() + 1 : 999);
            Decoration decoration = new Decoration(LevelTag.of(LevelStore.get().level(target.getUuid())).append(Component.space()),
                    suffix(target, challenges));
            CachedDecoration cached = DECORATIONS.get(target.getUuid());
            if (cached == null || !cached.original.equals(decoration)) {
                DECORATIONS.put(target.getUuid(), new CachedDecoration(decoration));
            }
        }
        DECORATIONS.keySet().retainAll(onlineIds);

        for (KatzePlayer viewer : online) {
            Player player = viewer.getPlayer();
            if (player == null) continue;

            Scoreboard board = ScoreboardManager.getOrCreateScoreboard(viewer);
            for (KatzePlayer target : online) {
                decorate(board, target, player);
            }
            for (Team team : List.copyOf(board.getTeams())) {
                if (isUuid(team.getName()) && !onlineTeams.contains(team.getName())) {
                    team.unregister();
                }
            }
            if (player.getScoreboard() != board) {
                player.setScoreboard(board);
            }
        }
    }

    private static void decorate(@NotNull Scoreboard board, @NotNull KatzePlayer target, @NotNull Player viewer) {
        String name = target.getUuid().toString();
        Team team = board.getTeam(name);
        if (team == null) {
            team = board.registerNewTeam(name);
        }
        if (!team.hasEntry(target.getName())) {
            team.addEntry(target.getName());
        }

        Decoration decoration = DECORATIONS.get(target.getUuid()).forViewer(viewer);
        Component prefix = decoration.prefix();
        if (!prefix.equals(team.prefix())) {
            team.prefix(prefix);
        }
        Component suffix = decoration.suffix();
        if (!suffix.equals(team.suffix())) {
            team.suffix(suffix);
        }
    }

    private static @NotNull Component suffix(@NotNull KatzePlayer target, List<Challenge> challenges) {
        return challenges.stream()
                .map(challenge -> challenge.getNametagSuffix(target))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> target.getTeam() == null ? Component.empty()
                        : Component.text(" | ", KatzeColor.GRAY).append(Component.translatable("katze.results.team",
                                TextArg.number("id", target.getTeam().getId())).color(KatzeColor.LIGHT_BLUE)));
    }

    private static boolean isUuid(@NotNull String name) {
        try {
            UUID.fromString(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
