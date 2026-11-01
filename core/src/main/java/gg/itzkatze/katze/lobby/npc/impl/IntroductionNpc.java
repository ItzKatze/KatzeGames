package gg.itzkatze.katze.lobby.npc.impl;

import gg.itzkatze.katze.lobby.npc.LobbyNpc;
import gg.itzkatze.katze.lobby.npc.NpcSkins;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.util.KatzeColor;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.IntStream;

public class IntroductionNpc extends LobbyNpc {

    private static final List<Component> DIALOGUE = IntStream.rangeClosed(1, 28)
            .mapToObj(line -> (Component) Component.translatable("katze.forcebattle.guide.line." + line))
            .toList();

    @Override
    public @NotNull String getId() {
        return "introduction";
    }

    @Override
    public @NotNull Component getName() {
        return Component.translatable("katze.forcebattle.guide.name").color(KatzeColor.GOLD);
    }

    @Override
    public @NotNull List<Component> getDialogue(@NotNull KatzePlayer player) {
        return DIALOGUE;
    }

    @Override
    public @Nullable ResolvableProfile getSkin() {
        return NpcSkins.ofTexture("dc5117690e3bc4f7a3385c138fa780bba403886b058c11bbe22862b9b6675fe7");
    }
}
