package gg.itzkatze.katze.allitems;

import gg.itzkatze.katze.data.BiomeSimplified;
import gg.itzkatze.katze.data.KatzeAdvancement;
import gg.itzkatze.katze.data.ObjectiveNames;
import gg.itzkatze.katze.data.StructureSimplified;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Arrays;

public final class AllItemsNames {

    private AllItemsNames() {
    }

    public static Component of(AllItemsObjectiveType type, String key, String fallback) {
        return switch (type) {
            case ITEM -> {
                Material material = Material.matchMaterial(key);
                yield material == null ? Component.text(fallback) : ObjectiveNames.of(material);
            }
            case BIOME -> Arrays.stream(BiomeSimplified.values())
                    .filter(value -> value.name().equals(key))
                    .findFirst()
                    .map(ObjectiveNames::of)
                    .orElseGet(() -> Component.text(fallback));
            case MOB -> Arrays.stream(EntityType.values())
                    .filter(value -> value.name().equals(key))
                    .findFirst()
                    .map(ObjectiveNames::of)
                    .orElseGet(() -> Component.text(fallback));
            case STRUCTURE -> Arrays.stream(StructureSimplified.values())
                    .filter(value -> value.name().equals(key))
                    .findFirst()
                    .map(ObjectiveNames::of)
                    .orElseGet(() -> Component.text(fallback));
            case ADVANCEMENT -> Arrays.stream(KatzeAdvancement.values())
                    .filter(value -> value.name().equals(key))
                    .findFirst()
                    .map(ObjectiveNames::of)
                    .orElseGet(() -> Component.text(fallback));
        };
    }
}
