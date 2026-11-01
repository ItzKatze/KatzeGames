package gg.itzkatze.katze.gui.sign;

import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.function.BiConsumer;

public final class NumberSignInput {

    private NumberSignInput() {
    }

    public static void open(
            KatzePlayer player,
            Component label,
            int current,
            int min,
            int max,
            BiConsumer<KatzePlayer, Integer> onComplete,
            BiConsumer<KatzePlayer, Component> onError
    ) {
        SignInput.open(
                player,
                List.of(
                        Component.empty(),
                        label.color(KatzeColor.LIGHT_BLUE),
                        Component.translatable("katze.settings.sign.current", gg.itzkatze.katze.text.TextArg.number("n", current)).color(KatzeColor.LIGHT_GRAY),
                        Component.translatable("katze.settings.sign.range", gg.itzkatze.katze.text.TextArg.number("min", min), gg.itzkatze.katze.text.TextArg.number("max", max)).color(KatzeColor.YELLOW)
                ),
                (katzePlayer, lines) -> {
                    String raw = lines.stream()
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .findFirst()
                            .orElse("");
                    if (raw.isEmpty()) {
                        if (onError != null) {
                            onError.accept(katzePlayer, Component.translatable("katze.settings.sign.error.empty"));
                        }
                        return;
                    }
                    int value;
                    try {
                        value = Integer.parseInt(raw);
                    } catch (NumberFormatException e) {
                        if (onError != null) {
                            onError.accept(katzePlayer, Component.translatable("katze.settings.sign.error.invalid"));
                        }
                        return;
                    }

                    int clamped = Math.max(min, Math.min(max, value));
                    if (value != clamped && onError != null) {
                        onError.accept(katzePlayer, Component.translatable("katze.settings.sign.error.clamped", gg.itzkatze.katze.text.TextArg.number("n", clamped)));
                    }
                    onComplete.accept(katzePlayer, clamped);
                }
        );
    }
}
