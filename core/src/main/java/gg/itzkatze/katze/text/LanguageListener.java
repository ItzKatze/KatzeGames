package gg.itzkatze.katze.text;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;

public final class LanguageListener implements Listener {

    @EventHandler
    public void onLocaleChange(PlayerLocaleChangeEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(KatzeGames.get(), () -> {
            if (!player.isOnline()) return;

            KatzeGUI gui = KatzeGUI.GUI_MAP.get(player.getUniqueId());
            if (gui != null) {
                gui.refreshLanguage(KatzePlayer.adapt(player));
            }
            ScoreboardManager.updateAll();
        });
    }
}
