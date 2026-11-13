package gg.itzkatze.katze.mobarmy;

import com.google.gson.Gson;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerLoadoutTest {
    @Test void capturedPlayerInventoryPassesValidationWithTheCurrentPaperSlotCount() {
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class<?>[]{PlayerInventory.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getContents" -> new ItemStack[43];
                    case "getSize" -> 43;
                    case "getHeldItemSlot" -> 0;
                    default -> throw new AssertionError(method.getName());
                });
        World world = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getKey")) return NamespacedKey.minecraft("overworld");
                    throw new AssertionError(method.getName());
                });
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getInventory" -> inventory;
                    case "getItemOnCursor" -> null;
                    case "getActivePotionEffects" -> List.of();
                    case "getHealth" -> 20d;
                    case "getAbsorptionAmount" -> 0d;
                    case "getFoodLevel" -> 20;
                    case "getSaturation" -> 5f;
                    case "getExhaustion", "getExp" -> 0f;
                    case "getLevel", "getTotalExperience", "getFireTicks", "getFreezeTicks" -> 0;
                    case "getRemainingAir" -> 300;
                    case "getLocation" -> new Location(world, 0, 80, 0);
                    default -> throw new AssertionError(method.getName());
                });

        PlayerLoadout loadout = PlayerLoadout.capture(player);

        assertEquals(43, loadout.inventory.size());
        assertEquals(43, loadout.inventorySize);
        assertDoesNotThrow(loadout::validate);
        Gson gson = new Gson();
        PlayerLoadout restored = gson.fromJson(gson.toJson(loadout), PlayerLoadout.class);
        assertDoesNotThrow(restored::validate);
        assertEquals(loadout.inventory, restored.inventory);
        assertEquals(loadout.inventorySize, restored.inventorySize);
    }

    @Test void truncatedInventoryStillFailsWithAnActionableError() {
        PlayerLoadout loadout = new PlayerLoadout();
        loadout.inventorySize = 43;
        loadout.inventory = new ArrayList<>(Collections.nCopies(41, null));

        IllegalStateException error = assertThrows(IllegalStateException.class, loadout::validate);

        assertEquals("Invalid MobArmy player inventory: expected 43 slots, got 41", error.getMessage());
    }
}
