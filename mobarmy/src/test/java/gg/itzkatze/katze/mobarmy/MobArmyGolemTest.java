package gg.itzkatze.katze.mobarmy;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyGolemTest {
    @Test void onlyWaveGolemsCanTargetPlayersWhileHelpersCanTargetWaveMobs() {
        var game = new MobArmy().game;
        game.match = new MobArmyMatch();
        game.match.phase = MobArmyMatch.Phase.COMBAT;
        var side = new MobArmyMatch.Side();
        game.match.sides.add(side);
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getKey")) return MobArmyArena.worldKey(0);
                    throw new AssertionError(method.getName());
                });
        IronGolem helper = entity(IronGolem.class, world);
        IronGolem wave = entity(IronGolem.class, world);
        Player player = entity(Player.class, world);
        LivingEntity enemy = entity(LivingEntity.class, world);
        side.members.put(player.getUniqueId(), "Player");
        side.live.put(wave.getUniqueId(), new MobArmyMatch.LiveMob("golem", 0, 0, 0, 0, 0));
        var listener = new MobArmyListener(game);
        assertTrue(target(listener, helper, player).isCancelled());
        assertFalse(target(listener, helper, enemy).isCancelled());
        assertFalse(target(listener, wave, player).isCancelled());
        assertTrue(target(listener, wave, enemy).isCancelled());
        side.roofTicks.put(player.getUniqueId(), 100L);
        assertTrue(target(listener, wave, player).isCancelled());
    }

    private EntityTargetLivingEntityEvent target(MobArmyListener listener, Entity source, LivingEntity target) {
        var event = new EntityTargetLivingEntityEvent(source, target, TargetReason.CLOSEST_ENTITY);
        listener.target(event);
        return event;
    }

    private <T extends Entity> T entity(Class<T> type, World world) {
        UUID id = UUID.randomUUID();
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> id;
            case "getWorld" -> world;
            default -> throw new AssertionError(method.getName());
        }));
    }
}
