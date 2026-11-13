package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.KatzeGames;
import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeType;
import gg.itzkatze.katze.challenges.TimerSpec;
import gg.itzkatze.katze.commands.framework.KatzeCommand;
import gg.itzkatze.katze.configuration.ChallengeSettingsManager;
import gg.itzkatze.katze.game.GameTimer;
import gg.itzkatze.katze.gui.KatzeGUI;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.TextArg;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Locale;

public final class MobArmy implements Challenge {

    final ChallengeSettingsManager settings = new ChallengeSettingsManager(this, MobArmySettings.class);
    final MobArmyGame game = new MobArmyGame(this);

    @Override
    public void onEnable() {
        game.load();
        game.enable();
    }

    @Override
    public void onDisable() {game.disable();}

    @Override
    public void onGameStart() {game.start();}

    @Override
    public void onGameReset() {game.reset();}

    @Override
    public void saveData() {game.save();}

    @Override
    public void loadData() {game.load();}

    @Override
    public void validateRestoredGame() {game.restore();}

    @Override
    public Component getStartError() {
        game.arena.template();
        return game.roster().size() < 2 ? Component.translatable("katze.mobarmy.error.sides") : null;
    }

    @Override
    public @NonNull ChallengeType getType() {return ChallengeType.MOB_ARMY;}

    @Override
    public @NonNull List<KatzeCommand> getCommands() {return MobArmyCommand.commands(game);}

    @Override
    public @NonNull List<Listener> getListeners() {return List.of(new MobArmyListener(game), new MobArmyArenaListener(game));}

    @Override
    public @NonNull List<Runnable> getTasks() {return List.of();}

    @Override
    public ChallengeSettingsManager getSettingsManager() {return settings;}

    @Override
    public KatzeGUI getSettingsGUI() {return new MobArmyMenus.Settings(this);}

    @Override
    public TimerSpec getTimerSpec() {
        return new TimerSpec(GameTimer.Mode.COUNT_DOWN, ((MobArmySettings.Duration) settings.getState(MobArmySettings.COLLECTION_DURATION)).minutes() * 60);
    }

    @Override
    public void onTimerElapsed() {
        if (game.match != null && game.match.phase == MobArmyMatch.Phase.COLLECTION) {
            KatzeGames.get().getServer().getScheduler().runTask(KatzeGames.get(), game::configure);
        }
    }

    @Override
    public boolean allowsCombination(ChallengeType type) {
        return type == ChallengeType.MOB_ARMY || type == ChallengeType.RANDOMIZER || type == ChallengeType.ITEM_RAIN;
    }

    @Override
    public boolean allowsGameplay(ChallengeType type) {
        return type == ChallengeType.MOB_ARMY || game.match == null || game.match.phase == MobArmyMatch.Phase.COLLECTION;
    }

    @Override
    public boolean locksChallengeSelection() {return true;}

    @Override
    public boolean requiresCombatDamage() {return game.combat();}

    @Override
    public Inventory getBackpack(KatzePlayer player) {
        var side = game.side(player.getUuid());
        return side == null || !settings.isEnabled(MobArmySettings.BACKPACK) ? null : game.backpack(side).getInventory();
    }

    @Override
    public Component getActionbar(@NonNull KatzePlayer player, boolean running, int seconds) {
        if (game.match == null || game.match.phase == MobArmyMatch.Phase.COLLECTION) return null;
        if (game.preparingArena) return Component.translatable("katze.mobarmy.arena.preparing");
        if (!running && game.match.phase != MobArmyMatch.Phase.RESULTS) return Component.translatable("katze.game.timer.paused");
        var side = game.side(player.getUuid());
        if (side != null && side.preparing) return Component.translatable("katze.mobarmy.arena.preparing");
        if (side != null && side.roofTicks.containsKey(player.getUuid())) {
            return Component.translatable("katze.mobarmy.roof", TextArg.number("seconds", (side.roofTicks.get(player.getUuid()) + 19) / 20));
        }
        return Component.translatable("katze.mobarmy.phase." + game.match.phase.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public int getActionbarPriority() {return 250;}

    @Override
    public @NonNull List<Component> getSpectatorInfo(KatzePlayer target) {
        var side = game.side(target.getUuid());
        return side == null ? List.of() : List.of(
                Component.text(side.name()),
                Component.translatable("katze.mobarmy.collected", TextArg.number("count", side.mobs.size()))
        );
    }

}
