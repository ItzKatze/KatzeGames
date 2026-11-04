package gg.itzkatze.katze.achievements;

import gg.itzkatze.katze.challenges.ChallengeType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AchievementRound {
    private int version = 1;
    private UUID id = UUID.randomUUID();
    private boolean finished;
    private Set<ChallengeType> initialChallenges = new HashSet<>();
    private Set<ChallengeType> ended = new HashSet<>();
    private Map<UUID, Participant> players = new HashMap<>();
    private Map<String, UUID> rods = new HashMap<>();

    public UUID id() {
        return id;
    }

    public boolean finished() {
        return finished;
    }

    public Set<ChallengeType> initialChallenges() {
        return initialChallenges;
    }

    public Set<ChallengeType> ended() {
        return ended;
    }

    public Map<UUID, Participant> players() {
        return players;
    }

    public Map<String, UUID> rods() {
        return rods;
    }

    public Participant player(UUID id) {
        return players.computeIfAbsent(id, ignored -> new Participant());
    }

    public boolean end(ChallengeType challenge) {
        if (!ended.add(challenge)) return false;
        finished = true;
        return true;
    }

    public boolean normalize() {
        if (version != 1 || id == null) return false;
        if (initialChallenges == null) initialChallenges = new HashSet<>();
        if (ended == null) ended = new HashSet<>();
        if (players == null) players = new HashMap<>();
        if (rods == null) rods = new HashMap<>();
        players.values().removeIf(player -> player == null);
        players.values().forEach(Participant::normalize);
        return true;
    }

    public static final class Participant {
        private String name;
        private boolean fullRound;
        private boolean ateVegan;
        private boolean ateNonVegan;
        private boolean usedJoker;
        private Map<String, Integer> counters = new HashMap<>();
        private Map<String, Set<String>> distinct = new HashMap<>();

        public String name() { return name; }
        public void name(String name) { this.name = name; }
        public boolean fullRound() { return fullRound; }
        public void fullRound(boolean fullRound) { this.fullRound = fullRound; }
        public boolean vegan() { return fullRound && ateVegan && !ateNonVegan; }
        public void food(boolean vegan) {
            ateVegan |= vegan;
            ateNonVegan |= !vegan;
        }
        public boolean usedJoker() { return usedJoker; }
        public void useJoker() { usedJoker = true; }
        public int value(Achievement achievement) { return counters.getOrDefault(achievement.id(), 0); }
        public void set(Achievement achievement, int value) { counters.put(achievement.id(), Math.max(0, value)); }
        public int add(Achievement achievement, int amount) {
            int value = (int) Math.min(Integer.MAX_VALUE, (long) value(achievement) + Math.max(0, amount));
            set(achievement, value);
            return value;
        }
        public int distinct(Achievement achievement, String key) {
            Set<String> values = distinct.computeIfAbsent(achievement.id(), ignored -> new HashSet<>());
            if (values.size() < achievement.maximum()) values.add(key);
            set(achievement, values.size());
            return values.size();
        }
        private void normalize() {
            if (counters == null) counters = new HashMap<>();
            if (distinct == null) distinct = new HashMap<>();
            counters.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() < 0);
            distinct.entrySet().removeIf(entry -> entry.getValue() == null);
        }
    }
}
