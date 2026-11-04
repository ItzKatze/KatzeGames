package gg.itzkatze.katze.achievements;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class AchievementInventory {
    private static final Set<Material> DYES = EnumSet.of(Material.WHITE_DYE, Material.ORANGE_DYE, Material.MAGENTA_DYE,
            Material.LIGHT_BLUE_DYE, Material.YELLOW_DYE, Material.LIME_DYE, Material.PINK_DYE, Material.GRAY_DYE,
            Material.LIGHT_GRAY_DYE, Material.CYAN_DYE, Material.PURPLE_DYE, Material.BLUE_DYE, Material.BROWN_DYE,
            Material.GREEN_DYE, Material.RED_DYE, Material.BLACK_DYE);
    private static final Set<Material> DIAMOND_ARMOR = EnumSet.of(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS);
    private static final Set<Material> AQUARIUM = EnumSet.of(Material.COD_BUCKET, Material.SALMON_BUCKET, Material.PUFFERFISH_BUCKET, Material.TROPICAL_FISH_BUCKET, Material.AXOLOTL_BUCKET);
    private static final List<Set<Material>> WOODS = List.of(
            Set.of(Material.OAK_LOG, Material.STRIPPED_OAK_LOG), Set.of(Material.SPRUCE_LOG, Material.STRIPPED_SPRUCE_LOG),
            Set.of(Material.BIRCH_LOG, Material.STRIPPED_BIRCH_LOG), Set.of(Material.JUNGLE_LOG, Material.STRIPPED_JUNGLE_LOG),
            Set.of(Material.ACACIA_LOG, Material.STRIPPED_ACACIA_LOG), Set.of(Material.DARK_OAK_LOG, Material.STRIPPED_DARK_OAK_LOG),
            Set.of(Material.MANGROVE_LOG, Material.STRIPPED_MANGROVE_LOG), Set.of(Material.CHERRY_LOG, Material.STRIPPED_CHERRY_LOG),
            Set.of(Material.PALE_OAK_LOG, Material.STRIPPED_PALE_OAK_LOG), Set.of(Material.CRIMSON_STEM, Material.STRIPPED_CRIMSON_STEM),
            Set.of(Material.WARPED_STEM, Material.STRIPPED_WARPED_STEM), Set.of(Material.BAMBOO_BLOCK, Material.STRIPPED_BAMBOO_BLOCK));
    private static final Set<Material> VEGAN = EnumSet.of(Material.BREAD, Material.APPLE, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE,
            Material.CARROT, Material.GOLDEN_CARROT, Material.POTATO, Material.BAKED_POTATO, Material.POISONOUS_POTATO,
            Material.BEETROOT, Material.BEETROOT_SOUP, Material.MELON_SLICE, Material.SWEET_BERRIES, Material.GLOW_BERRIES,
            Material.DRIED_KELP, Material.CHORUS_FRUIT, Material.MUSHROOM_STEW, Material.SUSPICIOUS_STEW);

    private AchievementInventory() {}

    public static boolean vegan(Material material) { return VEGAN.contains(material); }

    public static boolean tool(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")
                || name.endsWith("_SWORD") || name.endsWith("_SPEAR") || Set.of(Material.SHEARS, Material.FISHING_ROD, Material.FLINT_AND_STEEL,
                Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.MACE, Material.BRUSH, Material.CARROT_ON_A_STICK, Material.WARPED_FUNGUS_ON_A_STICK).contains(material);
    }

    public static int countDistinct(Set<Material> present, Set<Material> required) {
        return (int) required.stream().filter(present::contains).count();
    }

    public static int woodFamilies(Set<Material> present) {
        return (int) WOODS.stream().filter(family -> family.stream().anyMatch(present::contains)).count();
    }

    public static void inspect(Player player, AchievementService service) {
        if (!service.eligible(player)) return;
        Set<Material> present = EnumSet.noneOf(Material.class);
        int bundles = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.isEmpty()) continue;
            Material type = item.getType();
            present.add(type);
            if (type == Material.BUNDLE || type.name().endsWith("_BUNDLE")) bundles += item.getAmount();
            if (tool(type) && item.containsEnchantment(Enchantment.SILK_TOUCH)) service.record(player, Achievement.SILK_TOUCH_TOOL);
        }
        service.snapshot(player, Achievement.BUNDLES, bundles);
        service.snapshot(player, Achievement.ALL_DYES, countDistinct(present, DYES));
        service.snapshot(player, Achievement.DIAMOND_ARMOR, countDistinct(present, DIAMOND_ARMOR));
        service.snapshot(player, Achievement.AQUARIUM, countDistinct(present, AQUARIUM));
        service.snapshot(player, Achievement.ALL_WOODS, woodFamilies(present));
        if (present.contains(Material.WITHER_SKELETON_SKULL)) service.record(player, Achievement.WITHER_SKULL);
        if (present.contains(Material.TRIDENT)) service.record(player, Achievement.TRIDENT);
        if (Arrays.stream(player.getInventory().getArmorContents()).filter(item -> item != null && !item.isEmpty())
                .anyMatch(item -> item.getItemMeta() instanceof ArmorMeta armor && armor.hasTrim())) service.record(player, Achievement.WEAR_ARMOR_TRIM);
    }
}
