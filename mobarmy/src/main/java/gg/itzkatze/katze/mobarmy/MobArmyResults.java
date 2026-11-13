package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.results.SavedBattle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class MobArmyResults {
    private MobArmyResults() {}

    static SavedBattle capture(MobArmyMatch match) {
        List<SavedBattle.Player> players = new ArrayList<>();
        List<SavedBattle.Team> teams = new ArrayList<>();
        var winners = match.winners();
        for (var side : match.sides) {
            Integer teamId = side.id.startsWith("team-") ? Integer.valueOf(side.id.substring(5)) : null;
            int points = winners.contains(side) ? 1 : 0;
            var objectives = side.mobs.stream().map(MobArmyResults::mob).toList();
            int place = side.forfeited ? match.sides.size() : 1 + (int) match.sides.stream()
                    .filter(other -> !other.forfeited && other.elapsedTicks < side.elapsedTicks).count();
            List<SavedBattle.Statistic> statistics = List.of(
                    new SavedBattle.Statistic("place", place, -1),
                    new SavedBattle.Statistic("battle_ticks", (int) side.elapsedTicks, -1),
                    new SavedBattle.Statistic("collected", side.mobs.size(), -1),
                    new SavedBattle.Statistic("forfeited", side.forfeited ? 1 : 0, -1));
            side.members.forEach((id, name) -> players.add(new SavedBattle.Player(id, name, teamId, points, objectives, statistics)));
            if (teamId != null) teams.add(new SavedBattle.Team(teamId, points, objectives));
        }
        players.sort(Comparator.comparingInt(player -> player.statistics().getFirst().value()));
        int duration = (int) Math.max(0, (match.endedAt - match.startedAt) / 1000);
        return new SavedBattle(match.id, match.startedAt, match.endedAt, duration, players, teams, SavedBattle.Game.MOB_ARMY);
    }

    private static SavedBattle.Objective mob(MobArmyMatch.CapturedMob mob) {
        var gson = GsonComponentSerializer.gson();
        return new SavedBattle.Objective(gson.serialize(Component.translatable("entity.minecraft." + mob.type.toLowerCase(Locale.ROOT))),
                gson.serialize(Component.translatable("katze.results.collected_mob")), mob.type + "_SPAWN_EGG", "", -1, true, false);
    }
}
