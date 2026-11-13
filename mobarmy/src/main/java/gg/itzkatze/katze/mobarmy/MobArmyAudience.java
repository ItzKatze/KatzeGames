package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.spectator.SpectatorManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class MobArmyAudience {
    private volatile View view = new View(false, Map.of());
    private final Set<Pair> hidden = new HashSet<>();

    void refresh(MobArmyMatch match) {
        List<Player> players = List.copyOf(Bukkit.getOnlinePlayers());
        Map<UUID, Role> roles = new HashMap<>();
        for (Player player : players) {
            var side = match == null ? null : match.side(player.getUniqueId());
            boolean spectator = SpectatorManager.isSpectator(player.getUniqueId()) || side == null || side.finished;
            roles.put(player.getUniqueId(), new Role(side == null ? null : side.id, spectator, player.getWorld().getKey().toString()));
        }
        view = new View(match != null && match.phase != MobArmyMatch.Phase.COLLECTION && match.phase != MobArmyMatch.Phase.RESULTS, Map.copyOf(roles));
        for (Player viewer : players) for (Player target : players) {
            if (viewer.equals(target)) continue;
            Pair pair = new Pair(viewer.getUniqueId(), target.getUniqueId());
            if (!view.visible(pair.viewer, pair.target)) {
                hidden.add(pair);
                viewer.hidePlayer(KatzeGames.get(), target);
            } else if (hidden.remove(pair)) viewer.showPlayer(KatzeGames.get(), target);
        }
    }

    boolean isolated() { return view.isolated; }
    boolean chatVisible(UUID sender, UUID viewer) { return view.chatVisible(sender, viewer); }

    void status(Player source, Component message) {
        if (message == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) if (view.chatVisible(source.getUniqueId(), player.getUniqueId())) player.sendMessage(message);
        Bukkit.getConsoleSender().sendMessage(message);
    }

    void restore() {
        view = new View(false, Map.of());
        for (Pair pair : hidden) {
            Player viewer = Bukkit.getPlayer(pair.viewer);
            Player target = Bukkit.getPlayer(pair.target);
            if (viewer != null && target != null) viewer.showPlayer(KatzeGames.get(), target);
        }
        hidden.clear();
    }

    record Role(String side, boolean spectator, String world) {}
    record View(boolean isolated, Map<UUID, Role> roles) {
        boolean visible(UUID viewer, UUID target) {
            if (!isolated || viewer.equals(target)) return true;
            Role from = roles.get(viewer), to = roles.get(target);
            if (from == null || to == null) return false;
            return from.spectator ? !to.spectator : !to.spectator && from.side != null && from.side.equals(to.side);
        }

        boolean chatVisible(UUID sender, UUID viewer) {
            if (!isolated || sender.equals(viewer)) return true;
            Role from = roles.get(sender), to = roles.get(viewer);
            if (from == null || to == null) return false;
            if (to.spectator) return from.spectator || from.world.equals(to.world);
            return !from.spectator && from.side != null && from.side.equals(to.side);
        }
    }
    private record Pair(UUID viewer, UUID target) {}
}
