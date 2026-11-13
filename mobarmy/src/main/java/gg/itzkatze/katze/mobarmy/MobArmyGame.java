package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.game.GameTimer;
import gg.itzkatze.katze.game.SessionPersistence;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.results.ResultArchives;
import gg.itzkatze.katze.spectator.SpectatorManager;
import gg.itzkatze.katze.team.KatzeTeam;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.JsonFiles;
import gg.itzkatze.katze.util.WorldStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

final class MobArmyGame {

    private static final String FILE = "mobarmy.json";
    private static final NamespacedKey MATCH = new NamespacedKey("katzegames", "mobarmy_match");
    private static final NamespacedKey WAVE = new NamespacedKey("katzegames", "mobarmy_wave");
    final MobArmy challenge;
    final MobArmyArena arena = new MobArmyArena();
    final MobArmyAudience audience = new MobArmyAudience();
    final MobArmyTracker tracker = new MobArmyTracker(this);
    final Map<String, MobArmyMenus.Backpack> backpacks = new HashMap<>();
    MobArmyMatch match;
    private BukkitTask ticker;
    private boolean restoring;
    private int saveTicks;
    private int viewTicks;
    private boolean failed;
    boolean preparingArena;

    MobArmyGame(MobArmy challenge) {this.challenge = challenge;}

    static String time(MobArmyMatch.Side side) {
        return side.forfeited ? "DNF" : String.format(
                Locale.ROOT, "%d:%02d.%02d", side.elapsedTicks / 1200,
                side.elapsedTicks / 20 % 60, side.elapsedTicks % 20 * 5
        );
    }

    static void online(MobArmyMatch.Side side, Consumer<Player> action) {
        for (UUID uuid : side.members.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) action.accept(player);
        }
    }

    private static void showTitle(Player player, Component title, Component subtitle) {
        player.showTitle(Title.title(title, subtitle, Title.Times.times(Duration.ZERO, Duration.ofSeconds(5), Duration.ZERO)));
    }

    private static MobArmyMatch.LiveMob live(LivingEntity entity) {
        Location location = entity.getLocation();
        return new MobArmyMatch.LiveMob(MobCapture.serialize(entity), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    void enable() {ticker = Bukkit.getScheduler().runTaskTimer(KatzeGames.get(), this::safeTick, 1, 1);}

    void disable() {
        audience.restore();
        arena.cancelBuilds();
        if (ticker != null) ticker.cancel();
        closeMenus();
    }

    List<MobArmyMatch.Side> roster() {
        Map<String, MobArmyMatch.Side> sides = new LinkedHashMap<>();
        KatzePlayer.getOnlinePlayers().stream().filter(KatzePlayer::isParticipating)
                .sorted(Comparator.comparing(player -> player.getUuid().toString())).forEach(player -> {
                    KatzeTeam team = player.getTeam();
                    String id = team == null ? player.getUuid().toString() : "team-" + team.getId();
                    var side = sides.computeIfAbsent(
                            id, key -> {
                                var created = new MobArmyMatch.Side();
                                created.id = key;
                                return created;
                            }
                    );
                    side.members.put(player.getUuid(), player.getName());
                });
        return new ArrayList<>(sides.values());
    }

    void start() {
        failed = false;
        match = new MobArmyMatch();
        match.minecraftVersion = Bukkit.getMinecraftVersion();
        match.arenaFingerprint = arena.template().fingerprint();
        match.arenaWorldVersion = 1;
        match.sides = roster();
        backpacks.clear();
        if (challenge.settings.isEnabled(MobArmySettings.STARTER_EQUIPMENT)) {
            for (var side : match.sides)
                online(
                        side, player -> {
                            for (Material material : List.of(Material.DIAMOND_AXE, Material.DIAMOND_PICKAXE, Material.DIAMOND_SHOVEL)) {
                                ItemStack item = new ItemStack(material);
                                item.addEnchantment(Enchantment.EFFICIENCY, 1);
                                MobArmyEquipment.mark(item);
                                player.getInventory().addItem(item);
                            }
                        }
                );
        }
        for (var side : match.sides) online(side, this::rememberPlayer);
        match.validate();
        dirty();
    }

    MobArmyMatch.Side side(UUID uuid) {return match == null ? null : match.side(uuid);}

    boolean combat() {return match != null && match.phase == MobArmyMatch.Phase.COMBAT;}

    boolean protectedPhase() {return match != null && match.phase != MobArmyMatch.Phase.COLLECTION;}

    boolean running() {return !failed && ChallengeManager.isGameStarted() && KatzeGames.getTimer().isRunning();}

    boolean roof(Player player) {
        var side = side(player.getUniqueId());
        return side != null && (side.preparing || side.roofTicks.containsKey(player.getUniqueId()));
    }

    boolean blocked(Player player) {
        var side = side(player.getUniqueId());
        return protectedPhase() && (side == null || match.phase != MobArmyMatch.Phase.COMBAT || side.finished || roof(player));
    }

    void configure() {
        if (match == null || match.phase != MobArmyMatch.Phase.COLLECTION || !ChallengeManager.isGameStarted()) return;
        closeMenus();
        match.phase = MobArmyMatch.Phase.CONFIGURATION;
        audience.refresh(match);
        preparingArena = true;
        var timer = KatzeGames.getTimer();
        timer.setMode(GameTimer.Mode.COUNT_UP);
        timer.pause();
        arena.prepare(match.sides.size(), this::configurationReady, this::fail);
        dirty();
    }

    private void configurationReady() {
        if (match == null || match.phase != MobArmyMatch.Phase.CONFIGURATION) return;
        preparingArena = false;
        for (int i = 0; i < match.sides.size(); i++) {
            var side = match.sides.get(i);
            Location roof = arena.roof(i);
            online(
                    side, player -> {
                        rememberPlayer(player);
                        player.teleport(roof);
                        player.setFireTicks(0);
                        player.setGameMode(GameMode.ADVENTURE);
                    }
            );
            online(side, player -> new MobArmyMenus.Waves(this, side).open(KatzePlayer.adapt(player)));
        }
        KatzeGames.getTimer().start();
        audience.refresh(match);
        dirty();
    }

    void ready(MobArmyMatch.Side side) {
        if (!running() || match == null || match.phase != MobArmyMatch.Phase.CONFIGURATION) return;
        boolean all = match.toggleReady(side);
        refresh(side);
        if (all) beginCombat();
        dirty();
    }

    void allocate(MobArmyMatch.Side side, String type, int wave, boolean add) {
        if (running() && match.allocate(side, type, wave, add)) {
            refresh(side);
            dirty();
        }
    }

    private void beginCombat() {
        closeMenus();
        for (var side : match.sides) {
            if (side.forfeited) continue;
            online(side, this::rememberPlayer);
            side.baselines.putAll(side.players);
            side.backpackBaseline = PlayerLoadout.encode(backpack(side).getInventory().getContents());
            if (!side.baselines.keySet().containsAll(side.members.keySet())) throw new IllegalStateException("Missing participant loadout");
        }
        match.phase = MobArmyMatch.Phase.COMBAT;
        for (var side : match.sides)
            if (!side.forfeited) {
                resetSet(side, () -> beginWave(side));
            }
        dirty();
    }

    private void resetSet(MobArmyMatch.Side side, Runnable onReady) {
        int index = match.sides.indexOf(side);
        online(side, Player::closeInventory);
        side.live.clear();
        side.queue.clear();
        side.roofTicks.clear();
        side.deaths.clear();
        side.players.clear();
        side.players.putAll(side.baselines);
        side.pendingBaseline = new ArrayList<>(side.members.keySet());
        backpack(side).getInventory().setContents(PlayerLoadout.decode(side.backpackBaseline));
        Runnable enter = () -> {
            if (match == null || match.phase != MobArmyMatch.Phase.COMBAT || side.finished) return;
            side.preparing = false;
            enterSet(side);
            onReady.run();
            dirty();
        };
        if (side.opponent > 0) {
            side.preparing = true;
            online(side, player -> { if (!player.isDead()) { player.teleport(arena.roof(index)); player.setGameMode(GameMode.ADVENTURE); } });
            arena.rebuild(index, side, enter, this::fail);
        } else {
            arena.clearEntities(index);
            enter.run();
        }
    }

    private void enterSet(MobArmyMatch.Side side) {
        int index = match.sides.indexOf(side);
        online(
                side, player -> {
                    if (player.isDead()) return;
                    side.baselines.get(player.getUniqueId()).restore(player, false);
                    player.teleport(arena.entrance(index));
                    player.setGameMode(GameMode.SURVIVAL);
                    side.pendingBaseline.remove(player.getUniqueId());
                }
        );
    }

    private void beginWave(MobArmyMatch.Side side) {
        tracker.clear(side);
        match.prepareWave(side);
        online(
                side, player -> showTitle(
                        player, Component.translatable("katze.mobarmy.wave", TextArg.number("wave", side.wave + 1)),
                        Component.translatable("katze.mobarmy.begins")
                )
        );
    }

    private void safeTick() {
        if (failed) return;
        try {tick();} catch (RuntimeException exception) { fail(exception); }
    }

    private void fail(RuntimeException exception) {
        failed = true;
        arena.cancelBuilds();
        KatzeGames.getTimer().pause();
        KatzeGames.broadcast(Component.translatable("katze.mobarmy.error.runtime"));
        KatzeGames.get().getLogger().log(Level.SEVERE, "MobArmy paused after a gameplay failure", exception);
    }

    private void tick() {
        if (match == null || restoring) return;
        if (++viewTicks >= 20) {
            viewTicks = 0;
            arena.maintainEnvironment();
            audience.refresh(match);
            updateSpectators();
            tracker.update();
            if (match.phase == MobArmyMatch.Phase.RESULTS) archive();
        }
        if (!running()) return;
        if (++saveTicks >= 100) {
            saveTicks = 0;
            dirty();
        }
        if (combat()) {
            for (var side : match.sides) {
                if (side.finished || side.preparing) continue;
                match.tickClock(side, true);
                for (var entry : new ArrayList<>(side.roofTicks.entrySet())) {
                    long remaining = entry.getValue();
                    if (remaining <= 0) {
                        side.roofTicks.remove(entry.getKey());
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null && !player.isDead()) {
                            player.setGameMode(GameMode.SURVIVAL);
                            player.teleport(arena.entrance(match.sides.indexOf(side)));
                        }
                    } else side.roofTicks.put(entry.getKey(), remaining);
                }
                var mob = match.nextSpawn(side);
                if (mob != null) {
                    try {
                        LivingEntity entity = MobCapture.spawn(mob.data, arena.world(match.sides.indexOf(side)), true, living -> arena.spawn(match.sides.indexOf(side), living));
                        track(side, entity);
                    } catch (RuntimeException exception) {
                        side.queue.addFirst(mob);
                        throw exception;
                    }
                }
                for (UUID uuid : new ArrayList<>(side.live.keySet())) {
                    Entity entity = Bukkit.getEntity(uuid);
                    if (!(entity instanceof LivingEntity living) || living.isDead()) continue;
                    MobHabitat.maintain(living);
                    int index = match.sides.indexOf(side);
                    if (!arena.insideMob(index, living.getLocation())) living.teleport(arena.spawn(index, living));
                    if (living instanceof IronGolem golem) {
                        Player target = side.members.keySet().stream().map(Bukkit::getPlayer)
                                .filter(player -> player != null && !player.isDead() && !roof(player) && player.getWorld().equals(golem.getWorld()))
                                .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(golem.getLocation()))).orElse(null);
                        golem.setPlayerCreated(false);
                        golem.setTarget(target);
                    }
                }
                if (side.queue.isEmpty() && side.live.isEmpty()) advance(side);
            }
            if (match.sides.stream().allMatch(side -> side.finished)) results();
        }
        if (protectedPhase()) for (var side : match.sides) enforcePositions(side);
    }

    private void advance(MobArmyMatch.Side side) {
        var progress = match.advance(side);
        if (progress == MobArmyMatch.Progress.OPPONENT) {
            resetSet(side, () -> beginWave(side));
            dirty();
            return;
        }
        if (progress == MobArmyMatch.Progress.FINISHED) {
            online(
                    side, player -> {
                        showTitle(player, Component.translatable("katze.mobarmy.complete"), Component.empty());
                        finish(player);
                    }
            );
            dirty();
            return;
        }
        beginWave(side);
        dirty();
    }

    void track(MobArmyMatch.Side side, LivingEntity entity) {
        if (restoring || !combat() || side.finished || side.preparing || entity instanceof Player) return;
        entity.setPersistent(true);
        entity.setRemoveWhenFarAway(false);
        entity.getPersistentDataContainer().set(MATCH, PersistentDataType.STRING, match.id.toString());
        entity.getPersistentDataContainer().set(WAVE, PersistentDataType.STRING, side.id + ":" + side.opponent + ":" + side.wave);
        side.live.put(entity.getUniqueId(), live(entity));
        dirty();
    }

    MobArmyMatch.Side tracked(Entity entity) {
        return match == null ? null : match.sides.stream().filter(side -> side.live.containsKey(entity.getUniqueId())).findFirst().orElse(null);
    }

    void died(LivingEntity entity) {
        var side = tracked(entity);
        if (side != null) {
            side.live.remove(entity.getUniqueId());
            dirty();
        }
    }

    void death(Player player) {
        var side = side(player.getUniqueId());
        if (side == null || !combat() || side.finished) return;
        PlayerLoadout retained = PlayerLoadout.capture(player);
        retained.health = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        side.players.put(player.getUniqueId(), retained);
        int deaths = side.deaths.merge(player.getUniqueId(), 1, Integer::sum);
        side.roofTicks.put(player.getUniqueId(), MobArmyMatch.penaltySeconds(deaths) * 20L);
        dirty();
    }

    void join(Player player) {
        var side = side(player.getUniqueId());
        if (side == null || !protectedPhase()) return;
        if (side.finished) {
            finish(player);
            return;
        }
        if (player.isDead()) return;
        PlayerLoadout saved = side.players.get(player.getUniqueId());
        if (saved != null) saved.restore(player, match.phase == MobArmyMatch.Phase.COMBAT && saved.world.equals(MobArmyArena.worldKey(match.sides.indexOf(side)).toString()));
        side.pendingBaseline.remove(player.getUniqueId());
        player.setGameMode(match.phase == MobArmyMatch.Phase.CONFIGURATION || roof(player) ? GameMode.ADVENTURE : GameMode.SURVIVAL);
        enforcePositions(side);
        audience.refresh(match);
    }

    void respawn(Player player) {
        var side = side(player.getUniqueId());
        if (side == null || !protectedPhase()) return;
        if (side.finished) {
            finish(player);
            return;
        }
        if (side.pendingBaseline.remove(player.getUniqueId())) side.baselines.get(player.getUniqueId()).restore(player, false);
        player.setGameMode(match.phase == MobArmyMatch.Phase.CONFIGURATION || roof(player) ? GameMode.ADVENTURE : GameMode.SURVIVAL);
        enforcePositions(side);
        rememberPlayer(player);
    }

    void enforcePositions(MobArmyMatch.Side side) {
        if (side.finished) return;
        int index = match.sides.indexOf(side);
        online(
                side, player -> {
                    if (player.isDead()) return;
                    boolean roof = match.phase == MobArmyMatch.Phase.CONFIGURATION || roof(player);
                    Location location = player.getLocation();
                    if (!arena.ready(index)) return;
                    if (roof ? !arena.inside(index, location, true) || location.getY() < arena.roofHeight() + 1
                            : !arena.inside(index, location, false)) player.teleport(roof ? arena.roof(index) : arena.entrance(index));
                }
        );
    }

    void rememberPlayer(Player player) {
        var side = side(player.getUniqueId());
        if (side != null && !player.isDead() && !side.pendingBaseline.contains(player.getUniqueId())) side.players.put(
                player.getUniqueId(),
                PlayerLoadout.capture(player)
        );
    }

    private void finish(Player player) {
        SpectatorManager.finish(player.getUniqueId());
        audience.refresh(match);
        if (!player.isDead() && canWatch(player)) watchTargets().stream().findFirst().ifPresent(target -> watch(player, target.getName()));
    }

    boolean canWatch(Player player) {
        var side = side(player.getUniqueId());
        return combat() && (side == null || side.finished) && SpectatorManager.isSpectator(player.getUniqueId());
    }

    List<Player> watchTargets() {
        if (!combat()) return List.of();
        return match.sides.stream().filter(side -> !side.finished).flatMap(side -> side.members.keySet().stream())
                .map(Bukkit::getPlayer).filter(player -> player != null && !player.isDead() && arena.owns(player.getWorld()))
                .sorted(Comparator.comparing(this::roof)).toList();
    }

    boolean watch(Player viewer, String name) {
        if (!canWatch(viewer)) return false;
        Player target = watchTargets().stream().filter(player -> player.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) return false;
        viewer.setSpectatorTarget(null);
        viewer.teleport(target.getLocation());
        audience.refresh(match);
        return true;
    }

    private void updateSpectators() {
        if (!combat()) return;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!canWatch(viewer) || viewer.isDead()) continue;
            int viewed = arena.index(viewer.getLocation(), match.sides.size());
            if (viewed < 0 || match.sides.get(viewed).finished) {
                watchTargets().stream().findFirst().ifPresent(target -> watch(viewer, target.getName()));
            }
        }
    }

    void forfeit(MobArmyMatch.Side side) {
        if (match == null || !running() || match.phase == MobArmyMatch.Phase.COLLECTION || match.phase == MobArmyMatch.Phase.RESULTS || side.finished) return;
        side.finished = true;
        side.forfeited = true;
        side.preparing = false;
        side.ready = true;
        side.queue.clear();
        side.live.clear();
        arena.clearEntities(match.sides.indexOf(side));
        online(side, this::finish);
        if (match.phase == MobArmyMatch.Phase.CONFIGURATION && match.sides.stream().allMatch(entry -> entry.ready)) beginCombat();
        dirty();
    }

    private void results() {
        match.phase = MobArmyMatch.Phase.RESULTS;
        match.endedAt = System.currentTimeMillis();
        archive();
        audience.restore();
        String names = String.join(" / ", match.winners().stream().map(MobArmyMatch.Side::name).toList());
        for (Player player : Bukkit.getOnlinePlayers())
            showTitle(
                    player,
                    names.isEmpty() ? Component.translatable("katze.mobarmy.no_winner") : Component.text(names),
                    names.isEmpty() ? Component.empty() : Component.translatable("katze.mobarmy.won")
            );
        for (var side : match.sides) {
            Component row = Component.translatable(
                    "katze.mobarmy.result", TextArg.text("team", side.name()),
                    TextArg.number("count", side.mobs.size()), TextArg.text("time", time(side))
            );
            KatzeGames.broadcast(row.append(Component.space()).append(Component.translatable("katze.mobarmy.details")
                    .clickEvent(ClickEvent.runCommand(side.detailsCommand()))));
        }
        KatzeGames.getTimer().pause();
        dirty();
    }

    private void archive() {
        if (!match.archived && ResultArchives.save(MobArmyResults.capture(match))) {
            match.archived = true;
            dirty();
        }
    }

    MobArmyMenus.Backpack backpack(MobArmyMatch.Side side) {
        return backpacks.computeIfAbsent(
                side.id, key -> {
                    var gui = new MobArmyMenus.Backpack(this, side);
                    if (!side.backpack.isEmpty()) gui.getInventory().setContents(PlayerLoadout.decode(side.backpack));
                    return gui;
                }
        );
    }

    void refresh(MobArmyMatch.Side side) {
        online(
                side, player -> {
                    if (player.getOpenInventory().getTopInventory().getHolder() instanceof MobArmyMenus.Menu menu && menu.side == side) {
                        menu.setItems(KatzePlayer.adapt(player));
                        menu.updateInventory(KatzePlayer.adapt(player));
                    }
                }
        );
    }

    void dirty() {SessionPersistence.requestSave(ChallengeType.MOB_ARMY);}

    void closeMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MobArmyMenus.Menu) player.closeInventory();
        }
    }

    void save() {
        if (match == null) return;
        for (var side : match.sides) {
            online(side, this::rememberPlayer);
            side.backpack = PlayerLoadout.encode(backpack(side).getInventory().getContents());
            if (protectedPhase()) side.blocks = side.blocks.stream().map(change -> {
                var block = arena.world(match.sides.indexOf(side)).getBlockAt(change.x(), change.y(), change.z());
                List<String> contents = block.getState() instanceof InventoryHolder holder ? PlayerLoadout.encode(holder.getInventory().getContents()) : null;
                return new MobArmyMatch.BlockChange(change.x(), change.y(), change.z(), change.original(), change.placed(), block.getBlockData().getAsString(), contents);
            }).collect(Collectors.toCollection(ArrayList::new));
            for (UUID uuid : new ArrayList<>(side.live.keySet())) {
                if (Bukkit.getEntity(uuid) instanceof LivingEntity living && !living.isDead()) side.live.put(uuid, live(living));
            }
        }
        try {JsonFiles.write(WorldStorage.getFilePath(FILE), match);} catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to save MobArmy",
                    exception
            );
        }
    }

    void load() {
        try {match = JsonFiles.read(WorldStorage.getFilePath(FILE), MobArmyMatch.class);} catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to load MobArmy", exception);
        }
    }

    void restore() {
        if (match == null) throw new IllegalStateException("Saved MobArmy match is missing");
        match.validate();
        if (!Bukkit.getMinecraftVersion().equals(match.minecraftVersion)) throw new IllegalStateException("MobArmy entity data requires the original Minecraft version");
        if (protectedPhase() && !arena.template().fingerprint().equals(match.arenaFingerprint)) {
            throw new IllegalStateException("The saved MobArmy round uses a different arena; restore its village.schem or use /reset");
        }
        match.arenaFingerprint = arena.template().fingerprint();
        if (match.arenaWorldVersion != 1) throw new IllegalStateException("Unsupported MobArmy arena world version; use /reset to start a new round");
        if (match.phase == MobArmyMatch.Phase.RESULTS) archive();
        if (!protectedPhase()) return;
        restoring = true;
        try {
            for (int index = 0; index < match.sides.size(); index++) {
                var side = match.sides.get(index);
                arena.generate(index);
                for (var change : side.blocks) {
                    var block = arena.world(index).getBlockAt(change.x(), change.y(), change.z());
                    if (change.current() == null) throw new IllegalStateException("Missing arena block snapshot");
                    block.setBlockData(Bukkit.createBlockData(change.current()), false);
                    if (change.contents() != null && block.getState() instanceof InventoryHolder holder)
                        holder.getInventory().setContents(PlayerLoadout.decode(change.contents()));
                }
                arena.clearEntities(index);
                if (side.preparing) {
                    side.preparing = false;
                    if (match.phase == MobArmyMatch.Phase.COMBAT && !side.finished) match.prepareWave(side);
                }
                var saved = new ArrayList<>(side.live.values());
                side.live.clear();
                for (var mob : saved) {
                    Location position = new Location(arena.world(index), mob.x(), mob.y(), mob.z(), mob.yaw(), mob.pitch());
                    LivingEntity entity = MobCapture.spawn(mob.data(), position, false);
                    side.live.put(entity.getUniqueId(), live(entity));
                }
            }
        } finally {restoring = false;}
    }

    void reset() {
        audience.restore();
        closeMenus();
        match = null;
        failed = false;
        preparingArena = false;
        backpacks.clear();
        WorldStorage.delete(FILE);
        arena.cleanup();
    }

}
