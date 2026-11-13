package gg.itzkatze.katze.mobarmy;

import org.bukkit.NamespacedKey;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

final class MobArmyEquipment {
    private static final NamespacedKey STARTER = new NamespacedKey("katzegames", "mobarmy_starter");

    private MobArmyEquipment() {}

    static void mark(ItemStack item) {
        item.editMeta(meta -> {
            meta.setUnbreakable(true);
            meta.getPersistentDataContainer().set(STARTER, PersistentDataType.BYTE, (byte) 1);
        });
    }

    static boolean isStarter(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(STARTER);
    }

    static void retain(PlayerDeathEvent event) {
        if (event.getKeepInventory()) return;
        for (ItemStack item : event.getEntity().getInventory().getContents()) {
            if (isStarter(item)) event.getItemsToKeep().add(item.clone());
        }
        event.getDrops().removeIf(MobArmyEquipment::isStarter);
    }
}
