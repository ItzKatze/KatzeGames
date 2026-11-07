package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.bingo.board.BingoObjective;
import gg.itzkatze.katze.bingo.match.BingoMatchManager;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.event.PlayerTeamJoinEvent;
import gg.itzkatze.katze.event.PlayerTeamLeaveEvent;
import gg.itzkatze.katze.event.TimerSetTimeEvent;
import gg.itzkatze.katze.event.TimerStartEvent;
import gg.itzkatze.katze.gui.KatzeGUI;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public final class BingoListener implements Listener {
    private final BingoMatchManager matches;
    public BingoListener(BingoMatchManager matches) { this.matches = matches; }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) matches.completed(killer, BingoObjective.Category.MOB, event.getEntityType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        matches.completed(event.getPlayer(), BingoObjective.Category.ADVANCEMENT, event.getAdvancement().getKey().getKey());
    }

    @EventHandler public void onTeamJoin(PlayerTeamJoinEvent event) { if (ChallengeManager.isGameStarted()) event.setCancelled(true); }
    @EventHandler public void onTeamLeave(PlayerTeamLeaveEvent event) { if (ChallengeManager.isGameStarted()) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void onTimerSet(TimerSetTimeEvent event) { matches.setTime(event.getNewTime()); }
    @EventHandler public void onTimerStart(TimerStartEvent event) { if (matches.ended()) event.setCancelled(true); }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        if (matches.frozen(event.getPlayer()) && event.hasChangedPosition()) event.setTo(event.getFrom());
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) { if (matches.frozen(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) { if (matches.frozen(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) { if (matches.frozen(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) { if (matches.frozen(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) { if (matches.frozen(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onConsume(PlayerItemConsumeEvent event) { if (matches.frozen(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) { if (matches.frozen(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBucketFill(PlayerBucketFillEvent event) { if (matches.frozen(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent event) { if (event.getEntity() instanceof Player player && matches.frozen(player)) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onProjectile(ProjectileLaunchEvent event) { if (event.getEntity().getShooter() instanceof Player player && matches.frozen(player)) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventory(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && !(event.getView().getTopInventory().getHolder(false) instanceof KatzeGUI)
                && matches.frozen(player)) event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) { if (event.getWhoClicked() instanceof Player player && matches.frozen(player)) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && matches.frozen(player)) event.setCancelled(true);
        if (event instanceof EntityDamageByEntityEvent damage) {
            Player attacker = damage.getDamager() instanceof Player player ? player
                    : damage.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player ? player : null;
            if (attacker != null && matches.frozen(attacker)) event.setCancelled(true);
        }
    }
}
