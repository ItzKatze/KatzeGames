package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.event.PlayerTeamJoinEvent;
import gg.itzkatze.katze.event.PlayerTeamLeaveEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class MobArmyListener implements Listener {

    private final MobArmyGame game;
    private final Map<UUID, MobArmyMatch.CapturedMob> lethal = new HashMap<>();

    MobArmyListener(MobArmyGame game) {this.game = game;}

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void capture(EntityDamageEvent event) {
        if (game.match == null || game.match.phase != MobArmyMatch.Phase.COLLECTION
                || !(event.getEntity() instanceof LivingEntity living) || !MobCapture.eligible(living)
                || event.getFinalDamage() < living.getHealth()) return;
        lethal.put(living.getUniqueId(), MobCapture.capture(living));
        later(() -> lethal.remove(living.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void mobDeath(EntityDeathEvent event) {
        var entity = event.getEntity();
        if (entity instanceof Player) return;
        var tracked = game.tracked(entity);
        if (tracked != null || game.arena.owns(entity.getWorld())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            game.died(entity);
            return;
        }
        var saved = lethal.remove(entity.getUniqueId());
        if (game.match == null || !game.running() || game.match.phase != MobArmyMatch.Phase.COLLECTION
                || !MobCapture.eligible(entity) || entity.getKiller() == null) return;
        var side = game.side(entity.getKiller().getUniqueId());
        if (side == null) return;
        side.mobs.add(saved != null ? saved : MobCapture.capture(entity));
        game.dirty();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void death(PlayerDeathEvent event) {
        if (game.match != null && game.match.phase == MobArmyMatch.Phase.COLLECTION) {
            var player = event.getEntity();
            var side = game.side(player.getUniqueId());
            if (side != null) {
                var saved = PlayerLoadout.capture(player);
                if (!event.getKeepInventory()) {
                    ItemStack[] retained = player.getInventory().getContents();
                    for (int slot = 0; slot < retained.length; slot++) {
                        if (!MobArmyEquipment.isStarter(retained[slot])) retained[slot] = null;
                    }
                    saved.inventory = PlayerLoadout.encode(retained);
                    MobArmyEquipment.retain(event);
                    saved.cursor = PlayerLoadout.encode(new ItemStack[]{null});
                }
                if (!event.getKeepLevel()) {
                    saved.level = event.getNewLevel();
                    saved.experience = 0;
                    saved.totalExperience = event.getNewTotalExp();
                }
                saved.health = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                saved.effects = List.of();
                saved.fireTicks = 0;
                saved.freezeTicks = 0;
                side.players.put(player.getUniqueId(), saved);
                game.dirty();
            }
        }
        if (!game.combat() || game.side(event.getEntity().getUniqueId()) == null) return;
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        game.death(event.getEntity());
        later(() -> {if (event.getEntity().isOnline() && event.getEntity().isDead()) event.getEntity().spigot().respawn();});
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void respawn(PlayerRespawnEvent event) {
        var side = game.side(event.getPlayer().getUniqueId());
        if (side == null || !game.protectedPhase()) return;
        int index = game.match.sides.indexOf(side);
        event.setRespawnLocation(game.roof(event.getPlayer()) || !game.combat() ? game.arena.roof(index) : game.arena.entrance(index));
        later(() -> game.respawn(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void join(PlayerJoinEvent event) {later(() -> game.join(event.getPlayer()));}

    @EventHandler(priority = EventPriority.MONITOR)
    public void quit(PlayerQuitEvent event) {
        game.rememberPlayer(event.getPlayer());
        game.dirty();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void spawn(CreatureSpawnEvent event) {
        if (!game.arena.owns(event.getLocation().getWorld())) return;
        int index = game.match == null ? -1 : game.arena.index(event.getLocation(), game.match.sides.size());
        if (!game.combat() || index < 0 || game.match.sides.get(index).finished || game.match.sides.get(index).preparing
                || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL || !MobCapture.eligible(event.getEntity())) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof IronGolem golem && !MobCapture.hostileGolem(golem)) {
            golem.setPlayerCreated(true);
            return;
        }
        game.track(game.match.sides.get(index), event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void transform(EntityTransformEvent event) {
        var side = game.tracked(event.getEntity());
        if (side == null) return;
        side.live.remove(event.getEntity().getUniqueId());
        for (Entity entity : event.getTransformedEntities()) if (entity instanceof LivingEntity living) game.track(side, living);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void remove(EntityRemoveEvent event) {
        var side = game.tracked(event.getEntity());
        if (side == null || !(event.getEntity() instanceof LivingEntity living)) return;
        if (event.getCause() == EntityRemoveEvent.Cause.DEATH || event.getCause() == EntityRemoveEvent.Cause.EXPLODE
                || event.getCause() == EntityRemoveEvent.Cause.TRANSFORMATION) {
            game.died(living);
        } else {
            var saved = side.live.get(living.getUniqueId());
            later(() -> {
                if (!game.combat() || side.finished || !side.live.containsKey(living.getUniqueId())) return;
                side.live.remove(living.getUniqueId());
                LivingEntity replacement = MobCapture.spawn(saved.data(), game.arena.world(game.match.sides.indexOf(side)), false,
                        entity -> game.arena.spawn(game.match.sides.indexOf(side), entity));
                game.track(side, replacement);
            });
        }
    }

    private Entity attacker(Entity entity) {
        if (entity instanceof TNTPrimed tnt && tnt.getSource() != null) return tnt.getSource();
        return entity instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter ? shooter : entity;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void damage(EntityDamageByEntityEvent event) {
        if (!game.protectedPhase()) return;
        Entity source = attacker(event.getDamager());
        if (event.getDamager() instanceof TNTPrimed && event.getEntity() instanceof Player) {
            event.setCancelled(true);
            return;
        }
        if (source instanceof Player player) {
            if (game.blocked(player) || event.getEntity() instanceof Player) {
                event.setCancelled(true);
                return;
            }
            var targetSide = game.tracked(event.getEntity());
            if (targetSide != null && targetSide != game.side(player.getUniqueId())) event.setCancelled(true);
        }
        if (source instanceof IronGolem) {
            if (game.tracked(source) != null && !(event.getEntity() instanceof Player)
                    || game.tracked(source) == null && event.getEntity() instanceof Player) event.setCancelled(true);
        }
        if (event.getEntity() instanceof Player player && game.tracked(source) != null && game.tracked(source) != game.side(player.getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void damage(EntityDamageEvent event) {
        if (game.combat() && game.tracked(event.getEntity()) != null && MobHabitat.preventsDamage(event.getEntity().getType(), event.getCause())) {
            event.setCancelled(true);
            return;
        }
        if (game.protectedPhase() && game.arena.owns(event.getEntity().getWorld()) && !game.running()) {
            event.setCancelled(true);
            return;
        }
        if (game.protectedPhase() && event.getEntity() instanceof Player && event instanceof EntityDamageByBlockEvent
                && event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player player && game.protectedPhase() && game.blocked(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void target(EntityTargetLivingEntityEvent event) {
        if (game.combat() && game.arena.owns(event.getEntity().getWorld())
                && event.getEntity() instanceof IronGolem && game.tracked(event.getEntity()) == null
                && event.getTarget() instanceof Player) {
            event.setCancelled(true);
            return;
        }
        var side = game.tracked(event.getEntity());
        if (side == null) return;
        if (event.getTarget() instanceof Player player && (game.blocked(player) || game.side(player.getUniqueId()) != side)) event.setCancelled(true);
        if (event.getEntity() instanceof IronGolem && event.getTarget() != null && !(event.getTarget() instanceof Player)) event.setCancelled(true);
    }

    private boolean canBuild(Player player, Block block) {
        var side = game.side(player.getUniqueId());
        return game.combat() && !game.blocked(player) && side != null && game.running()
                && game.arena.inside(game.match.sides.indexOf(side), block.getLocation(), false)
                && block.getY() > MobArmyArena.FLOOR && block.getY() < game.arena.roofHeight() - 2;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void place(BlockPlaceEvent event) {
        if (!game.protectedPhase()) return;
        var side = game.side(event.getPlayer().getUniqueId());
        if (!canBuild(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        var replaced = event instanceof BlockMultiPlaceEvent multi ? multi.getReplacedBlockStates() : List.of(event.getBlockReplacedState());
        if (replaced.stream().anyMatch(state -> !canBuild(event.getPlayer(), state.getBlock()))) {
            event.setCancelled(true);
            return;
        }
        for (var state : replaced) {
            Block block = state.getBlock();
            if (side.blocks.stream().noneMatch(change -> change.x() == block.getX() && change.y() == block.getY() && change.z() == block.getZ())) {
                side.blocks.add(new MobArmyMatch.BlockChange(block.getX(), block.getY(), block.getZ(), state.getBlockData().getAsString(), true));
            }
        }
        game.dirty();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent event) {
        if (!game.protectedPhase()) return;
        var side = game.side(event.getPlayer().getUniqueId());
        if (!canBuild(event.getPlayer(), event.getBlock()) || !game.arena.placed(side, event.getBlock())) event.setCancelled(true);
        else game.dirty();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void interact(PlayerInteractEvent event) {
        if (!game.protectedPhase()) return;
        if (game.blocked(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void interactEntity(PlayerInteractEntityEvent event) {
        if (game.protectedPhase() && (game.blocked(event.getPlayer()) || event.getRightClicked() instanceof Player
                || game.tracked(event.getRightClicked()) != game.side(event.getPlayer().getUniqueId()))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void move(PlayerMoveEvent event) {
        if (!game.protectedPhase()) return;
        var side = game.side(event.getPlayer().getUniqueId());
        if (side == null || side.finished) return;
        boolean roof = !game.combat() || game.roof(event.getPlayer());
        int index = game.match.sides.indexOf(side);
        if (!game.arena.inside(index, event.getTo(), roof) || roof && event.getTo().getY() < game.arena.roofHeight() + 1) event.setTo(event.getFrom());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void teleport(EntityTeleportEvent event) {
        var side = game.tracked(event.getEntity());
        if (side != null && event.getTo() != null && !game.arena.insideMob(game.match.sides.indexOf(side), event.getTo())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void teleport(PlayerTeleportEvent event) {
        if (!game.protectedPhase() || event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) return;
        var side = game.side(event.getPlayer().getUniqueId());
        if (side == null || side.finished) return;
        boolean roof = !game.combat() || game.roof(event.getPlayer());
        if (event.getTo() == null || !game.arena.inside(game.match.sides.indexOf(side), event.getTo(), roof)
                || roof && event.getTo().getY() < game.arena.roofHeight() + 1) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void inventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getHolder() instanceof MobArmyMenus.Backpack backpack) {
            if (!backpack.accessible(player)) event.setCancelled(true);
            else game.dirty();
        } else if (game.protectedPhase() && game.roof(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getHolder() instanceof MobArmyMenus.Backpack backpack) {
            if (!backpack.accessible(player)) event.setCancelled(true);
            else game.dirty();
        } else if (game.protectedPhase() && game.roof(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void hopper(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof MobArmyMenus.Backpack || event.getDestination().getHolder() instanceof MobArmyMenus.Backpack)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void drop(PlayerDropItemEvent event) {if (game.protectedPhase() && game.blocked(event.getPlayer())) event.setCancelled(true);}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void bucket(PlayerBucketEmptyEvent event) {
        if (!game.protectedPhase()) return;
        if (!canBuild(event.getPlayer(), event.getBlock())) event.setCancelled(true);
        else {
            game.arena.remember(game.side(event.getPlayer().getUniqueId()), event.getBlock(), true);
            game.dirty();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void bucket(PlayerBucketFillEvent event) {
        if (!game.protectedPhase()) return;
        if (!canBuild(event.getPlayer(), event.getBlock()) || !game.arena.placed(game.side(event.getPlayer().getUniqueId()), event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void flow(BlockFromToEvent event) {
        if (!game.arena.owns(event.getBlock().getWorld())) return;
        int index = game.match == null ? -1 : game.arena.index(event.getToBlock().getLocation(), game.match.sides.size());
        if (index < 0 || !game.combat() || game.match.sides.get(index).preparing || event.getToBlock().getY() <= MobArmyArena.FLOOR
                || event.getToBlock().getY() >= game.arena.roofHeight() - 2) {
            event.setCancelled(true);
            return;
        }
        game.arena.remember(game.match.sides.get(index), event.getToBlock(), true);
        game.dirty();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explode(EntityExplodeEvent event) {
        if (!game.arena.owns(event.getLocation().getWorld())) return;
        event.blockList().clear();
        event.setYield(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explode(BlockExplodeEvent event) {
        if (game.arena.owns(event.getBlock().getWorld())) {
            event.blockList().clear();
            event.setYield(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void change(EntityChangeBlockEvent event) {if (game.arena.owns(event.getBlock().getWorld())) event.setCancelled(true);}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void burn(BlockBurnEvent event) {if (game.arena.owns(event.getBlock().getWorld())) event.setCancelled(true);}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void ignite(BlockIgniteEvent event) {
        if (!game.arena.owns(event.getBlock().getWorld())) return;
        if (event.getPlayer() == null || !canBuild(event.getPlayer(), event.getBlock())) event.setCancelled(true);
        else game.arena.remember(game.side(event.getPlayer().getUniqueId()), event.getBlock(), true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void spread(BlockSpreadEvent event) {if (game.arena.owns(event.getBlock().getWorld())) event.setCancelled(true);}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void form(BlockFormEvent event) {if (game.arena.owns(event.getBlock().getWorld())) event.setCancelled(true);}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void fade(BlockFadeEvent event) {if (game.arena.owns(event.getBlock().getWorld())) event.setCancelled(true);}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void piston(BlockPistonExtendEvent event) {if (game.arena.owns(event.getBlock().getWorld())) event.setCancelled(true);}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void piston(BlockPistonRetractEvent event) {if (game.arena.owns(event.getBlock().getWorld())) event.setCancelled(true);}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void splash(PotionSplashEvent event) {
        if (!game.protectedPhase()) return;
        Entity source = attacker(event.getPotion());
        if (source instanceof Player player) {
            boolean harmful = event.getPotion().getEffects().stream().anyMatch(effect -> effect.getType().getEffectCategory() == PotionEffectType.Category.HARMFUL);
            for (LivingEntity entity : event.getAffectedEntities())
                if (entity instanceof Player target
                        && (game.blocked(player) || game.blocked(target) || harmful && !player.equals(target)
                        || game.side(player.getUniqueId()) != game.side(target.getUniqueId()))) event.setIntensity(entity, 0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void cloud(AreaEffectCloudApplyEvent event) {
        if (!game.protectedPhase()) return;
        var cloud = event.getEntity();
        boolean harmful = cloud.getCustomEffects().stream().anyMatch(effect -> effect.getType().getEffectCategory() == PotionEffectType.Category.HARMFUL)
                || cloud.getBasePotionType() != null && cloud.getBasePotionType().getPotionEffects().stream()
                .anyMatch(effect -> effect.getType().getEffectCategory() == PotionEffectType.Category.HARMFUL);
        event.getAffectedEntities().removeIf(entity -> entity instanceof Player player && (game.blocked(player)
                || cloud.getSource() instanceof Player owner && (game.blocked(owner) || harmful && !owner.equals(player)
                || game.side(owner.getUniqueId()) != game.side(player.getUniqueId()))));
    }

    @EventHandler
    public void teamJoin(PlayerTeamJoinEvent event) {if (game.match != null) event.setCancelled(true);}

    @EventHandler
    public void teamLeave(PlayerTeamLeaveEvent event) {if (game.match != null) event.setCancelled(true);}

    private void later(Runnable action) {KatzeGames.get().getServer().getScheduler().runTask(KatzeGames.get(), action);}

}
