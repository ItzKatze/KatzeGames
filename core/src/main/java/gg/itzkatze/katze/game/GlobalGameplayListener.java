package gg.itzkatze.katze.game;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.ChallengeManager;
import gg.itzkatze.katze.configuration.GlobalSetting;
import gg.itzkatze.katze.configuration.SettingsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public final class GlobalGameplayListener implements Listener {

    private boolean isInactiveGame() {
        GameTimer timer = KatzeGames.getTimer();
        return ChallengeManager.isGameStarted() && timer.getMode() != GameTimer.Mode.DISABLED && !timer.isRunning();
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        boolean combatDamage = ChallengeManager.getActiveChallenges().stream().anyMatch(challenge -> challenge.requiresCombatDamage());
        if (event.getEntity() instanceof Player && (isInactiveGame() || (!combatDamage && SettingsManager.isEnabled(GlobalSetting.NO_DAMAGE)))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && (isInactiveGame() || SettingsManager.isEnabled(GlobalSetting.NO_HUNGER))) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20);
        }
    }
}
