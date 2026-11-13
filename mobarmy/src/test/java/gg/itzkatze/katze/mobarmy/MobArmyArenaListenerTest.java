package gg.itzkatze.katze.mobarmy;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyArenaListenerTest {
    @Test void fatigueProtectionTargetsGuardianAttacksAndAllowsOtherEffectSources() {
        assertTrue(MobArmyArenaListener.guardianEffect(EntityType.ELDER_GUARDIAN, Cause.ATTACK));
        assertTrue(MobArmyArenaListener.guardianEffect(null, Cause.ATTACK));
        assertFalse(MobArmyArenaListener.guardianEffect(EntityType.PLAYER, Cause.POTION_DRINK));
        assertFalse(MobArmyArenaListener.guardianEffect(null, Cause.PLUGIN));
        assertFalse(MobArmyArenaListener.guardianEffect(null, Cause.MILK));
    }

    @Test void daylightDoesNotBurnWaveMobsButBlockFireStillWorks() {
        var challenge = new MobArmy();
        var game = challenge.game;
        game.match = new MobArmyMatch();
        var side = new MobArmyMatch.Side();
        game.match.sides.add(side);
        UUID uuid = UUID.randomUUID();
        LivingEntity mob = (LivingEntity) Proxy.newProxyInstance(LivingEntity.class.getClassLoader(), new Class<?>[]{LivingEntity.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getUniqueId")) return uuid;
                    throw new AssertionError(method.getName());
                });
        side.live.put(uuid, new MobArmyMatch.LiveMob("snapshot", 0, 0, 0, 0, 0));
        var listener = new MobArmyArenaListener(game);
        var sun = new EntityCombustEvent(mob, 8f);
        listener.daylight(sun);
        assertTrue(sun.isCancelled());
        var lava = new EntityCombustByBlockEvent(null, mob, 8f);
        listener.daylight(lava);
        assertFalse(lava.isCancelled());
    }
}
