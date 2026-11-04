package gg.itzkatze.katze.game;

import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.lobby.LobbyManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class GameWorldListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (GameWorldManager.isLimbo(event.getFrom().getWorld())) {
            event.setCancelled(true);
            return;
        }
        if (!GameWorldManager.getWorlds().contains(event.getFrom().getWorld())) return;
        if (!GameWorldManager.isReady() || (!ChallengeManager.isGameStarted() && !event.getPlayer().isOp())) {
            event.setCancelled(true);
            return;
        }
        World.Environment source = event.getFrom().getWorld().getEnvironment();
        World.Environment target = switch (event.getCause()) {
            case NETHER_PORTAL -> source == World.Environment.NETHER ? World.Environment.NORMAL : World.Environment.NETHER;
            case END_PORTAL -> source == World.Environment.THE_END ? World.Environment.NORMAL : World.Environment.THE_END;
            default -> null;
        };
        if (target != null) event.setTo(destination(event.getFrom(), target));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (GameWorldManager.isLimbo(event.getFrom().getWorld())) {
            event.setCancelled(true);
            return;
        }
        if (!GameWorldManager.getWorlds().contains(event.getFrom().getWorld())) return;
        if (!GameWorldManager.isReady() || !ChallengeManager.isGameStarted()) {
            event.setCancelled(true);
            return;
        }
        World.Environment source = event.getFrom().getWorld().getEnvironment();
        World.Environment target = switch (event.getPortalType()) {
            case NETHER -> source == World.Environment.NETHER ? World.Environment.NORMAL : World.Environment.NETHER;
            case ENDER -> source == World.Environment.THE_END ? World.Environment.NORMAL : World.Environment.THE_END;
            default -> null;
        };
        if (target != null) event.setTo(destination(event.getFrom(), target));
    }

    private static Location destination(Location from, World.Environment environment) {
        World world = GameWorldManager.getWorld(environment);
        if (environment == World.Environment.THE_END) return new Location(world, 100.5, 50, 0.5, from.getYaw(), from.getPitch());
        if (from.getWorld().getEnvironment() == World.Environment.THE_END) return world.getSpawnLocation();
        double scale = environment == World.Environment.NETHER ? 0.125 : 8;
        Location to = from.clone();
        to.setWorld(world);
        to.setX(Math.clamp(from.getX() * scale, -29999872, 29999872));
        to.setZ(Math.clamp(from.getZ() * scale, -29999872, 29999872));
        to.setY(Math.clamp(from.getY(), world.getMinHeight() + 1, world.getMaxHeight() - 2));
        return to;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!GameWorldManager.isReady() && event.getTo() != null
                && GameWorldManager.getWorlds().contains(event.getTo().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!ChallengeManager.isGameStarted()) {
            LobbyManager.createLobbyIfNeeded();
            Location spawn = LobbyManager.getSpawn();
            if (spawn != null) event.setRespawnLocation(spawn);
        } else if (!GameWorldManager.getWorlds().contains(event.getRespawnLocation().getWorld())) {
            event.setRespawnLocation(GameWorldManager.getWorld().getSpawnLocation());
        }
    }
}
