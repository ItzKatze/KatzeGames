package gg.itzkatze.katze.util;

import gg.itzkatze.katze.team.KatzeTeam;
import gg.itzkatze.katze.player.KatzePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.util.Comparator;
import java.util.stream.Collectors;

public class ResultUtils {

    public static Component buildPlayerResultMessage(KatzePlayer player) {
        int place = KatzePlayer.getPlace(player);

        return Component.translatable("katze.game.results.player",
                        gg.itzkatze.katze.text.TextArg.text("name", player.getName()),
                        gg.itzkatze.katze.text.TextArg.number("place", place),
                        gg.itzkatze.katze.text.TextArg.number("points", player.getPoints()))
                .append(Component.translatable("katze.game.results.open")
                        .color(KatzeColor.LIME)
                        .clickEvent(ClickEvent.runCommand("/result player " + player.getName()))
                );
    }

    public static Component buildTeamResultMessage(KatzeTeam team) {
        int place = KatzeTeam.getPlace(team);
        String players = team.getPlayers().stream()
                .sorted(Comparator.comparingInt(KatzePlayer::getPoints).reversed())
                .map(p -> p.getName() + " (" + p.getPoints() + ")")
                .collect(Collectors.joining(", "));

        return Component.translatable("katze.game.results.team",
                        gg.itzkatze.katze.text.TextArg.number("id", team.getId()),
                        gg.itzkatze.katze.text.TextArg.text("players", players),
                        gg.itzkatze.katze.text.TextArg.number("place", place),
                        gg.itzkatze.katze.text.TextArg.number("points", team.getPoints()))
                .append(Component.translatable("katze.game.results.open")
                        .color(KatzeColor.LIME)
                        .clickEvent(ClickEvent.runCommand("/result team " + team.getId())));
    }
}
