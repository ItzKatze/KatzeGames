package gg.itzkatze.katze.mobarmy;

import io.papermc.paper.entity.EntitySerializationFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.IronGolem;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

final class MobCapture {

    private static final Set<String> VOLATILE = Set.of(
            "uuid", "pos", "motion", "rotation", "health", "hurttime",
            "hurtbytimestamp", "deathtime", "falldistance", "fall_distance", "fire", "air", "onground", "on_ground",
            "portal_cooldown", "portalcooldown", "brain", "angryat", "anger_time", "angertime", "last_hurt_by_mob",
            "ticksfrozen", "ticks_frozen", "fall_flying", "leash", "leash_holder", "in_love", "lovecause",
            "absorptionamount", "absorption_amount", "bukkit.worlduuidmost", "bukkit.worlduuidleast"
    );
    private static final Set<String> DETAILS = Set.of(
            "age", "color", "variant", "type", "baby", "isbaby", "sheared", "size",
            "villagerdata", "collarcolor", "cat_type", "wolf_variant", "rabbit_type", "fox_type", "coat_variant"
    );

    private static final NamespacedKey HOSTILE_GOLEM = new NamespacedKey("katzegames", "mobarmy_hostile_golem");

    static boolean hostileGolem(IronGolem golem) {
        return golem.getPersistentDataContainer().has(HOSTILE_GOLEM);
    }

    static boolean eligible(LivingEntity entity) {
        return !(entity instanceof Player) && !(entity instanceof EnderDragon) && entity.getType().isAlive()
                && entity.getType() != EntityType.ARMOR_STAND;
    }

    static MobArmyMatch.CapturedMob capture(LivingEntity entity) {
        MobArmyMatch.CapturedMob mob = new MobArmyMatch.CapturedMob();
        mob.type = entity.getType().name();
        mob.data = serialize(entity);
        var snapshot = entity.createSnapshot();
        if (snapshot == null) throw new IllegalStateException("Mob cannot be captured: " + mob.type);
        String state = snapshot.getAsString();
        mob.variant = digest(canonical(state));
        fields(state).forEach((key, value) -> {
            if (DETAILS.contains(key)) mob.attributes.add(key + ": " + value);
        });
        if (entity.getEquipment() != null) {
            List<ItemStack> equipment = new ArrayList<>(Arrays.asList(entity.getEquipment().getArmorContents()));
            equipment.add(entity.getEquipment().getItemInMainHand());
            equipment.add(entity.getEquipment().getItemInOffHand());
            for (ItemStack item : equipment) {
                if (item == null || item.isEmpty()) continue;
                mob.equipment.add(item.getType().name() + (item.getEnchantments().isEmpty() ? "" : " " + item.getEnchantments()));
            }
        }
        for (Attribute attribute : List.of(Attribute.MAX_HEALTH, Attribute.ATTACK_DAMAGE, Attribute.MOVEMENT_SPEED, Attribute.ARMOR)) {
            var value = entity.getAttribute(attribute);
            if (value != null) mob.attributes.add(attribute.getKey().getKey() + ": " + value.getValue());
        }
        return mob;
    }

    @SuppressWarnings("deprecation")
    static String serialize(LivingEntity entity) {
        return Base64.getEncoder().encodeToString(Bukkit.getUnsafe().serializeEntity(entity, EntitySerializationFlag.FORCE));
    }

    @SuppressWarnings("deprecation")
    static LivingEntity deserialize(String data, World world) {
        if (!(Bukkit.getUnsafe().deserializeEntity(Base64.getDecoder().decode(data), world, false, false) instanceof LivingEntity living)) {
            throw new IllegalStateException("Saved MobArmy entity is not living");
        }
        return living;
    }

    static LivingEntity spawn(String data, Location location, boolean fresh) {
        return spawn(data, location.getWorld(), fresh, entity -> location);
    }

    static LivingEntity spawn(String data, World world, boolean fresh, Function<LivingEntity, Location> position) {
        LivingEntity living = deserialize(data, world);
        if (fresh && living instanceof IronGolem golem) {
            golem.getPersistentDataContainer().set(HOSTILE_GOLEM, PersistentDataType.BYTE, (byte) 1);
            golem.setPlayerCreated(false);
        }
        if (fresh) {
            living.setHealth(living.getAttribute(Attribute.MAX_HEALTH).getValue());
            living.setFireTicks(0);
            living.setFreezeTicks(0);
            living.setRemainingAir(living.getMaximumAir());
            living.setNoDamageTicks(0);
            living.setVelocity(new Vector());
        }
        living.setPersistent(true);
        living.setRemoveWhenFarAway(false);
        if (living instanceof Mob mob) {
            mob.setTarget(null);
            mob.setAware(true);
        }
        living.setAI(true);
        if (!living.spawnAt(position.apply(living))) throw new IllegalStateException("Unable to spawn MobArmy entity");
        return living;
    }

    static String canonical(String nbt) {
        return fields(nbt).toString();
    }

    private static TreeMap<String, String> fields(String nbt) {
        var fields = new TreeMap<String, String>();
        int start = 1;
        int depth = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = 1; i < nbt.length(); i++) {
            char c = nbt.charAt(i);
            if (quote != 0) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') quote = c;
            else if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') {
                if (depth > 0) depth--;
                else addField(fields, nbt.substring(start, i));
            } else if (c == ',' && depth == 0) {
                addField(fields, nbt.substring(start, i));
                start = i + 1;
            }
        }
        return fields;
    }

    private static void addField(TreeMap<String, String> fields, String field) {
        int colon = field.indexOf(':');
        if (colon < 0) return;
        String key = field.substring(0, colon).trim().replace("\"", "").replace("'", "").toLowerCase(Locale.ROOT);
        if (!VOLATILE.contains(key)) fields.put(key, field.substring(colon + 1).trim());
    }

    private static String digest(String value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));} catch (
                NoSuchAlgorithmException exception) {throw new IllegalStateException(exception);}
    }

}
