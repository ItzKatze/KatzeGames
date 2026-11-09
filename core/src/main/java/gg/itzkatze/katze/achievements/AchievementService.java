package gg.itzkatze.katze.achievements;

import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.game.SessionPersistence;
import gg.itzkatze.katze.level.GameXp;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.player.data.PlayerDataStore;
import gg.itzkatze.katze.player.data.PlayerProfile;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.WorldStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class AchievementService {
    public record Unlock(Achievement achievement, List<AchievementStage> stages, int xp, int oldLevel, int newLevel) {}

    private static AchievementService installed;
    private final PlayerDataStore data;
    private final AchievementNamePack customNames;
    private final BiConsumer<UUID, Unlock> notice;
    private final Consumer<UUID> refresh;
    private final Map<UUID, Integer> jumpStreaks = new HashMap<>();
    private final Set<UUID> retries = new HashSet<>();
    private AchievementRound round;

    public AchievementService(PlayerDataStore data, BiConsumer<UUID, Unlock> notice, Consumer<UUID> refresh) {
        this(data, notice, refresh, AchievementNamePack.custom());
    }

    AchievementService(PlayerDataStore data, BiConsumer<UUID, Unlock> notice, Consumer<UUID> refresh, AchievementNamePack customNames) {
        this.data = data;
        this.customNames = customNames;
        this.notice = notice;
        this.refresh = refresh;
        data.profiles().forEach((id, profile) -> {
            if (!profile.achievements().pending().isEmpty()) retries.add(id);
        });
    }

    public static AchievementService get() {
        if (installed == null) throw new IllegalStateException("Achievements have not been loaded");
        return installed;
    }

    public static void load() {
        installed = new AchievementService(PlayerDataStore.get(), AchievementService::notifyPlayer, AchievementGui::refresh);
    }

    private static void notifyPlayer(UUID id, Unlock unlock) {
        Player player = Bukkit.getPlayer(id);
        if (player == null) return;
        player.sendMessage(get().unlockMessage(id, unlock, player.locale()));
        if (unlock.newLevel() > unlock.oldLevel()) {
            player.sendMessage(Component.translatable("katze.achievements.level_up", TextArg.number("level", unlock.newLevel())));
        }
    }

    Component unlockMessage(UUID id, Unlock unlock, Locale locale) {
        boolean staged = unlock.achievement().stages().size() > 1;
        Component name = title(id, unlock.achievement());
        Component requirement = unlock.achievement().description(unlock.stages().getLast());
        if (staged) {
            Component stages = Component.join(JoinConfiguration.separator(Component.text(", ")), unlock.stages().stream()
                    .map(stage -> Component.translatable("katze.achievements.stage", TextArg.text("stage", stage.id()))).toList());
            name = name.append(Component.text(" (")).append(stages).append(Component.text(")"));
        }
        Component tooltip = GlobalTranslator.render(Component.empty().decoration(TextDecoration.OBFUSCATED, false)
                .append(requirement.color(KatzeColor.LIGHT_GRAY)).append(Component.newline())
                .append(Component.translatable("katze.achievements.unlock_xp", TextArg.number("xp", unlock.xp())).color(KatzeColor.LIME)), locale);
        Component achievement = name.color(KatzeColor.GOLD).hoverEvent(tooltip);
        return GlobalTranslator.render(Component.translatable("katze.achievements.unlocked", TextArg.component("achievement", achievement)), locale);
    }

    public AchievementProfile profile(UUID player) {
        PlayerProfile profile = data.find(player);
        return profile == null ? new AchievementProfile() : profile.achievements();
    }

    public boolean usesCustomNames(UUID player) {
        PlayerProfile profile = data.find(player);
        return profile != null && profile.customAchievementNames();
    }

    public Component title(UUID player, Achievement achievement) {
        return usesCustomNames(player) ? customNames.title(achievement) : achievement.title();
    }

    public boolean setCustomNames(UUID player, String name, boolean enabled) {
        return data.transaction(player, () -> {
            data.profile(player, name).setCustomAchievementNames(enabled);
            data.markDirty();
        });
    }

    public void progress(UUID player, String name, Achievement achievement, int value) {
        value = Math.clamp(value, 0, achievement.maximum());
        AchievementProfile profile = data.profile(player, name).achievements();
        boolean changed = profile.recordBest(achievement, value);
        int target = value;
        if (achievement.stages().stream().anyMatch(stage -> target >= stage.target() && !profile.completed(achievement, stage))) {
            profile.queue(achievement, value);
            retries.add(player);
            data.markDirty();
            settle(player, achievement, value);
        } else if (changed) {
            data.markDirty();
        }
        if (changed) refresh.accept(player);
    }

    private void settle(UUID player, Achievement achievement, int value) {
        AchievementProfile profile = profile(player);
        List<AchievementStage> earned = achievement.stages().stream()
                .filter(stage -> value >= stage.target() && !profile.completed(achievement, stage)).toList();
        if (earned.isEmpty()) {
            if (profile.pending().containsKey(achievement.id())) {
                profile.settled(achievement);
                data.markDirty();
            }
            return;
        }
        int before = profile.xp();
        int oldLevel = profile.level().level();
        PlayerProfile playerProfile = data.find(player);
        String name = playerProfile == null ? null : playerProfile.name();
        long timestamp = System.currentTimeMillis();
        if (data.transaction(player, () -> {
            AchievementProfile current = data.profile(player, name).achievements();
            current.recordBest(achievement, value);
            earned.forEach(stage -> current.complete(achievement, stage, timestamp));
            current.settled(achievement);
            data.markDirty();
        })) {
            AchievementProfile saved = profile(player);
            notice.accept(player, new Unlock(achievement, earned, saved.xp() - before, oldLevel, saved.level().level()));
            refresh.accept(player);
        }
    }

    public void retryPending() {
        for (UUID player : Set.copyOf(retries)) {
            profile(player).pending().forEach((key, value) -> Arrays.stream(Achievement.values())
                    .filter(achievement -> achievement.id().equals(key)).findFirst()
                    .ifPresent(achievement -> settle(player, achievement, value)));
            if (profile(player).pending().isEmpty()) retries.remove(player);
        }
    }

    public AchievementRound round() { return round; }

    public UUID roundId() { return round == null ? null : round.id(); }

    public void begin() {
        begin(ChallengeManager.getActiveChallenges().stream().map(challenge -> challenge.getType()).collect(Collectors.toSet()),
                KatzePlayer.PLAYERS.stream().filter(KatzePlayer::isParticipating).filter(player -> !player.isOffline())
                        .collect(Collectors.toMap(KatzePlayer::getUuid, KatzePlayer::getName)));
    }

    public void begin(Set<ChallengeType> challenges, Map<UUID, String> participants) {
        round = new AchievementRound();
        round.initialChallenges().addAll(challenges);
        participants.forEach((id, name) -> {
            AchievementRound.Participant player = round.player(id);
            player.name(name);
            player.fullRound(true);
            refresh.accept(id);
        });
    }

    public void reset() {
        retryPending();
        Set<UUID> players = round == null ? Set.of() : Set.copyOf(round.players().keySet());
        round = null;
        players.forEach(refresh);
    }

    public void save() {
        if (round == null) WorldStorage.delete("achievements_game.json");
        else WorldStorage.save("achievements_game.json", round);
    }

    public void restore(UUID expectedId) {
        AchievementRound loaded = WorldStorage.load("achievements_game.json", AchievementRound.class);
        restore(loaded, expectedId);
        if (round == null) WorldStorage.delete("achievements_game.json");
    }

    void restore(AchievementRound loaded, UUID expectedId) {
        round = expectedId != null && loaded != null && loaded.normalize() && expectedId.equals(loaded.id()) ? loaded : null;
    }

    public boolean eligible(Player player) {
        return round != null && !round.finished() && GameXp.isGameRunning()
                && player.getGameMode() == GameMode.SURVIVAL && KatzePlayer.adapt(player).isParticipating();
    }

    public boolean forceBattleEligible(Player player) {
        return eligible(player, Achievement.INDIVIDUAL_POINTS);
    }

    boolean tracks(Achievement achievement) {
        ChallengeType challenge = achievement.category().getChallenge();
        return challenge == null || round != null && round.initialChallenges().contains(challenge) && !round.ended().contains(challenge);
    }

    private boolean eligible(Player player, Achievement achievement) {
        ChallengeType challenge = achievement.category().getChallenge();
        return tracks(achievement) && (challenge == null || ChallengeManager.isChallengeActive(challenge)) && eligible(player);
    }

    public void record(Player player, Achievement achievement) {
        snapshot(player, achievement, 1);
    }

    public void snapshot(Player player, Achievement achievement, int value) {
        if (!eligible(player, achievement)) return;
        AchievementRound.Participant state = participant(player);
        if (state.value(achievement) == value) return;
        state.set(achievement, value);
        progress(player.getUniqueId(), player.getName(), achievement, value);
        refresh.accept(player.getUniqueId());
        SessionPersistence.requestSave();
    }

    public void add(Player player, Achievement achievement, int amount) {
        if (!eligible(player, achievement) || amount <= 0) return;
        AchievementRound.Participant state = participant(player);
        int value = state.add(achievement, amount);
        progress(player.getUniqueId(), player.getName(), achievement, value);
        refresh.accept(player.getUniqueId());
        SessionPersistence.requestSave();
    }

    public void distinct(Player player, Achievement achievement, String key) {
        if (!eligible(player, achievement)) return;
        AchievementRound.Participant state = participant(player);
        int before = state.value(achievement);
        int value = state.distinct(achievement, key);
        if (before == value) return;
        progress(player.getUniqueId(), player.getName(), achievement, value);
        refresh.accept(player.getUniqueId());
        SessionPersistence.requestSave();
    }

    public AchievementRound.Participant participant(Player player) {
        AchievementRound.Participant state = round.player(player.getUniqueId());
        state.name(player.getName());
        return state;
    }

    public void disqualify(UUID player) {
        if (round == null || !round.players().containsKey(player) || !round.player(player).fullRound()) return;
        round.player(player).fullRound(false);
        SessionPersistence.requestSave();
    }

    public void challengeDisabled(ChallengeType type) {
        if (round == null || round.finished()) return;
        if (type == ChallengeType.FORCE_BATTLE) round.players().values().forEach(AchievementRound.Participant::useJoker);
        SessionPersistence.requestSave();
    }

    public void recordObjective(Player finisher, boolean team) {
        if (!forceBattleEligible(finisher)) return;
        add(finisher, Achievement.INDIVIDUAL_POINTS, 1);
        if (team) add(finisher, Achievement.TEAM_OBJECTIVES, 1);
    }

    public void recordAssignment(Player player, Material material) {
        if (forceBattleEligible(player) && player.getInventory().contains(material)) record(player, Achievement.ALREADY_HAVE_OBJECTIVE);
    }

    public void recordJoker(UUID player) {
        if (round == null) return;
        round.player(player).useJoker();
        SessionPersistence.requestSave();
    }

    public void naturalEnd(ChallengeType challenge, Set<UUID> winners, Set<UUID> teamWinners) {
        if (round == null || !round.initialChallenges().contains(challenge)) return;
        if (!round.end(challenge)) return;
        if (Achievement.VEGAN_ROUND.category().getChallenge() == challenge) round.players().forEach((id, state) -> {
            if (state.vegan()) progress(id, state.name(), Achievement.VEGAN_ROUND, 1);
        });
        Map<UUID, AchievementRound.Participant> players = round.players();
        winners.forEach(id -> {
            if (Achievement.WIN_GAME.category().getChallenge() != challenge) return;
            AchievementRound.Participant state = round.player(id);
            progress(id, state.name(), Achievement.WIN_GAME, 1);
            if (challenge != ChallengeType.FORCE_BATTLE) return;
            if (round.initialChallenges().contains(challenge) && state.fullRound() && !state.usedJoker()) {
                progress(id, state.name(), Achievement.WIN_WITHOUT_JOKERS, 1);
            }
            long lower = players.values().stream().filter(other -> other.value(Achievement.INDIVIDUAL_POINTS) < state.value(Achievement.INDIVIDUAL_POINTS)).count();
            if (teamWinners.contains(id) && players.size() >= 4 && lower < 3) {
                progress(id, state.name(), Achievement.LOW_SCORE_WINNER, 1);
            }
        });
    }

    public void jumpStarted(Player player) {
        jumpStreaks.put(player.getUniqueId(), 0);
        refresh.accept(player.getUniqueId());
    }

    public void jumpLanded(Player player) {
        if (!jumpStreaks.containsKey(player.getUniqueId())) return;
        int count = jumpStreaks.merge(player.getUniqueId(), 1, Integer::sum);
        progress(player.getUniqueId(), player.getName(), Achievement.JUMP_STREAK, count);
        refresh.accept(player.getUniqueId());
    }

    public void jumpEnded(UUID player) {
        jumpStreaks.remove(player);
        refresh.accept(player);
    }

    public void legendaryCase(Player player) {
        progress(player.getUniqueId(), player.getName(), Achievement.LEGENDARY_CASE_ITEM, 1);
    }

    public int current(UUID player, Achievement achievement) {
        if (achievement == Achievement.JUMP_STREAK) return jumpStreaks.getOrDefault(player, 0);
        return round == null || !round.players().containsKey(player) ? 0 : round.player(player).value(achievement);
    }
}
