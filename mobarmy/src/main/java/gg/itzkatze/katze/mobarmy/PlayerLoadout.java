package gg.itzkatze.katze.mobarmy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

final class PlayerLoadout {

    private static final int LEGACY_INVENTORY_SIZE = 41;
    int inventorySize = LEGACY_INVENTORY_SIZE;
    List<String> inventory;
    List<String> cursor;
    List<Effect> effects;
    double health;
    double absorption;
    int food;
    float saturation;
    float exhaustion;
    int level;
    float experience;
    int totalExperience;
    int fireTicks;
    int air;
    int freezeTicks;
    int selectedSlot;
    String world;
    double x;
    double y;
    double z;
    float yaw;
    float pitch;

    static PlayerLoadout capture(Player player) {
        PlayerLoadout data = new PlayerLoadout();
        data.inventorySize = player.getInventory().getSize();
        data.inventory = encode(player.getInventory().getContents());
        data.cursor = encode(new ItemStack[]{player.getItemOnCursor()});
        data.effects = player.getActivePotionEffects().stream().map(effect -> new Effect(
                effect.getType().getKey().toString(), effect.getDuration(), effect.getAmplifier(),
                effect.isAmbient(), effect.hasParticles(), effect.hasIcon()
        )).toList();
        data.health = player.getHealth();
        data.absorption = player.getAbsorptionAmount();
        data.food = player.getFoodLevel();
        data.saturation = player.getSaturation();
        data.exhaustion = player.getExhaustion();
        data.level = player.getLevel();
        data.experience = player.getExp();
        data.totalExperience = player.getTotalExperience();
        data.fireTicks = player.getFireTicks();
        data.air = player.getRemainingAir();
        data.freezeTicks = player.getFreezeTicks();
        data.selectedSlot = player.getInventory().getHeldItemSlot();
        Location location = player.getLocation();
        data.world = location.getWorld().getKey().toString();
        data.x = location.getX();
        data.y = location.getY();
        data.z = location.getZ();
        data.yaw = location.getYaw();
        data.pitch = location.getPitch();
        return data;
    }

    static List<String> encode(ItemStack[] contents) {
        List<String> result = new ArrayList<>(contents.length);
        for (ItemStack item : contents)
            result.add(item == null || item.isEmpty() ? null
                    : Base64.getEncoder().encodeToString(item.serializeAsBytes()));
        return result;
    }

    static ItemStack[] decode(List<String> contents) {
        return contents.stream().map(item -> item == null ? null
                : ItemStack.deserializeBytes(Base64.getDecoder().decode(item))).toArray(ItemStack[]::new);
    }

    void validate() {
        if (inventorySize < LEGACY_INVENTORY_SIZE || inventory == null || inventory.size() != inventorySize) {
            throw new IllegalStateException("Invalid MobArmy player inventory: expected " + inventorySize
                    + " slots, got " + (inventory == null ? "none" : inventory.size()));
        }
        if (cursor == null || cursor.size() != 1 || effects == null
                || world == null || world.isBlank() || !Double.isFinite(health) || health <= 0 || !Double.isFinite(absorption)
                || absorption < 0 || food < 0 || food > 20 || !Float.isFinite(saturation) || !Float.isFinite(exhaustion)
                || level < 0 || !Float.isFinite(experience) || experience < 0 || experience > 1
                || selectedSlot < 0 || selectedSlot > 8 || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalStateException("Incomplete MobArmy player loadout");
        }
        for (Effect effect : effects) if (effect == null || effect.type == null) throw new IllegalStateException("Invalid saved potion effect");
    }

    void restore(Player player, boolean position) {
        player.closeInventory();
        player.setItemOnCursor(null);
        player.getInventory().setContents(decode(inventory));
        player.setItemOnCursor(decode(cursor)[0]);
        player.getInventory().setHeldItemSlot(selectedSlot);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        for (Effect effect : effects) {
            var type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.fromString(effect.type));
            if (type == null) throw new IllegalStateException("Unknown saved potion effect: " + effect.type);
            player.addPotionEffect(new PotionEffect(type, effect.duration, effect.amplifier, effect.ambient, effect.particles, effect.icon));
        }
        player.setHealth(Math.max(1, Math.min(health, player.getAttribute(Attribute.MAX_HEALTH).getValue())));
        player.setAbsorptionAmount(absorption);
        player.setFoodLevel(food);
        player.setSaturation(saturation);
        player.setExhaustion(exhaustion);
        player.setLevel(level);
        player.setExp(experience);
        player.setTotalExperience(totalExperience);
        player.setFireTicks(fireTicks);
        player.setRemainingAir(air);
        player.setFreezeTicks(freezeTicks);
        if (position) {
            var destination = Bukkit.getWorld(NamespacedKey.fromString(world));
            if (destination == null) throw new IllegalStateException("Missing loadout world: " + world);
            player.teleport(new Location(destination, x, y, z, yaw, pitch));
        }
    }

    private record Effect(String type, int duration, int amplifier, boolean ambient, boolean particles, boolean icon) {}

}
