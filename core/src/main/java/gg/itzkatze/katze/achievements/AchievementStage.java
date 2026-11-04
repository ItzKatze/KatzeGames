package gg.itzkatze.katze.achievements;

import java.util.Objects;

public record AchievementStage(String id, int target, AchievementRarity rarity) {
    public AchievementStage {
        Objects.requireNonNull(id);
        Objects.requireNonNull(rarity);
        if (id.isBlank() || target <= 0) throw new IllegalArgumentException("Invalid achievement stage");
    }
}
