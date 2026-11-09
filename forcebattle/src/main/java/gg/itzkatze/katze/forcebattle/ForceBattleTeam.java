package gg.itzkatze.katze.forcebattle;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.achievements.AchievementService;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.configuration.SettingsManager;
import gg.itzkatze.katze.event.ObjectiveUpdateEvent;
import gg.itzkatze.katze.event.PlayerTeamJoinEvent;
import gg.itzkatze.katze.event.PlayerTeamLeaveEvent;
import gg.itzkatze.katze.forcebattle.gui.TeamBackpackGUI;
import gg.itzkatze.katze.game.SessionPersistence;
import gg.itzkatze.katze.level.LevelStore;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.team.KatzeTeam;
import gg.itzkatze.katze.text.TextArg;
import gg.itzkatze.katze.util.KatzeColor;
import gg.itzkatze.katze.util.Toast;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ForceBattleTeam extends KatzeTeam {

    private static final Logger logger = LoggerFactory.getLogger("ForceBattle/" + ForceBattleTeam.class.getSimpleName());

    private static final int SLOTS_PER_MEMBER = 45;
    private static final int MAX_PAGE_SIZE = 54;
    @Getter
    private final List<TeamBackpackGUI> BACKPACK_INVENTORIES = new ArrayList<>();
    @Getter
    @Setter
    private int chainProgress = 0;
    @Getter
    @Setter
    private ForceBattleObjective objective;
    @Getter
    @Setter
    private ForceBattleObjective lastObjective;

    public ForceBattleTeam(List<KatzePlayer> players) {
        super(players);
    }

    public ForceBattleTeam(List<KatzePlayer> players, int id, boolean register) {
        super(players, id, register);
    }

    @Override
    public List<KatzePlayer> getPlayers() {
        return players;
    }


    @Override
    public void addPlayer(KatzePlayer katzePlayer) {
        PlayerTeamJoinEvent teamJoinEvent = new PlayerTeamJoinEvent(this, katzePlayer);
        Bukkit.getPluginManager().callEvent(teamJoinEvent);
        if (teamJoinEvent.isCancelled()) {
            logger.info("PlayerTeamJoinEvent has been denied by an external plugin.");
            return;
        }

        Component joinMessage = Component.translatable("katze.forcebattle.team.member_joined",
                TextArg.text("player", katzePlayer.getName()));
        players.forEach(teamPlayer -> teamPlayer.sendMessage(joinMessage));
        players.add(katzePlayer);
        katzePlayer.sendMessage(Component.translatable("katze.forcebattle.team.joined",
                TextArg.number("team", this.getId())).color(KatzeColor.LIME));
    }

    @Override
    public void removePlayer(KatzePlayer katzePlayer) {
        PlayerTeamLeaveEvent teamLeaveEvent = new PlayerTeamLeaveEvent(this, katzePlayer);
        Bukkit.getPluginManager().callEvent(teamLeaveEvent);
        if (teamLeaveEvent.isCancelled()) {
            logger.info("PlayerTeamLeaveEvent has been denied by an external plugin.");
            return;
        }

        players.remove(katzePlayer);
        Component leaveMessage = Component.translatable("katze.forcebattle.team.member_left",
                TextArg.text("player", katzePlayer.getName()));
        players.forEach(teamPlayer -> teamPlayer.sendMessage(leaveMessage));
        if (players.isEmpty()) {
            this.delete();
        }
    }

    @Override
    public void updateObjective(KatzePlayer finisher, boolean finishLast, boolean hasBeenSkipped) {
        SessionPersistence.requestSave(ChallengeType.FORCE_BATTLE);
        if (finishLast && this.objective != null) {
            boolean firstCompletion = !this.objective.isFinished();
            this.objective.setFinished(finisher);
            this.objective.setHasBeenSkipped(hasBeenSkipped);

            finisher.setPoints(finisher.getPoints() + 1);
            if (!hasBeenSkipped && finisher.isParticipating()) {
                LevelStore.get().grant(finisher, ForceBattleXp.OBJECTIVE.xp());
                if (firstCompletion && finisher.getPlayer() != null) AchievementService.get().recordObjective(finisher.getPlayer(), true);
            }
        }

        ForceBattleObjective newObjective = ForceBattleObjectiveManager.getInstance().getNewObjective(this);

        ObjectiveUpdateEvent updateEvent = new ObjectiveUpdateEvent(this, newObjective);
        Bukkit.getPluginManager().callEvent(updateEvent);
        if (updateEvent.isCancelled()) {
            if (newObjective != null) newObjective.delete();
            return;
        }

        this.objective = (ForceBattleObjective) updateEvent.getNewObjective();
        if (this.objective != null && this.objective.getBattleType() == ForceBattleType.FORCE_ITEM) {
            Material material = this.objective.getObjectiveValue(Material.class);
            if (material != null) players.stream().map(KatzePlayer::getPlayer).filter(Objects::nonNull)
                    .forEach(player -> AchievementService.get().recordAssignment(player, material));
        }
        if (KatzeGames.getTimer().isRunning() && this.objective != null) {
            Material toastIcon = this.objective.getToastIcon();
            Component objectiveName = this.objective.displayName().color(KatzeColor.BLUE);
            Component toastMessage = Component.translatable("katze.forcebattle.team.new_team_objective",
                    TextArg.component("objective", objectiveName));
            for (KatzePlayer member : players) {
                member.sendMessage(Component.translatable("katze.forcebattle.team.next_objective",
                        TextArg.component("objective", objectiveName),
                        TextArg.component("type", this.objective.getBattleType().displayName())));
                if (member.getPlayer() != null) {
                    Bukkit.getScheduler().runTaskLater(
                            KatzeGames.get(),
                            () -> Toast.display(member.getPlayer(), toastIcon, toastMessage, Toast.Style.CHALLENGE, KatzeGames.get()),
                            2L
                    );
                }
            }
        }
        if (SettingsManager.isEnabled(ForceBattleSettings.CHAIN_MODE)) {
            increaseChainProgress();
        }
    }

    /**
     * Increases the chain progress for ForceBattle.
     */
    public void increaseChainProgress() {
        chainProgress++;
    }

    public static Optional<ForceBattleTeam> findById(int id) {
        Optional<KatzeTeam> teamOpt = KatzeTeam.ofId(id);
        if (teamOpt.isPresent() && teamOpt.get() instanceof ForceBattleTeam) {
            return Optional.of((ForceBattleTeam) teamOpt.get());
        }
        return Optional.empty();
    }

    public void sendJoinRequest(KatzePlayer katzePlayer) {
        if (!isPrivate()) {
            katzePlayer.addToTeam(this);
            return;
        }
        this.lastJoinRequest = new JoinRequest(katzePlayer, this);
        Component requestMessage = Component.translatable("katze.forcebattle.team.join_request",
                TextArg.text("player", katzePlayer.getName()));
        players.forEach(teamPlayer -> teamPlayer.sendMessage(requestMessage));
    }

    // Override delete to handle ForceBattle-specific cleanup
    @Override
    public void delete() {
        // Clean up ForceBattle-specific resources
        BACKPACK_INVENTORIES.clear();
        super.delete();
    }

    public static void deleteAll() {
        // Find all ForceBattleTeam instances in the base teams list
        List<ForceBattleTeam> forceBattleTeams = new ArrayList<>();
        for (KatzeTeam team : KatzeTeam.teams) {
            if (team instanceof ForceBattleTeam) {
                forceBattleTeams.add((ForceBattleTeam) team);
            }
        }

        for (ForceBattleTeam team : forceBattleTeams) {
            team.delete();
        }
    }

    public static Map<Integer, ForceBattleTeam> getTeamPlaces() {
        Map<Integer, ForceBattleTeam> places = new HashMap<>();

        final Map<ForceBattleTeam, Integer> TEAM_POINTS_MAP = new HashMap<>();
        for (KatzeTeam team : KatzeTeam.teams) {
            if (team instanceof final ForceBattleTeam fbTeam) {
                TEAM_POINTS_MAP.put(fbTeam, fbTeam.getPoints());
            }
        }

        List<Map.Entry<ForceBattleTeam, Integer>> sortedTeams = TEAM_POINTS_MAP.entrySet()
                .stream()
                .sorted(Map.Entry.<ForceBattleTeam, Integer>comparingByValue().reversed())
                .toList();

        int place = 1;
        for (Map.Entry<ForceBattleTeam, Integer> entry : sortedTeams) {
            places.put(place++, entry.getKey());
        }

        return places;
    }

    public static int getPlace(ForceBattleTeam team) {
        List<ForceBattleTeam> sortedTeams = new ArrayList<>();
        for (KatzeTeam t : KatzeTeam.teams) {
            if (t instanceof ForceBattleTeam) {
                sortedTeams.add((ForceBattleTeam) t);
            }
        }
        sortedTeams.sort(Comparator.comparingInt(ForceBattleTeam::getPoints).reversed());

        int place = 1;
        int previousPoints = -1;

        for (int i = 0; i < sortedTeams.size(); i++) {
            ForceBattleTeam current = sortedTeams.get(i);

            if (current.getPoints() != previousPoints) {
                place = i + 1;
                previousPoints = current.getPoints();
            }

            if (current.equals(team)) {
                return place;
            }
        }

        return -1;
    }


    public void createBackpackGUIs() {
        int requiredSlots = getPlayers().size() * SLOTS_PER_MEMBER;
        int requiredPages = (int) Math.ceil(requiredSlots / (double) MAX_PAGE_SIZE);

        BACKPACK_INVENTORIES.clear();

        for (int i = 0; i < requiredPages; i++) {
            int pageSize = Math.min(MAX_PAGE_SIZE, requiredSlots - (i * MAX_PAGE_SIZE));

            BACKPACK_INVENTORIES.add(new TeamBackpackGUI(i, pageSize, this));
        }
    }

}

