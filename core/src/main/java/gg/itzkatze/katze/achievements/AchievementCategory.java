package gg.itzkatze.katze.achievements;

import gg.itzkatze.katze.challenges.ChallengeType;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.Locale;

@Getter
public enum AchievementCategory {
    GENERAL(Material.BOOK, null),
    FORCE_BATTLE(Material.DIAMOND_SWORD, ChallengeType.FORCE_BATTLE),
    ALL_ITEMS(Material.CHEST, ChallengeType.ALL_ITEMS),
    DEATH_SWAP(Material.ENDER_PEARL, ChallengeType.DEATH_SWAP),
    RANDOMIZER(Material.SNIFFER_EGG, ChallengeType.RANDOMIZER),
    ITEM_RAIN(Material.DROPPER, ChallengeType.ITEM_RAIN),
    BINGO(Material.MAP, ChallengeType.BINGO),
    LOBBY(Material.FEATHER, null);

    private final Material icon;
    private final ChallengeType challenge;

    AchievementCategory(Material icon, ChallengeType challenge) {
        this.icon = icon;
        this.challenge = challenge;
    }

    public Component title() {
        return challenge == null
                ? Component.translatable("katze.achievements.category." + name().toLowerCase(Locale.ROOT))
                : challenge.displayName();
    }
}
