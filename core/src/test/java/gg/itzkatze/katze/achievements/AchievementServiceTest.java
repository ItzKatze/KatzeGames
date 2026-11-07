package gg.itzkatze.katze.achievements;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.level.LevelStore;
import gg.itzkatze.katze.player.data.PlayerDataStore;
import gg.itzkatze.katze.text.TextStyle;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import org.bukkit.Material;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

final class AchievementServiceTest {
    @TempDir Path directory;
    private final UUID player = UUID.randomUUID();
    private final List<AchievementService.Unlock> notices = new ArrayList<>();
    private Path file;
    private PlayerDataStore data;
    private LevelStore levels;
    private AchievementService service;

    @BeforeEach
    void setUp() {
        file = directory.resolve("players.json");
        data = new PlayerDataStore(file, (task, ticks) -> {});
        levels = new LevelStore(data);
        service = service(data);
    }

    private AchievementService service(PlayerDataStore store) {
        return new AchievementService(store, (id, unlock) -> notices.add(unlock), id -> {});
    }

    @Test
    void catalogueHasExactlyThePdfEntriesAndConfirmedRewards() {
        assertEquals(42, Achievement.values().length);
        assertEquals(42, Arrays.stream(Achievement.values()).map(Achievement::id).distinct().count());
        assertEquals(78, Arrays.stream(Achievement.values()).mapToInt(achievement -> achievement.stages().size()).sum());
        assertEquals(3700, Arrays.stream(Achievement.values()).flatMap(achievement -> achievement.stages().stream()).mapToInt(stage -> stage.rarity().xp()).sum());
        assertEquals(List.of(4, 10, 15, 20, 25), Achievement.ROUND_DEATHS.stages().stream().map(AchievementStage::target).toList());
        assertEquals(8, AchievementCategory.values().length);
    }

    @Test
    void singleAchievementPaysOnceEvenAfterReload() {
        service.progress(player, "Katze", Achievement.ENTER_NETHER, 1);
        service.progress(player, "Katze", Achievement.ENTER_NETHER, 1);
        assertEquals(25, service.profile(player).xp());
        assertEquals(1, notices.size());
        PlayerDataStore loaded = new PlayerDataStore(file, (task, ticks) -> {});
        LevelStore loadedLevels = new LevelStore(loaded);
        service(loaded).progress(player, "NewName", Achievement.ENTER_NETHER, 1);
        assertEquals(25, loaded.find(player).achievements().xp());
        assertEquals(0, loadedLevels.xp(player));
        assertEquals(1, notices.size());
        assertTrue(loaded.find(player).achievements().completed(Achievement.ENTER_NETHER, Achievement.ENTER_NETHER.stages().getFirst()));
    }

    @Test
    void aSnapshotCrossingAllStagesGrantsThemTogetherOnce() {
        service.progress(player, "Katze", Achievement.BUNDLES, 20);
        service.progress(player, "Katze", Achievement.BUNDLES, 100);
        assertEquals(125, service.profile(player).xp());
        assertEquals(5, service.profile(player).completedStages(Achievement.BUNDLES));
        assertEquals(1, notices.size());
        assertEquals(5, notices.getFirst().stages().size());
        assertEquals(125, notices.getFirst().xp());
    }

    @Test
    void laterThresholdsPayOnlyNewStages() {
        service.progress(player, "Katze", Achievement.BUNDLES, 5);
        assertEquals(50, service.profile(player).xp());
        service.progress(player, "Katze", Achievement.BUNDLES, 3);
        assertEquals(50, service.profile(player).xp());
        service.progress(player, "Katze", Achievement.BUNDLES, 10);
        assertEquals(75, service.profile(player).xp());
        assertEquals(2, notices.size());
        assertEquals(10, service.profile(player).best(Achievement.BUNDLES));
    }

    @Test
    void failedSavePublishesNoXpOrUnlockAndRetryWorks() throws Exception {
        Files.createDirectory(file);
        List<Integer> changedLevels = new ArrayList<>();
        levels.addLevelUpListener((id, before, after) -> changedLevels.add(after));
        service.progress(player, "Katze", Achievement.BUNDLES, 20);
        assertEquals(0, service.profile(player).xp());
        assertEquals(1, levels.level(player));
        assertTrue(levels.drainRecentGains().isEmpty());
        assertTrue(changedLevels.isEmpty());
        assertTrue(notices.isEmpty());
        assertEquals(0, service.profile(player).completedStages(Achievement.BUNDLES));
        assertEquals(20, service.profile(player).pending().get("bundles"));
        Files.delete(file);
        service.retryPending();
        assertEquals(125, service.profile(player).xp());
        assertTrue(levels.drainRecentGains().isEmpty());
        assertEquals(1, notices.size());
        assertTrue(service.profile(player).pending().isEmpty());
        service.retryPending();
        assertEquals(125, service.profile(player).xp());
    }

    @Test
    void pendingRewardSurvivesProfileReloadAndRoundReset() {
        data.profile(player, "Katze").achievements().queue(Achievement.ROUND_DEATHS, 10);
        data.markDirty();
        assertTrue(data.flush());
        PlayerDataStore loaded = new PlayerDataStore(file, (task, ticks) -> {});
        LevelStore loadedLevels = new LevelStore(loaded);
        AchievementService restored = service(loaded);
        restored.reset();
        assertEquals(50, loaded.find(player).achievements().xp());
        assertEquals(0, loadedLevels.xp(player));
        assertEquals(2, restored.profile(player).completedStages(Achievement.ROUND_DEATHS));
    }

    @Test
    void xpIsDerivedOnlyFromKnownCompletedStagesOnEveryRead() {
        AchievementProfile profile = new Gson().fromJson("""
                {"xp":2147483647,"best":{"round_deaths":25},"pending":{"bundles":20},
                "completed":{"unknown/1":1,"trident/99":1,"trident/1":0}}
                """, AchievementProfile.class);
        assertEquals(0, profile.xp());
        profile.complete(Achievement.ENTER_NETHER, Achievement.ENTER_NETHER.stages().getFirst(), 123);
        assertEquals(25, profile.xp());
        profile.complete(Achievement.BUNDLES, Achievement.BUNDLES.stages().getFirst(), 123);
        assertEquals(50, profile.xp());
        profile.complete(Achievement.BUNDLES, Achievement.BUNDLES.stages().getFirst(), 124);
        assertEquals(50, profile.xp());
    }

    @Test
    void deathsFromDifferentRoundsDoNotCombineButUnlocksRemain() {
        service.begin(Set.of(ChallengeType.FORCE_BATTLE), Map.of(player, "Katze"));
        assertEquals(3, service.round().player(player).add(Achievement.ROUND_DEATHS, 3));
        service.progress(player, "Katze", Achievement.ROUND_DEATHS, service.current(player, Achievement.ROUND_DEATHS));
        service.reset();
        service.begin(Set.of(ChallengeType.FORCE_BATTLE), Map.of(player, "Katze"));
        service.round().player(player).add(Achievement.ROUND_DEATHS, 1);
        service.progress(player, "Katze", Achievement.ROUND_DEATHS, service.current(player, Achievement.ROUND_DEATHS));
        assertEquals(0, service.profile(player).xp());
        service.round().player(player).add(Achievement.ROUND_DEATHS, 3);
        service.progress(player, "Katze", Achievement.ROUND_DEATHS, service.current(player, Achievement.ROUND_DEATHS));
        assertEquals(25, service.profile(player).xp());
        service.reset();
        assertEquals(0, service.current(player, Achievement.ROUND_DEATHS));
        assertEquals(1, service.profile(player).completedStages(Achievement.ROUND_DEATHS));
    }

    @Test
    void distinctCountersDeduplicateAndRoundDataKeepsDisqualifiers() {
        service.begin(Set.of(ChallengeType.FORCE_BATTLE), Map.of(player, "Katze"));
        AchievementRound.Participant participant = service.round().player(player);
        participant.distinct(Achievement.BIOMES, "minecraft:plains");
        participant.distinct(Achievement.BIOMES, "minecraft:plains");
        participant.food(true);
        participant.food(false);
        participant.useJoker();
        Gson gson = new Gson();
        AchievementRound restored = gson.fromJson(gson.toJson(service.round()), AchievementRound.class);
        assertTrue(restored.normalize());
        assertEquals(service.roundId(), restored.id());
        assertEquals(1, restored.player(player).value(Achievement.BIOMES));
        assertFalse(restored.player(player).vegan());
        assertTrue(restored.player(player).usedJoker());
    }

    @Test
    void naturalEndPaysOnceAndLaterChallengeResultsStillWork() {
        service.begin(Set.of(ChallengeType.FORCE_BATTLE, ChallengeType.DEATH_SWAP), Map.of(player, "Katze"));
        service.round().player(player).food(true);
        service.naturalEnd(ChallengeType.DEATH_SWAP, Set.of(player), Set.of());
        assertEquals(0, service.profile(player).xp());
        service.naturalEnd(ChallengeType.FORCE_BATTLE, Set.of(player), Set.of());
        assertEquals(350, service.profile(player).xp());
        service.naturalEnd(ChallengeType.FORCE_BATTLE, Set.of(player), Set.of());
        assertEquals(350, service.profile(player).xp());
    }

    @Test
    void bingoDoesNotTrackOrAwardForceBattleAchievements() {
        service.begin(Set.of(ChallengeType.BINGO), Map.of(player, "Katze"));
        service.round().player(player).food(true);
        for (Achievement achievement : Achievement.in(AchievementCategory.FORCE_BATTLE)) {
            assertFalse(service.tracks(achievement), achievement.id());
            service.record(null, achievement);
            service.snapshot(null, achievement, achievement.maximum());
            service.add(null, achievement, achievement.maximum());
            service.distinct(null, achievement, "bingo");
            assertEquals(0, service.current(player, achievement));
        }
        service.naturalEnd(ChallengeType.FORCE_BATTLE, Set.of(player), Set.of(player));
        assertFalse(service.round().finished());
        service.naturalEnd(ChallengeType.BINGO, Set.of(player), Set.of(player));
        assertTrue(service.round().finished());
        assertTrue(notices.isEmpty());
        assertEquals(0, service.profile(player).xp());
    }

    @Test
    void forceBattleTrackingStopsWhenItsRoundEnds() {
        service.begin(Set.of(ChallengeType.FORCE_BATTLE), Map.of(player, "Katze"));
        for (Achievement achievement : Achievement.in(AchievementCategory.FORCE_BATTLE)) assertTrue(service.tracks(achievement));
        service.naturalEnd(ChallengeType.FORCE_BATTLE, Set.of(), Set.of());
        for (Achievement achievement : Achievement.in(AchievementCategory.FORCE_BATTLE)) assertFalse(service.tracks(achievement));
    }

    @Test
    void lateJoinersAndJokerUsersCannotClaimACleanRound() {
        UUID late = UUID.randomUUID();
        service.begin(Set.of(ChallengeType.FORCE_BATTLE), Map.of(player, "Katze"));
        service.round().player(player).useJoker();
        service.round().player(late).food(true);
        service.naturalEnd(ChallengeType.FORCE_BATTLE, Set.of(player, late), Set.of());
        assertEquals(50, service.profile(player).xp());
        assertEquals(50, service.profile(late).xp());
    }

    @Test
    void bottomThreeUsesPersonalPointsAndNeedsFourPlayers() {
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        UUID fourth = UUID.randomUUID();
        service.begin(Set.of(ChallengeType.FORCE_BATTLE), Map.of(player, "Katze", second, "Two", third, "Three", fourth, "Four"));
        service.round().player(player).useJoker();
        service.round().player(player).set(Achievement.INDIVIDUAL_POINTS, 2);
        service.round().player(second).set(Achievement.INDIVIDUAL_POINTS, 1);
        service.round().player(third).set(Achievement.INDIVIDUAL_POINTS, 2);
        service.round().player(fourth).set(Achievement.INDIVIDUAL_POINTS, 9);
        service.naturalEnd(ChallengeType.FORCE_BATTLE, Set.of(player), Set.of(player));
        assertEquals(150, service.profile(player).xp());
        assertEquals(1, service.profile(player).completedStages(Achievement.LOW_SCORE_WINNER));
    }

    @Test
    void oldProfileKeepsXpAndUnknownAchievementHistory() throws Exception {
        Files.writeString(file, """
                {"version":1,"players":{"%s":{"name":"Katze","xp":123,"achievements":null}}}
                """.formatted(player));
        PlayerDataStore loaded = new PlayerDataStore(file, (task, ticks) -> {});
        assertEquals(123, loaded.find(player).xp());
        assertNotNull(loaded.find(player).cosmetics());
        assertNotNull(loaded.find(player).achievements());
        String json = "{\"best\":{\"future_achievement\":8},\"completed\":{\"future_achievement/1\":123}}";
        Gson gson = new Gson();
        AchievementProfile profile = gson.fromJson(json, AchievementProfile.class);
        profile.normalize();
        assertTrue(gson.toJson(profile).contains("future_achievement/1"));
    }

    @Test
    void woodFamiliesDoNotDoubleCountVariantsAndPlanksDoNotCount() {
        assertEquals(1, AchievementInventory.woodFamilies(Set.of(Material.OAK_LOG, Material.STRIPPED_OAK_LOG, Material.BIRCH_PLANKS)));
        assertEquals(2, AchievementInventory.woodFamilies(Set.of(Material.OAK_LOG, Material.BAMBOO_BLOCK)));
        assertTrue(AchievementInventory.vegan(Material.MUSHROOM_STEW));
        assertFalse(AchievementInventory.vegan(Material.RABBIT_STEW));
        assertFalse(AchievementInventory.vegan(Material.HONEY_BOTTLE));
    }

    @Test
    void orphanedOrMismatchedRoundCannotRestoreProgress() {
        service.begin(Set.of(ChallengeType.FORCE_BATTLE), Map.of(player, "Katze"));
        AchievementRound saved = service.round();
        UUID expected = service.roundId();
        saved.player(player).set(Achievement.ROUND_DEATHS, 3);
        service.restore(saved, UUID.randomUUID());
        assertNull(service.round());
        service.restore(saved, null);
        assertNull(service.round());
        service.restore(saved, expected);
        assertEquals(3, service.current(player, Achievement.ROUND_DEATHS));
    }

    @Test
    void fewerThanFourPlayersCannotEarnBottomThreeReward() {
        UUID second = UUID.randomUUID();
        service.begin(Set.of(ChallengeType.FORCE_BATTLE), Map.of(player, "Katze", second, "Other"));
        service.round().player(player).useJoker();
        service.naturalEnd(ChallengeType.FORCE_BATTLE, Set.of(player), Set.of(player));
        assertEquals(50, service.profile(player).xp());
        assertEquals(0, service.profile(player).completedStages(Achievement.LOW_SCORE_WINNER));
    }

    @Test
    void noWinnerDoesNotCreateAWinAndResetDoesNotAwardVeganRound() {
        service.begin(Set.of(ChallengeType.FORCE_BATTLE), Map.of(player, "Katze"));
        service.round().player(player).food(true);
        service.reset();
        assertEquals(0, service.profile(player).xp());
        service.begin(Set.of(ChallengeType.FORCE_BATTLE), Map.of(player, "Katze"));
        service.naturalEnd(ChallengeType.FORCE_BATTLE, Set.of(), Set.of());
        assertEquals(0, service.profile(player).xp());
    }

    @Test
    void miningFatigueSurvivalResetsWithoutRevokingSavedBest() {
        service.begin(Set.of(ChallengeType.FORCE_BATTLE), Map.of(player, "Katze"));
        AchievementRound.Participant state = service.round().player(player);
        state.add(Achievement.MINING_FATIGUE, 250);
        service.progress(player, "Katze", Achievement.MINING_FATIGUE, state.value(Achievement.MINING_FATIGUE));
        state.set(Achievement.MINING_FATIGUE, 0);
        state.add(Achievement.MINING_FATIGUE, 50);
        service.progress(player, "Katze", Achievement.MINING_FATIGUE, state.value(Achievement.MINING_FATIGUE));
        assertEquals(250, service.profile(player).best(Achievement.MINING_FATIGUE));
        assertEquals(0, service.profile(player).xp());
    }

    @Test
    void exceptionInAtomicAwardRollsBackProfileWithoutPublishingChanges() {
        levels.adjust(player, "Katze", 10);
        assertThrows(IllegalStateException.class, () -> data.transaction(player, () -> {
            data.profile(player, "Katze").achievements().complete(Achievement.TRIDENT, Achievement.TRIDENT.stages().getFirst(), 123);
            assertEquals(250, data.profile(player, "Katze").achievements().xp());
            throw new IllegalStateException("Failed update");
        }));
        assertEquals(0, service.profile(player).xp());
        assertEquals(10, levels.xp(player));
        assertEquals(0, service.profile(player).completedStages(Achievement.TRIDENT));
        assertTrue(levels.drainRecentGains().isEmpty());
    }

    @Test
    void customNamesAreOptInPerPlayerAndIndependentOfLocale() {
        assertFalse(service.usesCustomNames(player));
        assertInstanceOf(TranslatableComponent.class, service.title(player, Achievement.TAME_ANIMAL));
        assertTrue(service.setCustomNames(player, "Katze", true));
        for (Locale locale : List.of(Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH)) {
            assertEquals("Ich habe einen neuen Teammate!", PlainTextComponentSerializer.plainText()
                    .serialize(GlobalTranslator.render(service.title(player, Achievement.TAME_ANIMAL), locale)));
        }
        UUID other = UUID.randomUUID();
        assertFalse(service.usesCustomNames(other));
        assertInstanceOf(TranslatableComponent.class, service.title(other, Achievement.TAME_ANIMAL));
        assertTrue(service.setCustomNames(player, "Katze", false));
        assertInstanceOf(TranslatableComponent.class, service.title(player, Achievement.TAME_ANIMAL));
    }

    @Test
    void customNamePreferencePersistsWithoutChangingRewards() {
        levels.adjust(player, "Katze", 123);
        service.progress(player, "Katze", Achievement.ENTER_NETHER, 1);
        assertTrue(service.setCustomNames(player, "Katze", true));
        PlayerDataStore loaded = new PlayerDataStore(file, (task, ticks) -> {});
        LevelStore loadedLevels = new LevelStore(loaded);
        AchievementService restored = service(loaded);
        assertTrue(restored.usesCustomNames(player));
        assertEquals(25, loaded.find(player).achievements().xp());
        assertEquals(123, loadedLevels.xp(player));
        assertEquals(1, restored.profile(player).completedStages(Achievement.ENTER_NETHER));
        assertTrue(restored.setCustomNames(player, "Katze", false));
        PlayerDataStore disabled = new PlayerDataStore(file, (task, ticks) -> {});
        assertFalse(disabled.find(player).customAchievementNames());
    }

    @Test
    void failedPreferenceSaveLeavesLocalizedNamesEnabled() throws Exception {
        Files.createDirectory(file);
        assertFalse(service.setCustomNames(player, "Katze", true));
        assertFalse(service.usesCustomNames(player));
        assertInstanceOf(TranslatableComponent.class, service.title(player, Achievement.TAME_ANIMAL));
    }

    @Test
    void customNamePackContainsEveryOriginalName() {
        AchievementNamePack pack = AchievementNamePack.custom();
        for (Achievement achievement : Achievement.values()) {
            assertFalse(assertInstanceOf(TextComponent.class, pack.title(achievement)).content().isBlank());
        }
        assertEquals("Sortierung à la Nied", assertInstanceOf(TextComponent.class, pack.title(Achievement.BUNDLES)).content());
        assertEquals("I have become Death. The Destroyer of Villages.", assertInstanceOf(TextComponent.class, pack.title(Achievement.START_RAID)).content());
        assertEquals("Ein Königreich für eine Kohle", assertInstanceOf(TextComponent.class, pack.title(Achievement.DEEPSLATE_COAL)).content());
    }

    @Test
    void achievementAndRegularLevelsNeverCreditEachOther() {
        levels.adjust(player, "Katze", 1000);
        int regularLevel = levels.level(player);
        List<UUID> regularChanges = new ArrayList<>();
        levels.addChangeListener(regularChanges::add);
        service.progress(player, "Katze", Achievement.TRIDENT, 1);
        assertEquals(250, service.profile(player).xp());
        assertEquals(new AchievementLevel(3, 0, 200), service.profile(player).level());
        assertEquals(1000, levels.xp(player));
        assertEquals(regularLevel, levels.level(player));
        assertTrue(regularChanges.isEmpty());
        assertTrue(levels.drainRecentGains().isEmpty());
        assertEquals(1, notices.getFirst().oldLevel());
        assertEquals(3, notices.getFirst().newLevel());
        levels.grant(player, "Katze", 50);
        assertEquals(1050, levels.xp(player));
        assertEquals(250, service.profile(player).xp());
        assertEquals(50, levels.drainRecentGains().get(player));
    }

    @Test
    void achievementLevelCostsStartAtOneHundredAndIncreaseByFifty() {
        assertEquals(new AchievementLevel(1, 0, 100), AchievementLevel.forXp(-1));
        assertEquals(new AchievementLevel(1, 0, 100), AchievementLevel.forXp(0));
        assertEquals(new AchievementLevel(1, 99, 100), AchievementLevel.forXp(99));
        assertEquals(new AchievementLevel(2, 0, 150), AchievementLevel.forXp(100));
        assertEquals(new AchievementLevel(2, 149, 150), AchievementLevel.forXp(249));
        assertEquals(new AchievementLevel(3, 0, 200), AchievementLevel.forXp(250));
        assertEquals(new AchievementLevel(3, 199, 200), AchievementLevel.forXp(449));
        assertEquals(new AchievementLevel(4, 0, 250), AchievementLevel.forXp(450));
        assertEquals(new AchievementLevel(4, 75, 250), AchievementLevel.forXp(525));
        assertEquals(new AchievementLevel(11, 450, 600), AchievementLevel.forXp(3700));
        assertEquals(new AchievementLevel(16, 550, 850), AchievementLevel.forXp(7300));
        assertEquals(new AchievementLevel(9267, 319797, 463400), AchievementLevel.forXp(Integer.MAX_VALUE));
    }

    @Test
    void legacyUnlocksDetermineAchievementXpWithoutChangingRegularXp() throws Exception {
        Files.writeString(file, """
                {"version":2,"players":{"%s":{"name":"Katze","xp":1500,"pdfAchievementNames":true,
                "achievements":{"completed":{"bundles/1":1,"bundles/2":2,"enter_nether/1":3,"unknown/1":4},
                "best":{"round_deaths":25}}}}}
                """.formatted(player));
        PlayerDataStore loaded = new PlayerDataStore(file, (task, ticks) -> {});
        assertEquals(75, loaded.find(player).achievements().xp());
        assertEquals(1, loaded.find(player).achievements().level().level());
        assertEquals(1500, loaded.find(player).xp());
        assertTrue(loaded.find(player).customAchievementNames());
        assertTrue(loaded.flush());
        PlayerDataStore reloaded = new PlayerDataStore(file, (task, ticks) -> {});
        AchievementService restored = service(reloaded);
        restored.progress(player, "Katze", Achievement.BUNDLES, 5);
        assertEquals(75, restored.profile(player).xp());
        restored.progress(player, "Katze", Achievement.TRIDENT, 1);
        assertEquals(325, restored.profile(player).xp());
        assertEquals(1500, reloaded.find(player).xp());
        assertEquals(1, notices.size());
    }

    @Test
    void allAchievementsPayOnlyTheSeparateRewardPool() {
        for (Achievement achievement : Achievement.values()) service.progress(player, "Katze", achievement, achievement.maximum());
        assertEquals(3700, service.profile(player).xp());
        assertEquals(11, service.profile(player).level().level());
        assertEquals(0, levels.xp(player));
        assertTrue(levels.drainRecentGains().isEmpty());
        PlayerDataStore loaded = new PlayerDataStore(file, (task, ticks) -> {});
        AchievementService restored = service(loaded);
        for (Achievement achievement : Achievement.values()) restored.progress(player, "Katze", achievement, achievement.maximum());
        assertEquals(3700, restored.profile(player).xp());
        assertEquals(42, notices.size());
    }

    @Test
    void everyStagePaysTheSameBaseReward() {
        for (AchievementStage stage : Achievement.BUNDLES.stages()) {
            service.progress(player, "Katze", Achievement.BUNDLES, stage.target());
            assertEquals(25, notices.getLast().xp());
            assertEquals(AchievementRarity.COMMON, notices.getLast().stages().getFirst().rarity());
        }
        assertEquals(5, notices.size());
        assertEquals(125, service.profile(player).xp());
    }

    @Test
    void outdatedSavedXpIsReplacedByCurrentCompletedStageRewards() throws Exception {
        Files.writeString(file, """
                {"version":3,"players":{"%s":{"name":"Katze","xp":1234,
                "achievements":{"xp":525,"completed":{"bundles/1":1,"bundles/2":2,"bundles/3":3,"bundles/4":4,"bundles/5":5}}}}}
                """.formatted(player));
        PlayerDataStore loaded = new PlayerDataStore(file, (task, ticks) -> {});
        AchievementService restored = service(loaded);
        assertEquals(new AchievementLevel(2, 25, 150), restored.profile(player).level());
        assertTrue(loaded.flush());
        JsonObject saved = new Gson().fromJson(Files.readString(file), JsonObject.class)
                .getAsJsonObject("players").getAsJsonObject(player.toString());
        assertFalse(saved.getAsJsonObject("achievements").has("xp"));
        assertEquals(1234, saved.get("xp").getAsInt());
        restored.progress(player, "Katze", Achievement.BUNDLES, 20);
        assertTrue(notices.isEmpty());
        assertEquals(125, restored.profile(player).xp());
        restored.progress(player, "Katze", Achievement.ROUND_DEATHS, 4);
        assertEquals(150, restored.profile(player).xp());
        assertEquals(25, notices.getFirst().xp());
        assertEquals(1234, loaded.find(player).xp());
        PlayerDataStore reloaded = new PlayerDataStore(file, (task, ticks) -> {});
        assertEquals(150, reloaded.find(player).achievements().xp());
        assertFalse(new Gson().toJsonTree(reloaded.find(player).achievements()).getAsJsonObject().has("xp"));
    }

    @Test
    void unlockHoverShowsBothLocalizedLinesForNormalAndCustomNames() throws Exception {
        MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("katzegames", "hover-test"), TextStyle.miniMessage());
        store.defaultLocale(Locale.ENGLISH);
        for (String language : List.of("en", "de")) {
            Properties properties = new Properties();
            try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/lang/achievements/" + language + ".properties"), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            store.registerAll(Locale.forLanguageTag(language), properties.stringPropertyNames().stream().collect(Collectors.toMap(key -> key, properties::getProperty)));
        }
        GlobalTranslator.translator().addSource(store);
        try {
            for (boolean custom : List.of(false, true)) {
                assertTrue(service.setCustomNames(player, "Katze", custom));
                for (Locale locale : List.of(Locale.ENGLISH, Locale.GERMAN)) {
                    for (Achievement achievement : List.of(Achievement.ENTER_NETHER, Achievement.BUNDLES)) {
                        AchievementStage stage = achievement.stages().get(achievement.stages().size() > 1 ? 1 : 0);
                        Component message = service.unlockMessage(player, new AchievementService.Unlock(achievement,
                                List.of(stage), 25, 1, 1), locale);
                        Component hover = findHover(message);
                        assertNotNull(hover);
                        String requirement = achievement == Achievement.ENTER_NETHER
                                ? locale.equals(Locale.GERMAN) ? "Betrete den Nether." : "Enter the Nether."
                                : locale.equals(Locale.GERMAN) ? "Trage 5 Bündel gleichzeitig." : "Carry 5 bundles at the same time.";
                        String reward = locale.equals(Locale.GERMAN) ? "Erhaltene Errungenschafts-XP: +25 XP" : "Achievement XP awarded: +25 XP";
                        assertEquals(requirement + "\n" + reward, PlainTextComponentSerializer.plainText().serialize(hover));
                        assertEquals(TextDecoration.State.FALSE, hover.decoration(TextDecoration.OBFUSCATED));
                    }
                }
            }
        } finally {
            GlobalTranslator.translator().removeSource(store);
        }
    }

    private Component findHover(Component component) {
        HoverEvent<?> event = component.hoverEvent();
        if (event != null && event.action() == HoverEvent.Action.SHOW_TEXT) return (Component) event.value();
        for (Component child : component.children()) {
            Component hover = findHover(child);
            if (hover != null) return hover;
        }
        return null;
    }
}
