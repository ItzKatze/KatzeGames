package gg.itzkatze.katze.allitems.commands;

import gg.itzkatze.katze.allitems.AllItems;
import gg.itzkatze.katze.allitems.AllItemsObjectiveType;
import gg.itzkatze.katze.allitems.AllItemsSettings;
import gg.itzkatze.katze.allitems.data.AllItemsData;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.commands.framework.CallerType;
import gg.itzkatze.katze.commands.framework.CommandCaller;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.TextArg;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AllItemsProgressCommand extends KatzeCommand {

    public AllItemsProgressCommand() {
        super(
                "allitemsprogress",
                List.of("allitemsstats", "allitemsinfo"),
                CallerType.PLAYER,
                "allitemsprogress",
                PermissionDefault.TRUE,
                "View your progress in the All Items challenge"
        );
    }

    @Override
    protected void executeCommand(@NotNull CommandCaller caller, String[] args) {
        if (!ChallengeManager.isChallengeActive(ChallengeType.ALL_ITEMS)) {
            caller.sendMessage(Component.translatable("katze.allitems.command.not_active"));
            return;
        }

        AllItemsData data = AllItems.getData();

        UUID playerId = caller instanceof KatzePlayer player ? player.getUuid() : null;
        int foundItems = data.getFoundCount(AllItemsObjectiveType.ITEM, playerId);
        int totalItems = data.getTotalAvailableItems();
        double itemPercent = data.getProgressPercentage(AllItemsObjectiveType.ITEM, playerId);

        caller.sendMessage(progress(AllItemsObjectiveType.ITEM, foundItems, totalItems, itemPercent));

        if (AllItemsSettings.isBiomesEnabled()) {
            int foundBiomes = data.getFoundCount(AllItemsObjectiveType.BIOME, playerId);
            int totalBiomes = data.getTotalAvailableBiomes();
            double percent = data.getProgressPercentage(AllItemsObjectiveType.BIOME, playerId);
            caller.sendMessage(progress(AllItemsObjectiveType.BIOME, foundBiomes, totalBiomes, percent));
        }
        if (AllItemsSettings.isMobsEnabled()) {
            int foundMobs = data.getFoundCount(AllItemsObjectiveType.MOB, playerId);
            int totalMobs = data.getTotalAvailableMobs();
            double percent = data.getProgressPercentage(AllItemsObjectiveType.MOB, playerId);
            caller.sendMessage(progress(AllItemsObjectiveType.MOB, foundMobs, totalMobs, percent));
        }
        if (AllItemsSettings.isStructuresEnabled()) {
            int foundStructures = data.getFoundCount(AllItemsObjectiveType.STRUCTURE, playerId);
            int totalStructures = data.getTotalAvailableStructures();
            double percent = data.getProgressPercentage(AllItemsObjectiveType.STRUCTURE, playerId);
            caller.sendMessage(progress(AllItemsObjectiveType.STRUCTURE, foundStructures, totalStructures, percent));
        }
        if (AllItemsSettings.isAdvancementsEnabled()) {
            int foundAdv = data.getFoundCount(AllItemsObjectiveType.ADVANCEMENT, playerId);
            int totalAdv = data.getTotalAvailableAdvancements();
            double percent = data.getProgressPercentage(AllItemsObjectiveType.ADVANCEMENT, playerId);
            caller.sendMessage(progress(AllItemsObjectiveType.ADVANCEMENT, foundAdv, totalAdv, percent));
        }

        Component mode = Component.translatable(AllItemsSettings.isTeamMode() ? "katze.allitems.mode.team" : "katze.allitems.mode.individual");
        caller.sendMessage(Component.translatable("katze.allitems.command.current_mode", TextArg.component("mode", mode)));

        List<AllItemsData.FoundObjective> recentItems = data.getRecentFinds(5).stream()
                .sorted((a, b) -> b.entry().foundTime().compareTo(a.entry().foundTime()))
                .toList();

        if (!recentItems.isEmpty()) {
            caller.sendMessage(Component.translatable("katze.allitems.command.recent"));

            for (AllItemsData.FoundObjective item : recentItems) {
                caller.sendMessage(Component.translatable("katze.allitems.command.recent_item",
                        TextArg.component("item", item.displayNameComponent()),
                        TextArg.text("player", item.entry().playerName()),
                        TextArg.text("time", item.entry().getFormattedTime())));
            }
        }
    }

    private Component progress(AllItemsObjectiveType type, int found, int total, double percent) {
        return Component.translatable("katze.allitems.command.progress",
                TextArg.component("type", Component.translatable("katze.allitems.type." + type.name().toLowerCase(Locale.ROOT))),
                TextArg.number("found", found),
                TextArg.number("total", total),
                TextArg.decimal("percent", percent));
    }

    @Override
    protected List<String> tabComplete(@NotNull CommandCaller caller, String[] args) {
        return List.of();
    }
}
