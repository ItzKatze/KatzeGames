package gg.itzkatze.katze.lobby.jumpandrun;

import gg.itzkatze.katze.lobby.cosmetics.BlockCosmetic;
import gg.itzkatze.katze.lobby.cosmetics.CosmeticCategory;
import gg.itzkatze.katze.lobby.cosmetics.CosmeticStore;
import gg.itzkatze.katze.lobby.cosmetics.SoundCosmetic;
import lombok.Getter;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.Objects;
import java.util.UUID;

@Getter
final class JumpRun {

    private final Player player;
    private final UUID id;
    private RunLook look;
    private Sound landingSound;
    private final BitSet visited = new BitSet();
    private Cell current;
    private Material currentBlock;
    private @Nullable Cell next;
    private HopDifficulty nextDifficulty = HopDifficulty.NORMAL;
    private boolean nextEvent;
    private @Nullable HopSteering.Leg leg;
    private int score;
    private int eventBlocks;
    private int eventPoints;

    JumpRun(@NotNull Player player, @NotNull RunLook look, @NotNull Cell start, @NotNull Cell next) {
        this.player = player;
        this.id = player.getUniqueId();
        this.look = look;
        this.landingSound = CosmeticStore.get().selectForRun(id, CosmeticCategory.SOUND)
                .map(SoundCosmetic.class::cast).map(SoundCosmetic::sound).orElse(SoundCosmetic.DEFAULT);
        this.current = start;
        this.currentBlock = look.block();
        this.next = next;
    }

    boolean land() {
        boolean event = nextEvent;
        score++;
        current = Objects.requireNonNull(next);
        currentBlock = look.block();
        next = null;
        nextEvent = false;
        return event;
    }

    void refreshCosmetics() {
        CosmeticStore store = CosmeticStore.get();
        if (store.isRandomPerJump(id, CosmeticCategory.BLOCK)) {
            look = store.selectForRun(id, CosmeticCategory.BLOCK)
                    .map(BlockCosmetic.class::cast).map(RunLook::of).orElse(look);
        }
        if (store.isRandomPerJump(id, CosmeticCategory.SOUND)) {
            landingSound = store.selectForRun(id, CosmeticCategory.SOUND)
                    .map(SoundCosmetic.class::cast).map(SoundCosmetic::sound).orElse(SoundCosmetic.DEFAULT);
        }
    }

    void setNext(@NotNull Cell next, @NotNull HopDifficulty difficulty, boolean event) {
        this.next = next;
        this.nextDifficulty = difficulty;
        this.nextEvent = event;
    }

    void recordEvent(int applied) {
        eventBlocks++;
        eventPoints += applied;
    }

    void setLeg(@NotNull HopSteering.Leg leg) {
        this.leg = leg;
    }
}
