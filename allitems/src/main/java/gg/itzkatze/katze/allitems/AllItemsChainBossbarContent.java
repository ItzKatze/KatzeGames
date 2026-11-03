package gg.itzkatze.katze.allitems;

import gg.itzkatze.katze.allitems.data.AllItemsData;
import gg.itzkatze.katze.bossbar.BossbarContent;
import gg.itzkatze.katze.bossbar.BossbarKind;
import gg.itzkatze.katze.player.KatzePlayer;
import gg.itzkatze.katze.text.TextArg;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AllItemsChainBossbarContent implements BossbarContent {

    private final AllItemsData data;

    public AllItemsChainBossbarContent(AllItemsData data) {
        this.data = data;
    }

    @Override
    public @NotNull BossbarKind getKind() {
        return BossbarKind.RUNNING;
    }

    @Override
    public @Nullable BossBar updateBossbar(@NotNull KatzePlayer player, @Nullable BossBar current) {
        if (!AllItemsSettings.isChainModeEnabled()) {
            return null;
        }
        if (player.getPlayer() == null) {
            return null;
        }
        AllItemsObjective objective = data.getCurrentObjective(player.getPlayer());
        if (objective == null) {
            return null;
        }

        return BossbarContent.update(current,
                Component.translatable("katze.allitems.bossbar.chain_objective",
                        TextArg.component("objective", objective.displayNameComponent()),
                        TextArg.component("type", objective.type().displayName())),
                1.0f,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS
        );
    }
}
