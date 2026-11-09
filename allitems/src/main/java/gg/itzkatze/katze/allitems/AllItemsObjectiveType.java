package gg.itzkatze.katze.allitems;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.Locale;

@Getter
public enum AllItemsObjectiveType {
    ITEM(Material.CHEST),
    BIOME(Material.GRASS_BLOCK),
    MOB(Material.ZOMBIE_SPAWN_EGG),
    STRUCTURE(Material.STRUCTURE_BLOCK),
    ADVANCEMENT(Material.BOOK);

    private final Material icon;

    AllItemsObjectiveType(Material icon) {
        this.icon = icon;
    }

    public Component displayName() {
        return Component.translatable("katze.allitems.type." + name().toLowerCase(Locale.ROOT));
    }

}
