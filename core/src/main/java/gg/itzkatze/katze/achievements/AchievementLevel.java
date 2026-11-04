package gg.itzkatze.katze.achievements;

public record AchievementLevel(int level, int into, int cost) {
    public static final int BASE_COST = 100;
    public static final int COST_INCREMENT = 50;

    public static AchievementLevel forXp(int xp) {
        int remaining = Math.max(0, xp);
        int level = 1;
        int cost = BASE_COST;
        while (remaining >= cost) {
            remaining -= cost;
            level++;
            cost += COST_INCREMENT;
        }
        return new AchievementLevel(level, remaining, cost);
    }
}
