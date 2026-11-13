package gg.itzkatze.katze.mobarmy;

import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MobHabitatTest {
    @Test void waterAndLavaMobsSurviveMissingTheirHabitat() {
        for (EntityType type : List.of(EntityType.AXOLOTL, EntityType.COD, EntityType.DOLPHIN, EntityType.GLOW_SQUID,
                EntityType.SQUID, EntityType.TADPOLE, EntityType.NAUTILUS, EntityType.ZOMBIE_NAUTILUS,
                EntityType.STRIDER, EntityType.BLAZE, EntityType.MAGMA_CUBE)) {
            assertTrue(MobHabitat.preventsDamage(type, DamageCause.DRYOUT), type.name());
            assertTrue(MobHabitat.preventsDamage(type, DamageCause.DROWNING), type.name());
            assertFalse(MobHabitat.preventsDamage(type, DamageCause.ENTITY_ATTACK), type.name());
            assertFalse(MobHabitat.preventsDamage(type, DamageCause.PROJECTILE), type.name());
        }
    }

    @Test void habitatProtectionDoesNotMakeOtherMobsOrPlayersImmuneToDrowning() {
        assertFalse(MobHabitat.preventsDamage(EntityType.ZOMBIE, DamageCause.DROWNING));
        assertFalse(MobHabitat.preventsDamage(EntityType.PLAYER, DamageCause.DROWNING));
        assertFalse(MobHabitat.preventsDamage(EntityType.COD, DamageCause.LAVA));
    }
}
