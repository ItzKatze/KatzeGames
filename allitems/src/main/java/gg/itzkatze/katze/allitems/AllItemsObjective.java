package gg.itzkatze.katze.allitems;

import net.kyori.adventure.text.Component;

public record AllItemsObjective(AllItemsObjectiveType type, String key, String displayName) {

    public Component displayNameComponent() {
        return AllItemsNames.of(type, key, displayName);
    }

    public boolean matches(AllItemsObjectiveType otherType, String otherKey) {
        return type == otherType && key.equals(otherKey);
    }
}
