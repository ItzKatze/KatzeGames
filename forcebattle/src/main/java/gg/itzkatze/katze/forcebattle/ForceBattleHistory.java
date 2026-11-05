package gg.itzkatze.katze.forcebattle;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.results.BattleResultsStore;
import gg.itzkatze.katze.results.SavedBattle;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public final class ForceBattleHistory {
    private static Round round;

    public record Member(UUID id, String name, Integer teamId) {}
    public record Round(UUID id, long startedAt, List<Member> members) {
        public Round {
            members = List.copyOf(members);
        }
    }

    private ForceBattleHistory() {
    }

    public static Round current() {
        return round;
    }

    public static void restore(Round saved) {
        round = saved;
    }

    public static void begin() {
        List<Member> members = KatzePlayer.getOnlinePlayers().stream()
                .filter(ForceBattlePlayer.class::isInstance)
                .filter(KatzePlayer::isParticipating)
                .map(player -> new Member(player.getUuid(), player.getName(), player.isInTeam() ? player.getTeam().getId() : null))
                .toList();
        round = new Round(UUID.randomUUID(), System.currentTimeMillis(), members);
    }

    public static void finish() {
        if (round == null) return;
        List<SavedBattle.Player> players = new ArrayList<>();
        for (Member member : round.members()) {
            ForceBattlePlayer player = KatzePlayer.adapt(member.id()).filter(ForceBattlePlayer.class::isInstance)
                    .map(ForceBattlePlayer.class::cast).orElse(null);
            List<ForceBattleObjective> objectives = new ArrayList<>();
            if (player != null) {
                objectives.addAll(ForceBattleObjective.finishedBy(player));
                ForceBattleObjective current = player.getObjective() != null ? player.getObjective() : player.getLastObjective();
                if (current != null && !current.isFinished()) objectives.add(current);
            }
            players.add(new SavedBattle.Player(member.id(), member.name(), member.teamId(), player == null ? 0 : player.getPoints(),
                    snapshot(objectives)));
        }
        List<SavedBattle.Team> teams = new ArrayList<>();
        for (Integer teamId : round.members().stream().map(Member::teamId).filter(Objects::nonNull).distinct().toList()) {
            List<SavedBattle.Objective> objectives = new ArrayList<>();
            players.stream().filter(player -> teamId.equals(player.teamId())).forEach(player -> objectives.addAll(player.objectives()));
            ForceBattleTeam.findById(teamId).ifPresent(team -> {
                ForceBattleObjective current = team.getObjective() != null ? team.getObjective() : team.getLastObjective();
                if (current != null && !current.isFinished()) objectives.add(snapshot(current));
            });
            int points = players.stream().filter(player -> teamId.equals(player.teamId())).mapToInt(SavedBattle.Player::points).sum();
            teams.add(new SavedBattle.Team(teamId, points, objectives.stream()
                    .sorted(Comparator.comparingInt(SavedBattle.Objective::remainingSeconds).reversed()).toList()));
        }
        SavedBattle battle = new SavedBattle(round.id(), round.startedAt(), System.currentTimeMillis(),
                KatzeGames.getTimer().getElapsedSeconds(), players, teams);
        try {
            BattleResultsStore.installed().save(battle);
            round = null;
            KatzePlayer.getOnlinePlayers().stream().filter(player -> battle.includes(player.getUuid()))
                    .forEach(player -> player.sendMessage(Component.translatable("katze.results.saved")));
        } catch (IOException e) {
            KatzeGames.get().getLogger().log(Level.SEVERE, "Failed to archive ForceBattle results", e);
        }
    }

    private static List<SavedBattle.Objective> snapshot(List<ForceBattleObjective> objectives) {
        return objectives.stream().distinct().map(ForceBattleHistory::snapshot).toList();
    }

    private static SavedBattle.Objective snapshot(ForceBattleObjective objective) {
        GsonComponentSerializer serializer = GsonComponentSerializer.gson();
        return new SavedBattle.Objective(serializer.serialize(objective.displayName()),
                serializer.serialize(objective.getBattleType().displayName()), objective.getToastIcon().name(),
                objective.isFinished() ? objective.getWhoFinished().getName() : "", objective.getTime(),
                objective.isFinished(), objective.hasBeenSkipped());
    }
}
