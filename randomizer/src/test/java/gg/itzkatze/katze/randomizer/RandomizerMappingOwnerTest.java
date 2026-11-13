package gg.itzkatze.katze.randomizer;

import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.team.KatzeTeam;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RandomizerMappingOwnerTest {

    @Test void teammatesShareMappingsAndOtherTeamsAndSoloPlayersStaySeparate() {
        KatzePlayer alice = new KatzePlayer(UUID.randomUUID(), "Alice");
        KatzePlayer bob = new KatzePlayer(UUID.randomUUID(), "Bob");
        KatzePlayer charlie = new KatzePlayer(UUID.randomUUID(), "Charlie");
        KatzePlayer solo = new KatzePlayer(UUID.randomUUID(), "Solo");
        KatzeTeam first = new KatzeTeam(List.of(alice, bob));
        KatzeTeam second = new KatzeTeam(List.of(charlie));
        try {
            String aliceOwner = owner(alice, true, true);
            assertEquals("team:" + first.getId(), aliceOwner);
            assertEquals(aliceOwner, owner(bob, true, true));
            assertNotEquals(aliceOwner, owner(charlie, true, true));
            assertEquals("player:" + solo.getUuid(), owner(solo, true, true));
            assertNull(owner(alice, true, false));
            assertNotEquals(owner(alice, false, false), owner(bob, false, false));
            assertNull(RandomizerGameListener.mappingOwner(null, false, true));
        } finally {
            KatzeTeam.teams.removeAll(List.of(first, second));
            KatzePlayer.PLAYERS.removeAll(List.of(alice, bob, charlie, solo));
        }
    }

    private String owner(KatzePlayer player, boolean shared, boolean teams) {
        Player bukkit = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return player.getUuid();
                    throw new AssertionError(method.getName());
                });
        return RandomizerGameListener.mappingOwner(bukkit, shared, teams);
    }
}
