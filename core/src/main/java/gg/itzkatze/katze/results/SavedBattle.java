package gg.itzkatze.katze.results;

import java.util.List;
import java.util.UUID;

public record SavedBattle(UUID id, long startedAt, long endedAt, int durationSeconds,
                         List<Player> players, List<Team> teams, Game game) {

    public SavedBattle {
        players = List.copyOf(players);
        teams = List.copyOf(teams);
        game = game == null ? Game.FORCE_BATTLE : game;
    }

    public SavedBattle(UUID id, long startedAt, long endedAt, int durationSeconds, List<Player> players, List<Team> teams) {
        this(id, startedAt, endedAt, durationSeconds, players, teams, Game.FORCE_BATTLE);
    }

    public enum Game {
        FORCE_BATTLE("ForceBattle"), ALL_ITEMS("AllItems"), DEATH_SWAP("DeathSwap");

        private final String displayName;

        Game(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public boolean includes(UUID player) {
        return players.stream().anyMatch(entry -> entry.id().equals(player));
    }

    public record Player(UUID id, String name, Integer teamId, int points, List<Objective> objectives, List<Statistic> statistics) {
        public Player {
            objectives = List.copyOf(objectives);
            statistics = statistics == null ? List.of() : List.copyOf(statistics);
        }

        public Player(UUID id, String name, Integer teamId, int points, List<Objective> objectives) {
            this(id, name, teamId, points, objectives, List.of());
        }
    }

    public record Statistic(String key, int value, int total) {}

    public record Team(int id, int points, List<Objective> objectives) {
        public Team {
            objectives = List.copyOf(objectives);
        }
    }

    public record Objective(String name, String type, String icon, String finishedBy,
                            int remainingSeconds, boolean finished, boolean skipped) {
    }
}
