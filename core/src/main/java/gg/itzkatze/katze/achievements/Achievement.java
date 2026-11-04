package gg.itzkatze.katze.achievements;

import net.kyori.adventure.text.Component;
import gg.itzkatze.katze.text.TextArg;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static gg.itzkatze.katze.achievements.AchievementCategory.*;
import static gg.itzkatze.katze.achievements.AchievementRarity.*;
import static gg.itzkatze.katze.achievements.AchievementScope.*;

public enum Achievement {
    CRAFT_ENDER_EYE(FORCE_BATTLE, Material.ENDER_EYE, EVENT, COMMON, 1),
    ENTER_NETHER(FORCE_BATTLE, Material.OBSIDIAN, EVENT, COMMON, 1),
    WALK_10000(FORCE_BATTLE, Material.LEATHER_BOOTS, ROUND_COUNT, COMMON, 10000),
    TAME_ANIMAL(FORCE_BATTLE, Material.BONE, EVENT, COMMON, 1),
    WEAR_ARMOR_TRIM(FORCE_BATTLE, Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, EVENT, COMMON, 1),
    BED_RESPAWN(FORCE_BATTLE, Material.RED_BED, EVENT, COMMON, 1),
    RETURN_TO_SENDER(FORCE_BATTLE, Material.TIPPED_ARROW, EVENT, COMMON, 1),
    USE_GOAT_HORN(FORCE_BATTLE, Material.GOAT_HORN, EVENT, COMMON, 1),
    BREAK_TOOL(FORCE_BATTLE, Material.IRON_PICKAXE, EVENT, COMMON, 1),
    THREE_STEWS(FORCE_BATTLE, Material.MUSHROOM_STEW, ROUND_DISTINCT, COMMON, 3),
    FIVE_DEATH_CAUSES(FORCE_BATTLE, Material.SKELETON_SKULL, ROUND_DISTINCT, COMMON, 5),
    ALL_DYES(FORCE_BATTLE, Material.RED_DYE, INVENTORY_SNAPSHOT, UNCOMMON, 16),
    SILK_TOUCH_TOOL(FORCE_BATTLE, Material.DIAMOND_PICKAXE, EVENT, UNCOMMON, 1),
    TRIAL_CHAMBER(FORCE_BATTLE, Material.TRIAL_KEY, EVENT, UNCOMMON, 1),
    WITHER_SKULL(FORCE_BATTLE, Material.WITHER_SKELETON_SKULL, EVENT, UNCOMMON, 1),
    START_RAID(FORCE_BATTLE, Material.OMINOUS_BOTTLE, EVENT, UNCOMMON, 1),
    DIAMOND_ARMOR(FORCE_BATTLE, Material.DIAMOND_CHESTPLATE, INVENTORY_SNAPSHOT, UNCOMMON, 4),
    VEGAN_ROUND(FORCE_BATTLE, Material.CARROT, EVENT, UNCOMMON, 1),
    AQUARIUM(FORCE_BATTLE, Material.AXOLOTL_BUCKET, INVENTORY_SNAPSHOT, UNCOMMON, 5),
    MINING_FATIGUE(FORCE_BATTLE, Material.MILK_BUCKET, ROUND_DURATION, UNCOMMON, 300),
    WIN_GAME(FORCE_BATTLE, Material.GOLDEN_HELMET, EVENT, UNCOMMON, 1),
    MUSHROOM_FIELDS(FORCE_BATTLE, Material.RED_MUSHROOM, EVENT, RARE, 1),
    STRONGHOLD(FORCE_BATTLE, Material.END_PORTAL_FRAME, EVENT, RARE, 1),
    LEGENDARY_CASE_ITEM(LOBBY, Material.ENDER_CHEST, EVENT, RARE, 1),
    ALL_WOODS(FORCE_BATTLE, Material.OAK_LOG, INVENTORY_SNAPSHOT, RARE, 12),
    ENCHANTED_GOLDEN_APPLE(FORCE_BATTLE, Material.ENCHANTED_GOLDEN_APPLE, EVENT, RARE, 1),
    ALREADY_HAVE_OBJECTIVE(FORCE_BATTLE, Material.CHEST, EVENT, RARE, 1),
    NETHER_STRUCTURES(FORCE_BATTLE, Material.NETHER_BRICKS, ROUND_DISTINCT, RARE, 3),
    LOW_SCORE_WINNER(FORCE_BATTLE, Material.WOODEN_SWORD, EVENT, RARE, 1),
    WIN_WITHOUT_JOKERS(FORCE_BATTLE, Material.NETHER_STAR, EVENT, MYTHICAL, 1),
    TRIDENT(FORCE_BATTLE, Material.TRIDENT, EVENT, MYTHICAL, 1),
    DEEPSLATE_COAL(FORCE_BATTLE, Material.DEEPSLATE_COAL_ORE, EVENT, MYTHICAL, 1),
    LIGHTNING_ROD(FORCE_BATTLE, Material.LIGHTNING_ROD, EVENT, MYTHICAL, 1),
    ROUND_DEATHS(FORCE_BATTLE, Material.TOTEM_OF_UNDYING, ROUND_COUNT, 4, 10, 15, 20, 25),
    JUMP_STREAK(LOBBY, Material.FEATHER, ATTEMPT_STREAK, 100, 200, 300, 400, 500),
    BUNDLES(FORCE_BATTLE, Material.BUNDLE, INVENTORY_SNAPSHOT, 3, 5, 10, 15, 20),
    BIOMES(FORCE_BATTLE, Material.COMPASS, ROUND_DISTINCT, 10, 18, 25, 30, 35),
    INDIVIDUAL_POINTS(FORCE_BATTLE, Material.GOLD_NUGGET, ROUND_COUNT, 20, 30, 40, 55, 75),
    BOAT_DISTANCE(FORCE_BATTLE, Material.OAK_BOAT, ROUND_COUNT, 2000, 4000, 8000, 12000, 16000),
    TEAM_OBJECTIVES(FORCE_BATTLE, Material.DIAMOND, ROUND_COUNT, 3, 5, 8, 10, 12),
    ADVANCEMENTS(FORCE_BATTLE, Material.KNOWLEDGE_BOOK, ROUND_DISTINCT, 10, 20, 30, 40, 45),
    MOB_TYPES(FORCE_BATTLE, Material.ROTTEN_FLESH, ROUND_DISTINCT, 5, 10, 15, 25, 35);

    private final AchievementCategory category;
    private final Material icon;
    private final AchievementScope scope;
    private final List<AchievementStage> stages;

    Achievement(AchievementCategory category, Material icon, AchievementScope scope, AchievementRarity rarity, int target) {
        this(category, icon, scope, List.of(new AchievementStage("1", target, rarity)));
    }

    Achievement(AchievementCategory category, Material icon, AchievementScope scope, int... targets) {
        this(category, icon, scope, staged(targets));
    }

    Achievement(AchievementCategory category, Material icon, AchievementScope scope, List<AchievementStage> stages) {
        this.category = category;
        this.icon = icon;
        this.scope = scope;
        this.stages = List.copyOf(stages);
        int previous = 0;
        for (AchievementStage stage : stages) {
            if (stage.target() <= previous) throw new IllegalArgumentException("Stage targets must increase");
            previous = stage.target();
        }
    }

    private static List<AchievementStage> staged(int[] targets) {
        if (targets.length != 5) throw new IllegalArgumentException("Expected five stages");
        List<AchievementStage> stages = new ArrayList<>();
        for (int i = 0; i < targets.length; i++) stages.add(new AchievementStage(Integer.toString(i + 1), targets[i], COMMON));
        return stages;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("katze.achievements." + id() + ".name");
    }

    public Component description() {
        return Component.translatable("katze.achievements." + id() + ".description");
    }

    public Component description(AchievementStage stage) {
        return stages.size() == 1 ? description() : Component.translatable("katze.achievements." + id() + ".stage_description",
                TextArg.number("target", stage.target()));
    }

    public AchievementCategory category() {
        return category;
    }

    public Material icon() {
        return icon;
    }

    public AchievementScope scope() {
        return scope;
    }

    public List<AchievementStage> stages() {
        return stages;
    }

    public int maximum() {
        return stages.getLast().target();
    }

    public static List<Achievement> in(AchievementCategory category) {
        return Arrays.stream(values()).filter(achievement -> achievement.category == category)
                .sorted(Comparator.comparing(achievement -> achievement.stages.getFirst().rarity())).toList();
    }
}
