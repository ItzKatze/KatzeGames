package gg.itzkatze.katze.lobby.cosmetics;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public final class PointsText {

    private PointsText() {
    }

    public static @NotNull String amount(int points) {
        return points + (points == 1 ? " point" : " points");
    }

    public static @NotNull String signed(int points) {
        return (points < 0 ? "-" : "+") + amount(Math.abs(points));
    }

    public static @NotNull String missing(int points) {
        return "You need " + points + " more " + (points == 1 ? "point." : "points.");
    }

    public static @NotNull Component amountComponent(int points) {
        return Component.translatable("katze.cosmetics.points_amount", gg.itzkatze.katze.text.TextArg.number("points", points));
    }

    public static @NotNull Component signedComponent(int points) {
        return Component.translatable("katze.cosmetics.points_signed", gg.itzkatze.katze.text.TextArg.signed("points", points));
    }

    public static @NotNull Component missingComponent(int points) {
        return Component.translatable("katze.cosmetics.points_missing", gg.itzkatze.katze.text.TextArg.number("points", points));
    }
}
