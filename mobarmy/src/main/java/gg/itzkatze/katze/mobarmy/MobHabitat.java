package gg.itzkatze.katze.mobarmy;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Set;

final class MobHabitat {

    private static final Set<EntityType> DEPENDENT = Set.of(
            EntityType.AXOLOTL, EntityType.COD, EntityType.DOLPHIN,
            EntityType.ELDER_GUARDIAN, EntityType.GLOW_SQUID, EntityType.GUARDIAN, EntityType.NAUTILUS,
            EntityType.ZOMBIE_NAUTILUS, EntityType.PUFFERFISH, EntityType.SALMON, EntityType.SQUID,
            EntityType.TADPOLE, EntityType.TROPICAL_FISH, EntityType.STRIDER, EntityType.BLAZE, EntityType.MAGMA_CUBE
    );

    private MobHabitat() {}

    static boolean preventsDamage(EntityType type, DamageCause cause) {
        return DEPENDENT.contains(type) && (cause == DamageCause.DRYOUT || cause == DamageCause.DROWNING);
    }

    static void maintain(LivingEntity entity) {
        if (DEPENDENT.contains(entity.getType())) entity.setRemainingAir(entity.getMaximumAir());
    }

}
