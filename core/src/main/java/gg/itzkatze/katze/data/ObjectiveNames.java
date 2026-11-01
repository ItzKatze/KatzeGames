package gg.itzkatze.katze.data;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public final class ObjectiveNames {

    private ObjectiveNames() {
    }

    public static @NotNull Component of(@NotNull Material value) {
        String type = value.isBlock() ? "block" : "item";
        return Component.translatable(type + ".minecraft." + value.getKey().getKey());
    }

    public static @NotNull Component of(@NotNull Biome value) {
        return Component.translatable("biome.minecraft." + value.key().value());
    }

    public static @NotNull Component of(@NotNull EntityType value) {
        return Component.translatable("entity.minecraft." + value.key().value());
    }

    public static @NotNull Component of(@NotNull BiomeSimplified value) {
        return Component.translatable("katze.objective.biome." + value.name().toLowerCase(java.util.Locale.ROOT));
    }

    public static @NotNull Component of(@NotNull MobsSimplified value) {
        return Component.translatable("katze.objective.mob." + value.name().toLowerCase(java.util.Locale.ROOT));
    }

    public static @NotNull Component of(@NotNull DamageTypeSimplified value) {
        return Component.translatable("katze.objective.death." + value.name().toLowerCase(java.util.Locale.ROOT));
    }

    public static @NotNull Component of(@NotNull StructureSimplified value) {
        return Component.translatable("katze.objective.structure." + value.name().toLowerCase(java.util.Locale.ROOT));
    }

    public static @NotNull Component of(@NotNull KatzeAdvancement value) {
        String path = value.key.getKey().replace('/', '.');
        return Component.translatable("advancements." + path + ".title");
    }

    public static @NotNull Component of(@NotNull KatzeStructure value) {
        return Component.translatable("katze.objective.structure_detail." + value.name().toLowerCase(java.util.Locale.ROOT));
    }

    public static @NotNull Component of(@NotNull KatzeDamageType value) {
        return Component.translatable("katze.objective.damage." + value.name().toLowerCase(java.util.Locale.ROOT));
    }
}
