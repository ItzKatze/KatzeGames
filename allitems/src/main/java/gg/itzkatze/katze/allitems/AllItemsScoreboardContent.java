package gg.itzkatze.katze.allitems;

import gg.itzkatze.katze.allitems.data.AllItemsData;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.scoreboard.ScoreboardContent;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AllItemsScoreboardContent implements ScoreboardContent {

    private final AllItemsData data;

    public AllItemsScoreboardContent(AllItemsData data) {
        this.data = data;
    }

    @Override
    @NotNull public Component getTitle() {
        return Component.translatable("katze.allitems.scoreboard.title");
    }

    @Override
    @NotNull public Component[] getLines(@NotNull KatzePlayer player) {
        List<Component> lines = new ArrayList<>();

        Component mode = Component.translatable(AllItemsSettings.isTeamMode() ? "katze.allitems.mode.team" : "katze.allitems.mode.individual");
        lines.add(Component.translatable("katze.allitems.scoreboard.mode", gg.itzkatze.katze.text.TextArg.component("mode", mode.color(KatzeColor.WHITE))));

        UUID playerId = player.getPlayer().getUniqueId();
        if (AllItemsSettings.isItemsEnabled()) {
            lines.add(buildProgressLine(AllItemsObjectiveType.ITEM, playerId));
        }

        if (AllItemsSettings.isBiomesEnabled()) {
            lines.add(buildProgressLine(AllItemsObjectiveType.BIOME, playerId));
        }
        if (AllItemsSettings.isMobsEnabled()) {
            lines.add(buildProgressLine(AllItemsObjectiveType.MOB, playerId));
        }
        if (AllItemsSettings.isStructuresEnabled()) {
            lines.add(buildProgressLine(AllItemsObjectiveType.STRUCTURE, playerId));
        }
        if (AllItemsSettings.isAdvancementsEnabled()) {
            lines.add(buildProgressLine(AllItemsObjectiveType.ADVANCEMENT, playerId));
        }

        lines.add(Component.text(" "));

        List<AllItemsData.FoundObjective> recentItems = data.getRecentFinds(3);
        if (!recentItems.isEmpty()) {
            lines.add(Component.translatable("katze.allitems.scoreboard.recent").color(KatzeColor.GOLD));

            for (AllItemsData.FoundObjective item : recentItems) {
                lines.add(item.displayNameComponent().color(KatzeColor.WHITE));
            }
        }

        if (AllItemsSettings.isChainModeEnabled()) {
            AllItemsObjective objective = data.getCurrentObjective(player.getPlayer());
            if (objective != null) {
                lines.add(Component.translatable(
                        "katze.allitems.scoreboard.chain",
                        gg.itzkatze.katze.text.TextArg.component("objective", objective.displayNameComponent())
                ));
            }
        }

        return lines.toArray(new Component[0]);
    }

    private Component buildProgressLine(AllItemsObjectiveType type, UUID playerId) {
        int found = data.getFoundCount(type, playerId);
        int total = switch (type) {
            case ITEM -> data.getTotalAvailableItems();
            case BIOME -> data.getTotalAvailableBiomes();
            case MOB -> data.getTotalAvailableMobs();
            case STRUCTURE -> data.getTotalAvailableStructures();
            case ADVANCEMENT -> data.getTotalAvailableAdvancements();
        };
        return Component.translatable("katze.allitems.scoreboard.progress",
                gg.itzkatze.katze.text.TextArg.component("type", Component.translatable("katze.allitems.type." + type.name().toLowerCase(java.util.Locale.ROOT))),
                gg.itzkatze.katze.text.TextArg.number("found", found),
                gg.itzkatze.katze.text.TextArg.number("total", total));
    }
}
