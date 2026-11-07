package gg.itzkatze.katze.bingo.match;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.achievements.AchievementService;
import gg.itzkatze.katze.bingo.BingoSettings;
import gg.itzkatze.katze.bingo.board.BingoBoard;
import gg.itzkatze.katze.bingo.board.BingoBoardGenerator;
import gg.itzkatze.katze.bingo.board.BingoObjective;
import gg.itzkatze.katze.bingo.board.BingoObjectiveCatalog;
import gg.itzkatze.katze.bingo.pack.BingoResourcePack;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.game.GameWorldManager;
import gg.itzkatze.katze.game.SessionPersistence;
import gg.itzkatze.katze.game.seed.SeedRequirement;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.bingo.gui.BingoBoardGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.results.ResultArchives;
import gg.itzkatze.katze.results.SavedBattle;
import gg.itzkatze.katze.spectator.SpectatorManager;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.JsonFiles;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.StructureScanner;
import gg.itzkatze.katze.util.WorldStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class BingoMatchManager {
    private static final String FILE = "bingo.json";
    private final BingoObjectiveCatalog catalog;
    private final BingoResourcePack pack;
    private final List<BingoMatch> matches = new ArrayList<>();
    private final Map<UUID, UUID> selected = new HashMap<>();
    private final Set<UUID> hidden = new HashSet<>();
    private List<BingoCompetitor> preparedRoster = List.of();
    private List<List<String>> preparedBoards = List.of();
    private List<String> preparedPool = List.of();
    private int lastTick;
    private int archiveTick;
    private boolean roundEnded;
    private long generationSeed;
    private BingoResultArchive archives;

    private record State(int version, String catalogue, List<BingoMatch> matches, boolean ended) {}

    public BingoMatchManager(BingoObjectiveCatalog catalog, BingoResourcePack pack) { this.catalog = catalog; this.pack = pack; }
    public BingoObjectiveCatalog catalog() { return catalog; }
    public List<BingoMatch> matches() { return List.copyOf(matches); }
    public boolean packReady(UUID player) { return pack.ready(player); }
    public boolean hidden(UUID player) { return hidden.contains(player); }
    public void toggleOverlay(UUID player) { if (!hidden.remove(player)) hidden.add(player); }
    public boolean started() { return !matches.isEmpty(); }
    public boolean ended() { return roundEnded; }

    public void setTime(int seconds) {
        if (!ChallengeManager.isGameStarted()) return;
        for (BingoMatch match : matches) if (match.result == BingoMatch.Result.ACTIVE) {
            match.remaining = Math.max(0, seconds);
            if (match.remaining == 0) { match.timeout(); finish(match); }
        }
        dirty();
    }

    public List<BingoCompetitor> roster() {
        Map<String, List<KatzePlayer>> groups = new LinkedHashMap<>();
        KatzePlayer.getOnlinePlayers().stream().filter(KatzePlayer::isParticipating).sorted(Comparator.comparing(player -> player.getUuid().toString()))
                .forEach(player -> groups.computeIfAbsent(player.isInTeam() ? "team/" + player.getTeam().getId() : "player/" + player.getUuid(), ignored -> new ArrayList<>()).add(player));
        return groups.entrySet().stream().map(entry -> {
            KatzePlayer first = entry.getValue().getFirst();
            Integer team = first.isInTeam() ? first.getTeam().getId() : null;
            return new BingoCompetitor(entry.getKey(), team == null ? first.getName() : "Team " + team, team,
                    entry.getValue().stream().map(player -> new BingoCompetitor.Member(player.getUuid(), player.getName())).toList());
        }).toList();
    }

    public Component startError() {
        if (ChallengeManager.getActiveChallenges().size() != 1) return Component.translatable("katze.bingo.error.combination");
        List<BingoCompetitor> roster = roster();
        if (roster.size() < 2 || roster.size() % 2 != 0) return Component.translatable("katze.bingo.error.competitors");
        if (!BingoSettings.unequalTeams() && roster.stream().map(competitor -> competitor.members().size()).distinct().count() != 1)
            return Component.translatable("katze.bingo.error.teams");
        if (Bukkit.getOnlinePlayers().stream().anyMatch(player -> !pack.ready(player.getUniqueId()))) return Component.translatable("katze.bingo.error.pack_wait");
        try { prepare(roster); }
        catch (IllegalArgumentException exception) { return Component.translatable(exception.getMessage()); }
        return null;
    }

    private void prepare(List<BingoCompetitor> roster) {
        List<BingoObjective> pool = catalog.enabled().stream().filter(objective -> objective.dimension().equals("NORMAL")
                || GameWorldManager.getWorlds().stream().anyMatch(world -> world.getEnvironment().name().equals(objective.dimension()))).toList();
        List<String> ids = pool.stream().map(BingoObjective::id).toList();
        if (roster.equals(preparedRoster) && ids.equals(preparedPool) && !preparedBoards.isEmpty()) return;
        generationSeed = new Random().nextLong();
        List<List<String>> boards = new ArrayList<>();
        for (int i = 0; i < roster.size() / 2; i++) boards.add(new BingoBoardGenerator().generate(pool, generationSeed + i));
        preparedRoster = List.copyOf(roster);
        preparedPool = ids;
        preparedBoards = List.copyOf(boards);
    }

    public List<SeedRequirement> requirements() {
        prepare(roster());
        return preparedBoards.stream().flatMap(board -> catalog.requirements(board).stream()).distinct().toList();
    }

    public void start() {
        prepare(roster());
        List<BingoCompetitor> roster = new ArrayList<>(preparedRoster);
        Collections.shuffle(roster, new Random(generationSeed));
        matches.clear();
        roundEnded = false;
        lastTick = Bukkit.getCurrentTick();
        for (int i = 0; i < roster.size() / 2; i++) matches.add(new BingoMatch(roster.subList(i * 2, i * 2 + 2), preparedBoards.get(i), BingoSettings.seconds(), generationSeed + i));
        for (Player player : Bukkit.getOnlinePlayers()) {
            BingoMatch match = assigned(player.getUniqueId());
            if (match == null) continue;
            for (String objectiveId : match.objectives) {
                BingoObjective objective = catalog.get(objectiveId);
                if (objective.category() != BingoObjective.Category.ADVANCEMENT) continue;
                var advancement = Bukkit.getAdvancement(NamespacedKey.minecraft(objective.target()));
                if (advancement == null) throw new IllegalStateException("Missing Bingo advancement: " + objective.target());
                var progress = player.getAdvancementProgress(advancement);
                for (String criterion : List.copyOf(progress.getAwardedCriteria())) progress.revokeCriteria(criterion);
            }
            player.sendMessage(Component.translatable("katze.bingo.start", TextArg.text("opponent", match.competitors.get(1 - match.side(player.getUniqueId())).name())));
        }
        dirty();
    }

    public BingoMatch assigned(UUID player) { return matches.stream().filter(match -> match.side(player) >= 0).findFirst().orElse(null); }
    public BingoMatch find(UUID id) { return matches.stream().filter(match -> match.id.equals(id)).findFirst().orElse(null); }
    public BingoMatch viewed(UUID player) {
        BingoMatch own = assigned(player);
        if (own != null && own.result == BingoMatch.Result.ACTIVE) return own;
        BingoMatch selection = find(selected.get(player));
        return selection != null ? selection : own;
    }

    public void select(UUID player, UUID board) {
        BingoMatch own = assigned(player);
        if (own == null || own.result != BingoMatch.Result.ACTIVE) selected.put(player, board);
    }

    public boolean online(BingoMatch match, int side) {
        return match.competitors.get(side).members().stream().anyMatch(member -> {
            Player player = Bukkit.getPlayer(member.id());
            return player != null && pack.ready(member.id()) && !SpectatorManager.isSpectator(member.id());
        });
    }

    public boolean active(BingoMatch match) {
        return ChallengeManager.isGameStarted() && KatzeGames.getTimer().isRunning() && match != null
                && match.result == BingoMatch.Result.ACTIVE && online(match, 0) && online(match, 1);
    }

    public boolean eligible(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL && pack.ready(player.getUniqueId())
                && !SpectatorManager.isSpectator(player.getUniqueId()) && active(assigned(player.getUniqueId()));
    }

    public boolean frozen(Player player) {
        if (!pack.ready(player.getUniqueId())) return true;
        BingoMatch match = assigned(player.getUniqueId());
        return ChallengeManager.isGameStarted() && match != null && match.result == BingoMatch.Result.ACTIVE && !active(match);
    }

    public int progress(Player player, BingoObjective objective) {
        if (objective.category() != BingoObjective.Category.ITEM) return 0;
        int count = 0;
        Material material = Material.valueOf(objective.target());
        for (ItemStack item : player.getInventory().getStorageContents()) if (item != null && item.getType() == material) count += item.getAmount();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() == material) count += offhand.getAmount();
        return Math.min(count, objective.count());
    }

    private boolean satisfied(Player player, BingoObjective objective) {
        return switch (objective.category()) {
            case ITEM -> progress(player, objective) >= objective.count();
            case BIOME -> catalog.matches(objective, player.getWorld().getBiome(player.getLocation()).getKey().getKey());
            case STRUCTURE -> catalog.targets(objective).stream().anyMatch(key -> StructureScanner.contains(player.getLocation(), NamespacedKey.minecraft(key)));
            default -> false;
        };
    }

    public void completed(Player player, BingoObjective.Category category, String target) {
        if (!eligible(player)) return;
        BingoMatch match = assigned(player.getUniqueId());
        for (int cell = 0; cell < 25 && match.result == BingoMatch.Result.ACTIVE; cell++) {
            BingoObjective objective = catalog.get(match.objectives.get(cell));
            if (objective.category() == category && catalog.matches(objective, target)) claim(match, cell, player);
        }
    }

    private void claim(BingoMatch match, int cell, Player player) {
        if (!eligible(player) || !match.claim(cell, match.side(player.getUniqueId()), player.getUniqueId())) return;
        notify(match, Component.translatable("katze.bingo.claim", TextArg.text("owner", match.competitors.get(match.owners[cell]).name()),
                TextArg.text("cell", BingoBoard.coordinate(cell)), TextArg.component("objective", catalog.name(match.objectives.get(cell)))));
        if (match.result != BingoMatch.Result.ACTIVE) finish(match);
        dirty();
        refresh();
    }

    public void tick() {
        pack.tick();
        for (Player player : Bukkit.getOnlinePlayers()) pack.hold(player);
        if (Bukkit.getCurrentTick() - archiveTick >= 200) {
            archiveTick = Bukkit.getCurrentTick();
            if (archives != null) archives.retry();
            matches.stream().filter(match -> match.result != BingoMatch.Result.ACTIVE && !match.archived).forEach(this::archive);
        }
        if (!ChallengeManager.isGameStarted() || matches.isEmpty()) return;
        for (BingoMatch match : matches) {
            if (!active(match)) continue;
            for (int cell = 0; cell < 25 && match.result == BingoMatch.Result.ACTIVE; cell++) {
                if (match.owners[cell] != -1) continue;
                BingoObjective objective = catalog.get(match.objectives.get(cell));
                for (int offset = 0; offset < 2 && match.owners[cell] == -1; offset++) {
                    int side = (match.tiePriority + offset) % 2;
                    for (BingoCompetitor.Member member : match.competitors.get(side).members()) {
                        Player player = Bukkit.getPlayer(member.id());
                        if (player != null && eligible(player) && satisfied(player, objective)) { claim(match, cell, player); break; }
                    }
                }
            }
            match.tiePriority = 1 - match.tiePriority;
        }
        int tick = Bukkit.getCurrentTick();
        if (tick - lastTick >= 20) {
            lastTick = tick;
            if (KatzeGames.getTimer().isRunning()) {
                for (BingoMatch match : matches) {
                    if (match.result != BingoMatch.Result.ACTIVE) continue;
                    match.tick(online(match, 0), online(match, 1));
                    if (match.result != BingoMatch.Result.ACTIVE) finish(match);
                }
                dirty();
            }
            refresh();
        }
        for (BingoMatch match : matches) if (match.result != BingoMatch.Result.ACTIVE) {
            for (BingoCompetitor competitor : match.competitors) for (BingoCompetitor.Member member : competitor.members()) {
                SpectatorManager.finish(member.id());
            }
        }
        if (!roundEnded && matches.stream().noneMatch(match -> match.result == BingoMatch.Result.ACTIVE)) {
            roundEnded = true;
            KatzeGames.getTimer().pause();
            Set<UUID> winners = new HashSet<>();
            Set<UUID> teamWinners = new HashSet<>();
            for (BingoMatch match : matches) if (match.winner >= 0) {
                BingoCompetitor winner = match.competitors.get(match.winner);
                for (BingoCompetitor.Member member : winner.members()) {
                    winners.add(member.id());
                    if (winner.teamId() != null) teamWinners.add(member.id());
                }
            }
            AchievementService.get().naturalEnd(ChallengeType.BINGO, winners, teamWinners);
            dirty();
        }
    }

    private void finish(BingoMatch match) {
        Component result = match.winner >= 0 ? Component.translatable("katze.bingo.winner", TextArg.text("winner", match.competitors.get(match.winner).name()))
                : Component.translatable("katze.bingo.draw");
        notify(match, result.append(Component.text(" · ")).append(Component.translatable("katze.bingo.result." + match.result.name().toLowerCase(Locale.ROOT))));
        for (BingoCompetitor competitor : match.competitors) for (BingoCompetitor.Member member : competitor.members()) {
            Player player = Bukkit.getPlayer(member.id());
            if (player != null) player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        }
        archive(match);
        dirty();
    }

    public void adminDraw(BingoMatch match) {
        if (match != null && match.result == BingoMatch.Result.ACTIVE) { match.finish(BingoMatch.Result.ADMIN_DRAW, -1); finish(match); }
    }

    private void notify(BingoMatch match, Component message) {
        for (BingoCompetitor competitor : match.competitors) for (BingoCompetitor.Member member : competitor.members()) {
            Player player = Bukkit.getPlayer(member.id());
            if (player != null) player.sendMessage(message.color(KatzeColor.GOLD));
        }
    }

    private void refresh() {
        for (Map.Entry<UUID, KatzeGUI> entry : Map.copyOf(KatzeGUI.GUI_MAP).entrySet()) {
            if (entry.getValue() instanceof BingoBoardGUI gui) KatzePlayer.adapt(entry.getKey()).ifPresent(gui::updateInventoryForPlayer);
        }
    }

    public void save() {
        try { JsonFiles.write(WorldStorage.getFilePath(FILE), new State(1, catalog.fingerprint(), matches, roundEnded)); }
        catch (IOException exception) { throw new UncheckedIOException(exception); }
    }

    public void load() {
        matches.clear();
        archives = new BingoResultArchive(KatzeGames.get().getDataDirectory().toPath().resolve("data/bingo-pending-results.json"), ResultArchives::save);
        try {
            State state = JsonFiles.read(WorldStorage.getFilePath(FILE), State.class);
            if (state == null) return;
            if (state.version() != 1 || !catalog.fingerprint().equals(state.catalogue()) || state.matches() == null)
                throw new IOException("Unsupported Bingo save or changed objective catalogue");
            Set<UUID> members = new HashSet<>();
            Set<UUID> matchIds = new HashSet<>();
            for (BingoMatch match : state.matches()) {
                if (match.id == null || match.competitors == null || match.competitors.size() != 2 || match.objectives == null || match.objectives.size() != 25
                        || match.owners == null || match.owners.length != 25 || match.contributors == null || match.contributors.length != 25
                        || match.claimedAt == null || match.claimedAt.length != 25 || match.grace == null || match.grace.length != 2
                        || match.result == null || match.winningLine == null || match.tiePriority < 0 || match.tiePriority > 1) throw new IOException("Invalid Bingo match");
                List<BingoObjective> objectives = match.objectives.stream().map(catalog::get).toList();
                if (!new BingoBoardGenerator().balanced(objectives)) throw new IOException("Invalid saved Bingo board");
                if (!matchIds.add(match.id) || match.elapsed < 0 || match.remaining < -1 || match.winner < -1 || match.winner > 1)
                    throw new IOException("Invalid Bingo match identity or clock");
                for (int grace : match.grace) if (grace < 0 || grace > 120) throw new IOException("Invalid reconnect grace");
                for (int cell = 0; cell < 25; cell++) {
                    int owner = match.owners[cell];
                    if (owner < -1 || owner > 1) throw new IOException("Invalid Bingo owner");
                    if (owner == -1 && (match.contributors[cell] != null || match.claimedAt[cell] != -1)) throw new IOException("Unclaimed cell has claim data");
                    if (owner >= 0 && (match.contributors[cell] == null || !match.competitors.get(owner).contains(match.contributors[cell])
                            || match.claimedAt[cell] < 0 || match.claimedAt[cell] > match.elapsed)) throw new IOException("Invalid Bingo contributor");
                }
                if (match.result == BingoMatch.Result.LINE && (match.winner < 0 || match.winningLine.isEmpty()
                        || !BingoBoard.LINES.contains(match.winningLine) || match.winningLine.stream().anyMatch(cell -> match.owners[cell] != match.winner)))
                    throw new IOException("Invalid winning Bingo line");
                if (match.result == BingoMatch.Result.ACTIVE && (match.winner != -1 || !match.winningLine.isEmpty()
                        || !BingoBoard.winningLine(match.owners, 0).isEmpty() || !BingoBoard.winningLine(match.owners, 1).isEmpty()))
                    throw new IOException("Active Bingo match has a winner");
                for (BingoCompetitor competitor : match.competitors) for (BingoCompetitor.Member member : competitor.members()) {
                    if (!members.add(member.id())) throw new IOException("Duplicate Bingo participant");
                }
            }
            matches.addAll(state.matches());
            roundEnded = state.ended();
            lastTick = Bukkit.getCurrentTick();
        } catch (IOException | RuntimeException exception) { throw new IllegalStateException("Bingo recovery stopped; preserve " + WorldStorage.getFilePath(FILE) + " and repair the save before retrying", exception); }
    }

    public void reset() {
        for (BingoMatch match : matches) {
            if (match.result == BingoMatch.Result.ACTIVE) match.finish(BingoMatch.Result.ABORTED, -1);
            if (!match.archived) archive(match);
        }
        matches.clear();
        selected.clear();
        preparedRoster = List.of();
        preparedBoards = List.of();
        preparedPool = List.of();
        roundEnded = false;
        WorldStorage.delete(FILE);
    }

    private void archive(BingoMatch match) {
        List<SavedBattle.Player> players = new ArrayList<>();
        for (BingoCompetitor competitor : match.competitors) for (BingoCompetitor.Member member : competitor.members()) {
            int contributions = 0;
            for (UUID contributor : match.contributors) if (member.id().equals(contributor)) contributions++;
            players.add(new SavedBattle.Player(member.id(), member.name(), competitor.teamId(), contributions, List.of()));
        }
        List<SavedBattle.BingoCell> cells = new ArrayList<>();
        for (int cell = 0; cell < 25; cell++) {
            UUID contributor = match.contributors[cell];
            String contributorName = players.stream().filter(player -> player.id().equals(contributor)).map(SavedBattle.Player::name).findFirst().orElse("");
            String id = match.objectives.get(cell);
            cells.add(new SavedBattle.BingoCell(GsonComponentSerializer.gson().serialize(catalog.name(id)),
                    GsonComponentSerializer.gson().serialize(catalog.difficulty(id)), catalog.icon(id).name(), match.owners[cell], contributorName,
                    match.claimedAt[cell], null));
        }
        SavedBattle.Bingo data = new SavedBattle.Bingo(match.competitors.stream().map(BingoCompetitor::name).toList(), match.winner,
                match.result.name().toLowerCase(Locale.ROOT), match.winningLine, cells);
        archives.submit(new SavedBattle(match.id, match.startedAt, match.endedAt, match.elapsed, players, List.of(), SavedBattle.Game.BINGO, data));
        match.archived = true;
    }

    private void dirty() { SessionPersistence.requestSave(ChallengeType.BINGO); }
}
