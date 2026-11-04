package gg.itzkatze.katze.forcebattle;

import gg.itzkatze.katze.achievements.AchievementService;
import gg.itzkatze.katze.challenges.ChallengeType;

import gg.itzkatze.katze.level.LevelStore;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.team.KatzeTeam;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class ForceBattleWinners {

    private record Competitor(int points, @NotNull List<ForceBattlePlayer> members) {
    }

    private ForceBattleWinners() {
    }

    static @NotNull List<ForceBattlePlayer> winners() {
        List<Competitor> competitors = new ArrayList<>();
        for (KatzeTeam team : KatzeTeam.teams) {
            if (!(team instanceof ForceBattleTeam forceBattleTeam)) continue;

            List<ForceBattlePlayer> members = forceBattleTeam.getPlayers().stream()
                    .filter(ForceBattlePlayer.class::isInstance)
                    .map(ForceBattlePlayer.class::cast)
                    .filter(KatzePlayer::isParticipating)
                    .toList();
            if (!members.isEmpty()) {
                competitors.add(new Competitor(forceBattleTeam.getPoints(), members));
            }
        }
        for (KatzePlayer player : KatzePlayer.PLAYERS) {
            if (player instanceof ForceBattlePlayer forceBattlePlayer && forceBattlePlayer.isParticipating() && !forceBattlePlayer.isInTeam()) {
                competitors.add(new Competitor(forceBattlePlayer.getPoints(), List.of(forceBattlePlayer)));
            }
        }
        if (competitors.size() < 2) return List.of();

        int top = competitors.stream().mapToInt(Competitor::points).max().orElse(0);
        if (top <= 0) return List.of();

        return competitors.stream()
                .filter(competitor -> competitor.points() == top)
                .flatMap(competitor -> competitor.members().stream())
                .toList();
    }

    static void reward() {
        List<ForceBattlePlayer> winners = winners();
        Set<UUID> ids = winners.stream().map(KatzePlayer::getUuid).collect(Collectors.toSet());
        Set<UUID> teamIds = winners.stream().filter(KatzePlayer::isInTeam).map(KatzePlayer::getUuid).collect(Collectors.toSet());
        AchievementService.get().naturalEnd(ChallengeType.FORCE_BATTLE, ids, teamIds);
        winners.forEach(winner -> LevelStore.get().grant(winner, ForceBattleXp.WIN.xp()));
    }
}
