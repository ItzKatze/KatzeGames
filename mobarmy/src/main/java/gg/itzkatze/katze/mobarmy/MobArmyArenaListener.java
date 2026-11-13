package gg.itzkatze.katze.mobarmy;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;
import java.util.Set;

final class MobArmyArenaListener implements Listener {
    private final MobArmyGame game;
    private static final Set<String> WHISPERS = Set.of("msg", "tell", "w");
    private static final Set<String> GLOBAL_CHAT = Set.of("me", "teammsg", "tm");

    MobArmyArenaListener(MobArmyGame game) { this.game = game; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void fatigue(EntityPotionEffectEvent event) {
        if (!game.arena.owns(event.getEntity().getWorld()) || event.getNewEffect() == null
                || !event.getNewEffect().getType().equals(PotionEffectType.MINING_FATIGUE)) return;
        if (guardianEffect(event.getSource() == null ? null : event.getSource().getType(), event.getCause())) {
            event.setCancelled(true);
        }
    }

    static boolean guardianEffect(EntityType source, EntityPotionEffectEvent.Cause cause) {
        return source == EntityType.ELDER_GUARDIAN || source == null && cause == EntityPotionEffectEvent.Cause.ATTACK;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void daylight(EntityCombustEvent event) {
        if (game.tracked(event.getEntity()) != null && !(event instanceof EntityCombustByBlockEvent)
                && !(event instanceof EntityCombustByEntityEvent)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void enter(PlayerTeleportEvent event) {
        if (event.getTo() == null || !game.arena.owns(event.getTo().getWorld())) return;
        MobArmyArena.applyEnvironment(event.getTo().getWorld());
        event.getPlayer().resetPlayerTime();
        event.getPlayer().resetPlayerWeather();
        event.getPlayer().removePotionEffect(PotionEffectType.MINING_FATIGUE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void weather(WeatherChangeEvent event) {
        if (game.arena.owns(event.getWorld()) && event.toWeatherState()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void thunder(ThunderChangeEvent event) {
        if (game.arena.owns(event.getWorld()) && event.toThunderState()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void time(TimeSkipEvent event) { if (game.arena.owns(event.getWorld())) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void death(PlayerDeathEvent event) {
        if (!game.protectedPhase()) return;
        Component message = event.deathMessage();
        event.deathMessage(null);
        game.audience.refresh(game.match);
        game.audience.status(event.getEntity(), message);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void join(PlayerJoinEvent event) {
        if (!game.protectedPhase()) return;
        Component message = event.joinMessage();
        event.joinMessage(null);
        game.audience.refresh(game.match);
        game.audience.status(event.getPlayer(), message);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void quit(PlayerQuitEvent event) {
        if (!game.protectedPhase()) return;
        Component message = event.quitMessage();
        event.quitMessage(null);
        game.audience.status(event.getPlayer(), message);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void advancement(PlayerAdvancementDoneEvent event) {
        if (game.protectedPhase()) event.message(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void chat(AsyncChatEvent event) {
        if (!game.audience.isolated()) return;
        event.viewers().removeIf(viewer -> viewer instanceof Player player
                && !game.audience.chatVisible(event.getPlayer().getUniqueId(), player.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void command(PlayerCommandPreprocessEvent event) {
        if (!game.protectedPhase()) return;
        var side = game.side(event.getPlayer().getUniqueId());
        if (side == null || side.finished) return;
        String[] words = event.getMessage().substring(1).split("\\s+", 3);
        String label = words[0].toLowerCase(Locale.ROOT);
        if (label.startsWith("minecraft:")) label = label.substring("minecraft:".length());
        boolean denied = GLOBAL_CHAT.contains(label) || WHISPERS.contains(label) && words.length > 1 && game.match.findSide(words[1]) != side;
        if (denied) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.translatable("katze.mobarmy.chat.team_only"));
        }
    }
}
