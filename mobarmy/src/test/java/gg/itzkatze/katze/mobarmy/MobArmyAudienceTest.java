package gg.itzkatze.katze.mobarmy;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MobArmyAudienceTest {
    @Test void opponentsStayHiddenBeforeAndAfterTheyFinish() {
        UUID player = UUID.randomUUID(), teammate = UUID.randomUUID(), opponent = UUID.randomUUID();
        var roles = new HashMap<>(Map.of(player, role("red", false, "red-world"), teammate, role("red", false, "red-world"),
                opponent, role("blue", false, "blue-world")));
        var before = new MobArmyAudience.View(true, roles);
        assertTrue(before.visible(player, teammate));
        assertFalse(before.visible(player, opponent));
        assertFalse(before.chatVisible(opponent, player));
        roles.put(opponent, role("blue", true, "red-world"));
        var after = new MobArmyAudience.View(true, roles);
        assertFalse(after.visible(player, opponent));
        assertFalse(after.chatVisible(opponent, player));
        assertTrue(after.visible(opponent, player));
        assertTrue(after.chatVisible(player, opponent));
    }

    @Test void spectatorsOnlyReceiveActiveTeamChatInTheWorldTheyWatch() {
        UUID player = UUID.randomUUID(), observer = UUID.randomUUID();
        var view = new MobArmyAudience.View(true, Map.of(player, role("red", false, "red-world"), observer, role(null, true, "blue-world")));
        assertFalse(view.chatVisible(player, observer));
        assertTrue(view.visible(observer, player));
        assertFalse(view.visible(player, observer));
        assertTrue(new MobArmyAudience.View(false, view.roles()).visible(player, observer));
        assertTrue(new MobArmyAudience.View(false, view.roles()).chatVisible(player, observer));
    }

    private static MobArmyAudience.Role role(String side, boolean spectator, String world) { return new MobArmyAudience.Role(side, spectator, world); }
}
