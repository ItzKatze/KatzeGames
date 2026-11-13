package gg.itzkatze.katze.randomizer;

import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.challenges.ChallengeType;
import net.kyori.adventure.text.Component;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RandomizerGameListenerTest {
    @Test void playerDeathsNeverEnterTheMobDropRandomizer() {
        var previous = new HashSet<>(ChallengeManager.getActiveChallenges());
        Challenge active = (Challenge) Proxy.newProxyInstance(Challenge.class.getClassLoader(), new Class<?>[]{Challenge.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getType" -> ChallengeType.RANDOMIZER;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> throw new AssertionError("Player death reached randomizer logic: " + method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, arguments) -> { throw new AssertionError("Unexpected player access: " + method.getName()); });
        DamageSource source = (DamageSource) Proxy.newProxyInstance(DamageSource.class.getClassLoader(), new Class<?>[]{DamageSource.class},
                (proxy, method, arguments) -> { throw new AssertionError(method.getName()); });
        PlayerDeathEvent death = new PlayerDeathEvent(player, source, List.of(), 7, 0, Component.empty(), true) {
            @Override public List<ItemStack> getDrops() { throw new AssertionError("Player drops must not be processed"); }
        };
        try {
            ChallengeManager.getActiveChallenges().clear();
            ChallengeManager.getActiveChallenges().add(active);

            assertDoesNotThrow(() -> new RandomizerGameListener(List.of()).onMobDeath(death));
            assertEquals(7, death.getDroppedExp());
            assertFalse(death.isCancelled());
        } finally {
            ChallengeManager.getActiveChallenges().clear();
            ChallengeManager.getActiveChallenges().addAll(previous);
        }
    }
}
