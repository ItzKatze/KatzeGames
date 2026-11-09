package gg.itzkatze.katze.challenges;

import gg.itzkatze.katze.gui.GUIMaterial;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@Getter
public enum ChallengeType {
    FORCE_BATTLE(new GUIMaterial(Material.DIAMOND_SWORD)),
    ALL_ITEMS(new GUIMaterial(Material.CHEST)),
    DEATH_SWAP(new GUIMaterial(Material.ENDER_PEARL)),
    RANDOMIZER(new GUIMaterial(Material.SNIFFER_EGG)),
    ITEM_RAIN(new GUIMaterial(Material.DROPPER)),
    BINGO(new GUIMaterial(Material.MAP)),
    ;

    private final GUIMaterial material;

    ChallengeType(GUIMaterial material) {
        this.material = material;
    }

    public @NotNull Component displayName() {
        return Component.translatable("katze.game.challenge." + name().toLowerCase(Locale.ROOT) + ".name");
    }

    public @NotNull Component getDescription() {
        return Component.translatable("katze.game.challenge." + name().toLowerCase(Locale.ROOT) + ".description");
    }
}
