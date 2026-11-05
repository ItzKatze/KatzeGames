package gg.itzkatze.katze.results;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.player.KatzePlayer;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.logging.Level;

public final class ResultArchives {
    private ResultArchives() {}

    public static boolean save(SavedBattle battle) {
        try {
            BattleResultsStore.installed().save(battle);
            KatzePlayer.getOnlinePlayers().stream().filter(player -> battle.includes(player.getUuid()))
                    .forEach(player -> player.sendMessage(Component.translatable("katze.results.saved")));
            return true;
        } catch (IOException e) {
            KatzeGames.get().getLogger().log(Level.SEVERE, "Failed to archive " + battle.game().displayName() + " results", e);
            return false;
        }
    }
}
