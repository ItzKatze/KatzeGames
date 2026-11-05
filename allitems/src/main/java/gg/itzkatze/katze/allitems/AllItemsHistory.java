package gg.itzkatze.katze.allitems;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.allitems.data.AllItemsData;
import gg.itzkatze.katze.results.ResultArchives;
import gg.itzkatze.katze.results.ResultRoster;
import gg.itzkatze.katze.results.SavedBattle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AllItemsHistory {
    private AllItemsHistory() {}

    public static void archive(AllItemsData data) {
        ResultRoster roster = data.getResultRoster();
        if (roster == null) return;
        List<SavedBattle.Player> players = new ArrayList<>();
        for (ResultRoster.Member member : roster.members()) {
            List<AllItemsObjective> discoveries = data.getRecordedDiscoveries(member.id());
            List<SavedBattle.Statistic> statistics = new ArrayList<>();
            for (AllItemsObjectiveType type : AllItemsObjectiveType.values()) {
                int found = (int) discoveries.stream().filter(objective -> objective.type() == type).count();
                int total = switch (type) {
                    case ITEM -> data.getTotalAvailableItems();
                    case BIOME -> data.getTotalAvailableBiomes();
                    case MOB -> data.getTotalAvailableMobs();
                    case STRUCTURE -> data.getTotalAvailableStructures();
                    case ADVANCEMENT -> data.getTotalAvailableAdvancements();
                };
                statistics.add(new SavedBattle.Statistic(type.name().toLowerCase(Locale.ROOT), found, total));
            }
            players.add(new SavedBattle.Player(member.id(), member.name(), AllItemsSettings.isTeamMode() ? 0 : null,
                    discoveries.size(), snapshot(discoveries, member.name()), statistics));
        }
        List<SavedBattle.Team> teams = List.of();
        if (AllItemsSettings.isTeamMode()) {
            List<SavedBattle.Objective> shared = snapshot(data.getRecordedDiscoveries(null), "");
            teams = List.of(new SavedBattle.Team(0, shared.size(), shared));
        }
        ResultArchives.save(new SavedBattle(roster.id(), roster.startedAt(), System.currentTimeMillis(),
                KatzeGames.getTimer().getElapsedSeconds(), players, teams, SavedBattle.Game.ALL_ITEMS));
    }

    private static List<SavedBattle.Objective> snapshot(List<AllItemsObjective> discoveries, String player) {
        GsonComponentSerializer serializer = GsonComponentSerializer.gson();
        return discoveries.stream().map(objective -> {
            Material icon = switch (objective.type()) {
                case ITEM -> Material.matchMaterial(objective.key());
                case BIOME -> Material.GRASS_BLOCK;
                case MOB -> Material.SPIDER_SPAWN_EGG;
                case STRUCTURE -> Material.STRUCTURE_BLOCK;
                case ADVANCEMENT -> Material.BREWING_STAND;
            };
            Component type = Component.translatable("katze.allitems.type." + objective.type().name().toLowerCase(Locale.ROOT));
            return new SavedBattle.Objective(serializer.serialize(AllItemsNames.of(objective.type(), objective.key(), objective.displayName())),
                    serializer.serialize(type), icon == null ? "BOOK" : icon.name(), player, -1, true, false);
        }).toList();
    }
}
