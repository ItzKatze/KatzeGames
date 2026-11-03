package gg.itzkatze.katze.data;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.EnumSet;
import java.util.Set;

public final class ObtainableObjectives {

    private static final Set<Material> UNOBTAINABLE_ITEMS = EnumSet.of(
            Material.BEDROCK, Material.BARRIER, Material.LIGHT, Material.STRUCTURE_BLOCK, Material.STRUCTURE_VOID,
            Material.JIGSAW, Material.TEST_BLOCK, Material.TEST_INSTANCE_BLOCK, Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK, Material.COMMAND_BLOCK_MINECART,
            Material.DEBUG_STICK, Material.KNOWLEDGE_BOOK, Material.SPAWNER, Material.TRIAL_SPAWNER, Material.VAULT,
            Material.BUDDING_AMETHYST, Material.REINFORCED_DEEPSLATE, Material.END_PORTAL_FRAME,
            Material.CHORUS_PLANT, Material.FARMLAND, Material.DIRT_PATH, Material.FROGSPAWN,
            Material.SUSPICIOUS_SAND, Material.SUSPICIOUS_GRAVEL, Material.PLAYER_HEAD, Material.PETRIFIED_OAK_SLAB);
    private static final Set<EntityType> UNOBTAINABLE_MOBS = EnumSet.of(
            EntityType.PLAYER, EntityType.GIANT, EntityType.ILLUSIONER, EntityType.MANNEQUIN, EntityType.CREAKING,
            EntityType.ARMOR_STAND);

    private ObtainableObjectives() {}

    public static boolean isItem(Material material) {
        return material.isItem() && !material.isAir() && !material.isLegacy()
                && !UNOBTAINABLE_ITEMS.contains(material)
                && !material.name().startsWith("INFESTED_") && !material.name().endsWith("_SPAWN_EGG");
    }

    public static boolean isMob(EntityType type) {
        return type.isAlive() && type.isSpawnable() && !UNOBTAINABLE_MOBS.contains(type);
    }
}
