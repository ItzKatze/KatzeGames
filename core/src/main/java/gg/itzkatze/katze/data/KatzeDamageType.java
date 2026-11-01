package gg.itzkatze.katze.data;

import lombok.Getter;
import org.bukkit.damage.DamageType;

import java.util.List;

@Getter
public enum KatzeDamageType {
    ARROW(List.of(DamageType.ARROW)),
    CACTUS(List.of(DamageType.CACTUS)),
    CAMPFIRE(List.of(DamageType.CAMPFIRE)),
    CRAMMING(List.of(DamageType.CRAMMING)),
    DRAGON_BREATH(List.of(DamageType.DRAGON_BREATH)),
    DROWN(List.of(DamageType.DROWN)),
    ENDER_PEARL(List.of(DamageType.ENDER_PEARL)),
    EXPLOSION(List.of(DamageType.EXPLOSION)),
    FALL(List.of(DamageType.FALL)),
    FALLING_ANVIL(List.of(DamageType.FALLING_ANVIL)),
    FALLING_BLOCK(List.of(DamageType.FALLING_BLOCK)),
    FALLING_STALACTITE(List.of(DamageType.FALLING_STALACTITE)),
    FIREBALL(List.of(DamageType.FIREBALL, DamageType.UNATTRIBUTED_FIREBALL)),
    FIREWORKS(List.of(DamageType.FIREWORKS)),
    FLY_INTO_WALL(List.of(DamageType.FLY_INTO_WALL)),
    FREEZE(List.of(DamageType.FREEZE)),
    HOT_FLOOR(List.of(DamageType.HOT_FLOOR)),
    IN_FIRE(List.of(DamageType.IN_FIRE)),
    IN_WALL(List.of(DamageType.IN_WALL)),
    LAVA(List.of(DamageType.LAVA)),
    LIGHTNING_BOLT(List.of(DamageType.LIGHTNING_BOLT)),
    MAGIC(List.of(DamageType.MAGIC)),
    MOB_ATTACK(List.of(DamageType.MOB_ATTACK)),
    MOB_PROJECTILE(List.of(DamageType.MOB_PROJECTILE)),
    ON_FIRE(List.of(DamageType.ON_FIRE)),
    OUT_OF_WORLD(List.of(DamageType.OUT_OF_WORLD)),
    PLAYER_EXPLOSION(List.of(DamageType.PLAYER_EXPLOSION)),
    SONIC_BOOM(List.of(DamageType.SONIC_BOOM)),
    SPIT(List.of(DamageType.SPIT)),
    STALAGMITE(List.of(DamageType.STALAGMITE)),
    STING(List.of(DamageType.STING)),
    SULFUR_CUBE_HOT(List.of(DamageType.SULFUR_CUBE_HOT)),
    SWEET_BERRY_BUSH(List.of(DamageType.SWEET_BERRY_BUSH)),
    TRIDENT(List.of(DamageType.TRIDENT)),
    WIND_CHARGE(List.of(DamageType.WIND_CHARGE)),
    WITHER(List.of(DamageType.WITHER, DamageType.WITHER_SKULL)),
    ;

    private final List<DamageType> types;

    KatzeDamageType(List<DamageType> types) {
        this.types = types;
    }
}
