package gg.itzkatze.katze.chat;

import gg.itzkatze.katze.level.LevelStore;
import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.spectator.SpectatorManager;
import gg.itzkatze.katze.spectator.WhisperFilter;
import gg.itzkatze.katze.level.LevelTag;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.team.KatzeTeam;
import gg.itzkatze.katze.util.KatzeColor;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class TeamChatManager implements Listener {

    private static final Set<UUID> TEAM_CHAT_PLAYERS = ConcurrentHashMap.newKeySet();

    public static boolean isTeamChatEnabled(@NotNull KatzePlayer player) {
        return TEAM_CHAT_PLAYERS.contains(player.getUuid());
    }

    public static void setTeamChatEnabled(@NotNull KatzePlayer player, boolean enabled) {
        if (enabled) {
            TEAM_CHAT_PLAYERS.add(player.getUuid());
        } else {
            TEAM_CHAT_PLAYERS.remove(player.getUuid());
        }
    }

    public static void sendTeamMessage(@NotNull KatzePlayer sender, @NotNull Component message) {
        KatzeTeam team = sender.getTeam();
        if (team == null) {
            sender.sendMessage(Component.translatable("katze.team.not_in_team").color(KatzeColor.RED));
            return;
        }

        Component formattedMessage = Component.translatable("katze.team.chat.prefix").color(KatzeColor.LIGHT_BLUE)
                .append(Component.text(" "))
                .append(LevelTag.of(LevelStore.get().level(sender.getUuid())))
                .append(Component.text(" <" + sender.getName() + "> ", KatzeColor.WHITE))
                .append(message);
        team.getPlayers().forEach(member -> member.sendMessage(formattedMessage));
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(@NotNull AsyncChatEvent event) {
        Player sender = event.getPlayer();
        UUID id = sender.getUniqueId();
        if (!TEAM_CHAT_PLAYERS.contains(id)) return;
        event.setCancelled(true);
        Component message = event.message();
        Bukkit.getScheduler().runTask(KatzeGames.get(), () -> {
            Player online = Bukkit.getPlayer(id);
            if (online == null) return;
            KatzePlayer player = KatzePlayer.adapt(online);
            if (player.isSpectator() && SpectatorManager.isBattleRunning()) {
                player.sendMessage(WhisperFilter.ONLY_SPECTATOR_CHAT);
                return;
            }
            sendTeamMessage(player, message);
        });
    }
}
