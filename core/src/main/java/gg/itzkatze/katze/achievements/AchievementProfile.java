package gg.itzkatze.katze.achievements;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AchievementProfile {
    private Map<String, Integer> best = new LinkedHashMap<>();
    private Map<String, Long> completed = new LinkedHashMap<>();
    private Map<String, Integer> pending = new LinkedHashMap<>();

    public int xp() {
        normalize();
        long total = 0;
        for (Achievement achievement : Achievement.values()) {
            for (AchievementStage stage : achievement.stages()) {
                if (completed.getOrDefault(achievement.id() + "/" + stage.id(), 0L) > 0) total += stage.rarity().xp();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public AchievementLevel level() {
        return AchievementLevel.forXp(xp());
    }

    public Map<String, Integer> pending() {
        normalize();
        return Map.copyOf(pending);
    }

    public void queue(Achievement achievement, int value) {
        normalize();
        pending.merge(achievement.id(), value, Math::max);
    }

    public void settled(Achievement achievement) {
        normalize();
        pending.remove(achievement.id());
    }

    public int best(Achievement achievement) {
        normalize();
        return Math.max(0, best.getOrDefault(achievement.id(), 0));
    }

    public boolean recordBest(Achievement achievement, int value) {
        if (value <= best(achievement)) return false;
        best.put(achievement.id(), value);
        return true;
    }

    public boolean completed(Achievement achievement, AchievementStage stage) {
        return completedAt(achievement, stage) > 0;
    }

    public long completedAt(Achievement achievement, AchievementStage stage) {
        normalize();
        return completed.getOrDefault(achievement.id() + "/" + stage.id(), 0L);
    }

    public void complete(Achievement achievement, AchievementStage stage, long timestamp) {
        normalize();
        completed.putIfAbsent(achievement.id() + "/" + stage.id(), Math.max(1, timestamp));
    }

    public int completedStages(Achievement achievement) {
        return (int) achievement.stages().stream().filter(stage -> completed(achievement, stage)).count();
    }

    public void normalize() {
        if (best == null) best = new LinkedHashMap<>();
        if (completed == null) completed = new LinkedHashMap<>();
        if (pending == null) pending = new LinkedHashMap<>();
        best.entrySet().removeIf(entry -> entry.getValue() == null);
        completed.entrySet().removeIf(entry -> entry.getValue() == null);
        pending.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() < 0);
    }
}
