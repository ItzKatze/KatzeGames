package gg.itzkatze.katze.mobarmy;

import gg.itzkatze.katze.text.Texts;
import gg.itzkatze.katze.util.ItemStackCreator;
import gg.itzkatze.katze.util.KatzeColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Comparator;
import java.util.List;

final class MobArmyTracker {
    private static final NamespacedKey KEY = new NamespacedKey("katzegames", "mobarmy_tracker");
    private final MobArmyGame game;

    MobArmyTracker(MobArmyGame game) { this.game = game; }

    static boolean available(MobArmyMatch.Side side) {
        if (side == null || side.finished || side.preparing) return false;
        int remaining = side.queue.size() + side.live.size();
        return remaining > 0 && remaining <= 5;
    }

    LivingEntity nearest(Player player, MobArmyMatch.Side side) {
        return nearest(player.getLocation(), side.live.keySet().stream().map(Bukkit::getEntity)
                .filter(entity -> entity instanceof LivingEntity).map(entity -> (LivingEntity) entity).toList());
    }

    static LivingEntity nearest(Location origin, List<LivingEntity> mobs) {
        return mobs.stream().filter(entity -> !entity.isDead() && entity.getWorld().equals(origin.getWorld()))
                .min(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(origin))).orElse(null);
    }

    void give(Player player) {
        var side = game.side(player.getUniqueId());
        if (!game.combat() || !game.running() || game.blocked(player) || !available(side)) {
            player.sendMessage(Component.translatable("katze.mobarmy.tracker.unavailable"));
            return;
        }
        LivingEntity target = nearest(player, side);
        if (target == null) { player.sendMessage(Component.translatable("katze.mobarmy.tracker.waiting")); return; }
        for (ItemStack item : player.getInventory().getContents()) if (isTracker(item)) { point(item, target, player); return; }
        ItemStack compass = ItemStackCreator.getStack(Component.translatable("katze.mobarmy.tracker.name").color(KatzeColor.LIME), Material.COMPASS, 1,
                Component.translatable("katze.mobarmy.tracker.hint").color(KatzeColor.LIGHT_GRAY));
        compass.editMeta(meta -> meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1));
        point(compass, target, player);
        if (!player.getInventory().addItem(compass).isEmpty()) player.sendMessage(Component.translatable("katze.mobarmy.tracker.full"));
        game.dirty();
    }

    void update() {
        if (game.match == null) return;
        for (var side : game.match.sides) MobArmyGame.online(side, player -> {
            LivingEntity target = game.combat() && game.running() && available(side) ? nearest(player, side) : null;
            for (ItemStack item : player.getInventory().getContents()) if (isTracker(item)) point(item, target, player);
        });
    }

    void clear(MobArmyMatch.Side side) {
        MobArmyGame.online(side, player -> {
            var inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getSize(); slot++) if (isTracker(inventory.getItem(slot))) inventory.setItem(slot, null);
            if (isTracker(player.getItemOnCursor())) player.setItemOnCursor(null);
        });
        var backpack = game.backpack(side).getInventory();
        for (int slot = 0; slot < backpack.getSize(); slot++) if (isTracker(backpack.getItem(slot))) backpack.setItem(slot, null);
    }

    private static boolean isTracker(ItemStack item) {
        return item != null && item.getType() == Material.COMPASS && item.getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }

    private static void point(ItemStack item, LivingEntity target, Player player) {
        ItemStack translated = Texts.render(item, player);
        if (translated != item) item.setItemMeta(translated.getItemMeta());
        item.editMeta(CompassMeta.class, meta -> {
            meta.setLodestone(target == null ? null : target.getLocation());
            meta.setLodestoneTracked(false);
        });
    }
}
