package gg.itzkatze.katze.deathswap;

import gg.itzkatze.katze.results.ResultRoster;
import gg.itzkatze.katze.results.SavedBattle;

import java.util.List;
import java.util.UUID;

public final class DeathSwapResults {
    public record Elimination(UUID player, int place, int survivedSeconds, int swapsSurvived) {}

    private DeathSwapResults() {}

    public static SavedBattle snapshot(ResultRoster roster, List<Elimination> eliminations, UUID winner,
                                      int elapsedSeconds, int swaps, long endedAt) {
        List<SavedBattle.Player> players = roster.members().stream().map(member -> {
            Elimination result = eliminations.stream().filter(entry -> entry.player().equals(member.id())).findFirst()
                    .orElse(new Elimination(member.id(), member.id().equals(winner) ? 1 : 0, elapsedSeconds, swaps));
            List<SavedBattle.Statistic> statistics = List.of(
                    new SavedBattle.Statistic("place", result.place(), -1),
                    new SavedBattle.Statistic("survived", result.survivedSeconds(), -1),
                    new SavedBattle.Statistic("swaps", result.swapsSurvived(), -1));
            return new SavedBattle.Player(member.id(), member.name(), null,
                    result.place() == 0 ? 0 : roster.members().size() - result.place() + 1, List.of(), statistics);
        }).toList();
        return new SavedBattle(roster.id(), roster.startedAt(), endedAt, elapsedSeconds,
                players, List.of(), SavedBattle.Game.DEATH_SWAP);
    }
}
