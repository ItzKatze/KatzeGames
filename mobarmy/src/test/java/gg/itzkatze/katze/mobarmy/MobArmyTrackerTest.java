package gg.itzkatze.katze.mobarmy;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyTrackerTest {
    @Test void trackerUnlocksOnlyForTheLastFiveIncludingQueuedMobs() {
        var side = new MobArmyMatch.Side();
        assertFalse(MobArmyTracker.available(side));
        for (int i = 0; i < 6; i++) side.queue.add(new MobArmyMatch.CapturedMob());
        assertFalse(MobArmyTracker.available(side));
        side.queue.removeFirst();
        assertTrue(MobArmyTracker.available(side));
        side.live.put(UUID.randomUUID(), new MobArmyMatch.LiveMob("data", 0, 0, 0, 0, 0));
        assertFalse(MobArmyTracker.available(side));
        side.queue.removeFirst();
        assertTrue(MobArmyTracker.available(side));
        side.preparing = true;
        assertFalse(MobArmyTracker.available(side));
        side.preparing = false;
        side.finished = true;
        assertFalse(MobArmyTracker.available(side));
    }

    @Test void nearestTargetIgnoresDeadMobsAndOtherTeamWorlds() {
        World world = world(), other = world();
        LivingEntity near = mob(new Location(world, 3, 84, 0), false);
        LivingEntity far = mob(new Location(world, 10, 80, 0), false);
        LivingEntity dead = mob(new Location(world, 1, 80, 0), true);
        LivingEntity foreign = mob(new Location(other, 0, 80, 0), false);
        assertSame(near, MobArmyTracker.nearest(new Location(world, 0, 80, 0), List.of(far, dead, foreign, near)));
        assertNull(MobArmyTracker.nearest(new Location(world, 0, 80, 0), List.of(dead, foreign)));
    }

    private static World world() {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("equals")) return proxy == arguments[0];
                    throw new AssertionError(method.getName());
                });
    }

    private static LivingEntity mob(Location location, boolean dead) {
        return (LivingEntity) Proxy.newProxyInstance(LivingEntity.class.getClassLoader(), new Class<?>[]{LivingEntity.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getWorld" -> location.getWorld();
                    case "getLocation" -> location;
                    case "isDead" -> dead;
                    default -> throw new AssertionError(method.getName());
                });
    }
}
