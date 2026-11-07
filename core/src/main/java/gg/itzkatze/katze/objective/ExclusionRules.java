package gg.itzkatze.katze.objective;

import gg.itzkatze.katze.data.KatzeAdvancement;
import gg.itzkatze.katze.data.KatzeDamageType;
import gg.itzkatze.katze.data.KatzeStructure;
import gg.itzkatze.katze.data.ObtainableObjectives;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

public final class ExclusionRules {

    private final boolean unobtainable;
    private final Set<Material> items;
    private final List<Predicate<String>> itemNameRules;
    private final Set<EntityType> mobs;
    private final Set<String> biomeKeys;
    private final Set<KatzeStructure> structures;
    private final Set<KatzeAdvancement> advancements;
    private final Set<String> deaths;

    private ExclusionRules(@NotNull Builder builder) {
        this.unobtainable = builder.unobtainable;
        this.items = builder.items;
        this.itemNameRules = builder.itemNameRules;
        this.mobs = builder.mobs;
        this.biomeKeys = builder.biomeKeys;
        this.structures = builder.structures;
        this.advancements = builder.advancements;
        this.deaths = builder.deaths;
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public boolean excludesItem(@NotNull Material material) {
        return (unobtainable && !ObtainableObjectives.isItem(material))
                || items.contains(material) || itemNameRules.stream().anyMatch(rule -> rule.test(material.name()));
    }

    public boolean excludesInventoryItem(@NotNull Material material) {
        return (unobtainable && !ObtainableObjectives.isInventoryItem(material))
                || items.contains(material) || itemNameRules.stream().anyMatch(rule -> rule.test(material.name()));
    }

    public boolean excludesMob(@NotNull EntityType type) {
        return (unobtainable && !ObtainableObjectives.isMob(type)) || mobs.contains(type);
    }

    public boolean excludesBiome(@NotNull String key) {
        return biomeKeys.contains(key.toLowerCase(Locale.ROOT));
    }

    public boolean excludesStructure(@NotNull KatzeStructure structure) {
        return structures.contains(structure);
    }

    public boolean excludesAdvancement(@NotNull KatzeAdvancement advancement) {
        return advancements.contains(advancement);
    }

    public boolean excludesDeath(@NotNull KatzeDamageType death) {
        return deaths.contains(death.name());
    }

    public static final class Builder {

        private boolean unobtainable;
        private final Set<Material> items = EnumSet.noneOf(Material.class);
        private final List<Predicate<String>> itemNameRules = new ArrayList<>();
        private final Set<EntityType> mobs = EnumSet.noneOf(EntityType.class);
        private final Set<String> biomeKeys = new HashSet<>();
        private final Set<KatzeStructure> structures = EnumSet.noneOf(KatzeStructure.class);
        private final Set<KatzeAdvancement> advancements = EnumSet.noneOf(KatzeAdvancement.class);
        private final Set<String> deaths = new HashSet<>();

        private Builder() {
        }

        public @NotNull Builder unobtainable() {
            unobtainable = true;
            return this;
        }

        public @NotNull Builder items(@NotNull Material... materials) {
            items.addAll(Arrays.asList(materials));
            return this;
        }

        public @NotNull Builder itemsNamed(@NotNull Predicate<String> nameRule) {
            itemNameRules.add(nameRule);
            return this;
        }

        public @NotNull Builder mobs(@NotNull EntityType... types) {
            mobs.addAll(Arrays.asList(types));
            return this;
        }

        public @NotNull Builder biomes(@NotNull String... keys) {
            for (String key : keys) {
                biomeKeys.add(key.toLowerCase(Locale.ROOT));
            }
            return this;
        }

        public @NotNull Builder structures(@NotNull KatzeStructure... values) {
            structures.addAll(Arrays.asList(values));
            return this;
        }

        public @NotNull Builder advancements(@NotNull KatzeAdvancement... values) {
            advancements.addAll(Arrays.asList(values));
            return this;
        }

        public @NotNull Builder deaths(@NotNull KatzeDamageType... values) {
            for (KatzeDamageType value : values) deaths.add(value.name());
            return this;
        }

        public @NotNull Builder deathsNamed(@NotNull String... values) {
            deaths.addAll(Arrays.asList(values));
            return this;
        }

        public @NotNull ExclusionRules build() {
            return new ExclusionRules(this);
        }
    }
}
