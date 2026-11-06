package gg.itzkatze.katze.results;

import gg.itzkatze.katze.gui.GUIClickableItem;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.StringUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class SavedResultsGUI extends KatzeGUI {
    private static final int PAGE_SIZE = 45;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final UUID roundId;
    private final UUID playerId;
    private final Integer teamId;
    private final int page;

    private record Entry(ItemStack item, Consumer<KatzePlayer> action) {}

    public SavedResultsGUI() {
        this(null, null, null, 0);
    }

    private SavedResultsGUI(UUID roundId, UUID playerId, Integer teamId, int page) {
        super(Component.translatable("katze.results.title"), 54);
        this.roundId = roundId;
        this.playerId = playerId;
        this.teamId = teamId;
        this.page = page;
    }

    @Override
    public void setItems(KatzePlayer viewer) {
        fill(ItemStackCreator.fillerItem());
        List<Entry> entries;
        try {
            BattleResultsStore store = BattleResultsStore.installed();
            if (roundId == null) {
                entries = rounds(store.forPlayer(viewer.getUuid()));
            } else {
                SavedBattle battle = store.findForPlayer(roundId, viewer.getUuid()).orElse(null);
                if (battle == null) {
                    viewer.sendMessage(Component.translatable("katze.results.unavailable").color(KatzeColor.RED));
                    return;
                }
                title = Component.text(battle.game().displayName() + " · " + DATE.format(Instant.ofEpochMilli(battle.startedAt())));
                if (playerId != null) {
                    battle.players().stream().filter(player -> player.id().equals(playerId)).findFirst()
                            .ifPresent(player -> title = Component.translatable("katze.results.player_objectives", TextArg.text("player", player.name())));
                } else if (teamId != null) {
                    title = battle.game() == SavedBattle.Game.ALL_ITEMS ? Component.translatable("katze.results.shared_collection")
                            : Component.translatable("katze.results.team_objectives", TextArg.number("id", teamId));
                }
                entries = playerId == null && teamId == null ? standings(battle) : objectives(battle);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (entries.isEmpty()) {
            button(22, new Entry(ItemStackCreator.getStack(Component.translatable("katze.results.empty"), Material.BOOK, List.of()), ignored -> {}));
        }
        for (int slot = 0; slot < PAGE_SIZE && page * PAGE_SIZE + slot < entries.size(); slot++) {
            button(slot, entries.get(page * PAGE_SIZE + slot));
        }
        if (page > 0) set(GUIClickableItem.getGoBackItem(45, new SavedResultsGUI(roundId, playerId, teamId, page - 1)));
        if ((page + 1) * PAGE_SIZE < entries.size()) {
            set(GUIClickableItem.getGoForthItem(53, new SavedResultsGUI(roundId, playerId, teamId, page + 1)));
        }
        if (roundId != null) {
            SavedResultsGUI parent = playerId != null || teamId != null
                    ? new SavedResultsGUI(roundId, null, null, 0) : new SavedResultsGUI();
            set(GUIClickableItem.getGoBackItem(49, parent));
        } else {
            set(GUIClickableItem.getCloseItem(49));
        }
    }

    private List<Entry> rounds(List<SavedBattle> battles) {
        List<Entry> entries = new ArrayList<>();
        for (SavedBattle battle : battles) {
            Component name = Component.text(DATE.format(Instant.ofEpochMilli(battle.startedAt())), KatzeColor.GOLD);
            List<Component> lore = List.of(
                    Component.text(battle.game().displayName(), KatzeColor.LIGHT_BLUE),
                    Component.translatable("katze.results.duration", TextArg.text("time", StringUtility.formatTime(battle.durationSeconds()))),
                    Component.translatable("katze.results.players", TextArg.number("count", battle.players().size())),
                    Component.translatable("katze.results.open"));
            entries.add(new Entry(ItemStackCreator.getStack(name, Material.CLOCK, lore),
                    viewer -> new SavedResultsGUI(battle.id(), null, null, 0).open(viewer)));
        }
        return entries;
    }

    private List<Entry> standings(SavedBattle battle) {
        List<Entry> entries = new ArrayList<>();
        List<SavedBattle.Team> teams = battle.teams().stream().sorted(Comparator.comparingInt(SavedBattle.Team::points).reversed()).toList();
        for (SavedBattle.Team team : teams) {
            Component name = battle.game() == SavedBattle.Game.ALL_ITEMS ? Component.translatable("katze.results.shared_collection")
                    : Component.translatable("katze.results.team", TextArg.number("id", team.id()));
            name = name.color(KatzeColor.GOLD);
            List<Component> lore = new ArrayList<>();
            lore.add(score(battle, team.points()));
            battle.players().stream().filter(player -> Integer.valueOf(team.id()).equals(player.teamId()))
                    .forEach(player -> lore.add(Component.text(player.name(), KatzeColor.LIGHT_GRAY)));
            entries.add(new Entry(ItemStackCreator.getStack(name, Material.WHITE_BANNER, lore),
                    viewer -> new SavedResultsGUI(roundId, null, team.id(), 0).open(viewer)));
        }
        List<SavedBattle.Player> players = battle.players().stream().sorted(Comparator.comparingInt(SavedBattle.Player::points).reversed()).toList();
        for (SavedBattle.Player player : players) {
            List<Component> lore = new ArrayList<>();
            if (battle.game() != SavedBattle.Game.DEATH_SWAP) lore.add(score(battle, player.points()));
            lore.addAll(player.statistics().stream().map(SavedResultsGUI::statistic).toList());
            if (player.teamId() != null && battle.game() != SavedBattle.Game.ALL_ITEMS) {
                lore.add(Component.translatable("katze.results.team", TextArg.number("id", player.teamId())));
            }
            entries.add(new Entry(ItemStackCreator.getPlayerSkull(player.id(), Component.text(player.name(), KatzeColor.LIGHT_BLUE), 1, lore.toArray(Component[]::new)),
                    viewer -> new SavedResultsGUI(roundId, player.id(), null, 0).open(viewer)));
        }
        return entries;
    }

    private List<Entry> objectives(SavedBattle battle) {
        List<Entry> entries = new ArrayList<>();
        if (playerId != null) {
            battle.players().stream().filter(player -> player.id().equals(playerId)).findFirst().ifPresent(player -> {
                for (SavedBattle.Statistic stat : player.statistics()) {
                    entries.add(new Entry(ItemStackCreator.getStack(statistic(stat), Material.PAPER, List.of()), ignored -> {}));
                }
            });
        }
        List<SavedBattle.Objective> objectives = playerId != null
                ? battle.players().stream().filter(player -> player.id().equals(playerId)).findFirst().map(SavedBattle.Player::objectives).orElse(List.of())
                : battle.teams().stream().filter(team -> team.id() == teamId).findFirst().map(SavedBattle.Team::objectives).orElse(List.of());
        entries.addAll(objectives.stream().map(objective -> {
            Material icon = Material.matchMaterial(objective.icon());
            Component name = GsonComponentSerializer.gson().deserialize(objective.type())
                    .append(Component.text(": ")).append(GsonComponentSerializer.gson().deserialize(objective.name()));
            List<Component> lore = new ArrayList<>();
            if (objective.finished()) {
                if (!objective.finishedBy().isBlank()) lore.add(Component.translatable("katze.results.finished_by", TextArg.text("player", objective.finishedBy())));
                if (objective.remainingSeconds() >= 0) {
                    lore.add(Component.translatable("katze.results.remaining", TextArg.text("time", StringUtility.formatTime(objective.remainingSeconds()))));
                }
            } else {
                lore.add(Component.translatable("katze.results.unfinished").color(KatzeColor.YELLOW));
            }
            if (objective.skipped()) lore.add(Component.translatable("katze.results.skipped").color(KatzeColor.RED));
            return new Entry(ItemStackCreator.getStack(name, icon == null ? Material.BOOK : icon, lore), ignored -> {});
        }).toList());
        return entries;
    }

    private static Component score(SavedBattle battle, int points) {
        String key = battle.game() == SavedBattle.Game.ALL_ITEMS ? "discoveries" : "points";
        return Component.translatable("katze.results." + key, TextArg.number("points", points)).color(KatzeColor.YELLOW);
    }

    private static Component statistic(SavedBattle.Statistic statistic) {
        String value = statistic.key().equals("survived") ? StringUtility.formatTime(statistic.value())
                : statistic.total() >= 0 ? statistic.value() + "/" + statistic.total() : Integer.toString(statistic.value());
        return Component.translatable("katze.results.stat." + statistic.key(), TextArg.text("value", value)).color(KatzeColor.LIGHT_GRAY);
    }

    private void button(int slot, Entry entry) {
        set(new GUIClickableItem(slot) {
            @Override
            public void run(InventoryClickEvent event, KatzePlayer player) {
                entry.action().accept(player);
            }

            @Override
            public ItemStack getItem(KatzePlayer player) {
                return entry.item();
            }
        });
    }

    @Override
    public boolean allowItemMoving() {
        return false;
    }
}
