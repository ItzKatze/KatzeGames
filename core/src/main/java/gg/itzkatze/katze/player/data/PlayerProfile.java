package gg.itzkatze.katze.player.data;

import com.google.gson.annotations.SerializedName;
import gg.itzkatze.katze.achievements.AchievementProfile;
import gg.itzkatze.katze.lobby.cosmetics.CosmeticProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerProfile {

    private @Nullable String name;
    private int xp;
    @SerializedName(value = "customAchievementNames", alternate = "pdfAchievementNames")
    private boolean customAchievementNames;
    private AchievementProfile achievements = new AchievementProfile();
    private CosmeticProfile cosmetics = new CosmeticProfile();

    public @Nullable String name() {
        return name;
    }

    public int xp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
    }

    public @NotNull CosmeticProfile cosmetics() {
        return cosmetics;
    }

    public @NotNull AchievementProfile achievements() {
        return achievements;
    }

    public boolean customAchievementNames() {
        return customAchievementNames;
    }

    public void setCustomAchievementNames(boolean enabled) {
        customAchievementNames = enabled;
    }

    void rename(@Nullable String name) {
        if (name != null) {
            this.name = name;
        }
    }

    void normalize() {
        xp = Math.max(xp, 0);
        if (achievements == null) achievements = new AchievementProfile();
        achievements.normalize();
        if (cosmetics == null) {
            cosmetics = new CosmeticProfile();
        }
    }
}
