package gg.itzkatze.katze.gui;

import gg.itzkatze.katze.text.Texts;
import lombok.Getter;
import gg.itzkatze.katze.player.KatzePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public abstract class KatzeGUI implements Listener, InventoryHolder {
    public static final Map<UUID, KatzeGUI> GUI_MAP = new ConcurrentHashMap<>();

    protected Component title;
    protected int size;
    protected final List<GUIItem> items;
    private Inventory inventory;

    private final Set<UUID> currentViewers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> refreshingViewers = ConcurrentHashMap.newKeySet();

    private boolean hasFinishedLoading = false;

    public KatzeGUI(String title, int size) {
        this(Component.text(title), size);
    }

    public KatzeGUI(Component title, int size) {
        this.title = title;
        this.size = size;
        this.items = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, size, title);
        }
        return inventory;
    }

    public void set(GUIItem item) {
        synchronized (items) {
            items.removeIf(i -> i.slot == item.slot);
            items.add(item);
        }
    }

    public GUIItem get(int slot) {
        synchronized (items) {
            return items.stream().filter(i -> i.slot == slot).findFirst().orElse(null);
        }
    }

    public void clear() {
        synchronized (items) {
            items.clear();
        }

        if (inventory != null) {
            inventory.clear();
        }
    }

    public int firstEmpty() {
        for (int i = 0; i < size; i++) {
            int finalI = i;
            long found = items.stream().filter(it -> it.slot == finalI).count();
            if (found == 0) return i;
        }
        return -1;
    }

    public void fill(ItemStack item, int from, int to, boolean overwrite) {
        for (int slot = from; slot <= to; slot++) {
            if (!overwrite && get(slot) != null) continue;
            set(new GUIItem(slot) {
                @Override
                public ItemStack getItem(KatzePlayer player) {
                    return item;
                }
            });
        }
    }

    public void fill(ItemStack item) {
        fill(item, 0, size - 1, true);
    }

    public void border(ItemStack item) {
        border(item, 0, size - 1, true);
    }

    public void border(ItemStack item, int corner1, int corner2, boolean overwrite) {
        int width = 9;
        int topLeft = Math.min(corner1, corner2);
        int bottomRight = Math.max(corner1, corner2);
        for (int slot = topLeft; slot <= bottomRight; slot++) {
            int row = slot / width;
            int col = slot % width;

            int topRow = topLeft / width;
            int bottomRow = bottomRight / width;
            int leftCol = topLeft % width;
            int rightCol = bottomRight % width;

            boolean border =
                    row == topRow || row == bottomRow ||
                            col == leftCol || col == rightCol;

            if (border) {
                if (!overwrite && get(slot) != null) continue;
                set(new GUIItem(slot) {
                    @Override
                    public ItemStack getItem(KatzePlayer player) {
                        return item;
                    }
                });
            }
        }
    }

    public void open(KatzePlayer player) {
        Player viewer = player.getPlayer();
        if (viewer == null) return;
        try {
            onOpen(player);
            setItems(player);
            updateInventory(player);
            hasFinishedLoading = true;
        } catch (Exception e) {
            viewer.sendMessage(Component.translatable("katze.gui.open_failed"));
            e.printStackTrace();
            return;
        }

        UUID id = viewer.getUniqueId();
        KatzeGUI previous = GUI_MAP.put(id, this);
        currentViewers.add(id);
        viewer.openInventory(getInventory());
        if (viewer.getOpenInventory().getTopInventory().getHolder() != this) {
            currentViewers.remove(id);
            GUI_MAP.remove(id, this);
            if (previous != null && viewer.getOpenInventory().getTopInventory().getHolder() == previous) {
                GUI_MAP.put(id, previous);
            }
            return;
        }
        afterOpen(player);
    }

    public void close(KatzePlayer player, CloseReason reason) {
        Player viewer = player.getPlayer();
        if (viewer == null) {
            currentViewers.remove(player.getUuid());
            GUI_MAP.remove(player.getUuid(), this);
            return;
        }
        if (viewer.getOpenInventory().getTopInventory().getHolder() != this) return;
        handleClose(new InventoryCloseEvent(viewer.getOpenInventory()), reason);
        if (viewer.getOpenInventory().getTopInventory().getHolder() == this) {
            viewer.closeInventory();
        }
    }

    public void handleClose(InventoryCloseEvent event, CloseReason reason) {
        UUID id = event.getPlayer().getUniqueId();
        if (isRefreshing(id) || !currentViewers.remove(id)) return;
        GUI_MAP.remove(id, this);
        onClose(event, reason);
    }

    public void updateInventory(KatzePlayer player) {
        Player bukkitPlayer = player.getPlayer();
        boolean online = bukkitPlayer != null && bukkitPlayer.isOnline();
        synchronized (items) {
            Inventory inv = getInventory();
            for (GUIItem item : items) {
                ItemStack stack = item.getItem(player);
                inv.setItem(item.slot, online ? Texts.render(stack, bukkitPlayer) : stack);
            }
        }
    }

    public void updateInventoryForPlayer(KatzePlayer player) {
        Player bukkitPlayer = player.getPlayer();
        if (bukkitPlayer.getOpenInventory().getTopInventory().getHolder() == this) {
            updateInventory(player);
        }
    }

    public void refreshLanguage(KatzePlayer player) {
        Player viewer = player.getPlayer();
        if (viewer == null || viewer.getOpenInventory().getTopInventory().getHolder() != this) return;

        updateInventory(player);
        refreshingViewers.add(viewer.getUniqueId());
        try {
            viewer.openInventory(getInventory());
        } finally {
            refreshingViewers.remove(viewer.getUniqueId());
        }
    }

    public boolean isRefreshing(UUID playerId) {
        return refreshingViewers.contains(playerId);
    }

    public abstract void setItems(KatzePlayer player);

    public abstract boolean allowItemMoving();

    public void onOpen(KatzePlayer player) {
    }

    public void afterOpen(KatzePlayer player) {
    }

    public void onClose(InventoryCloseEvent e, CloseReason reason) {
    }

    public enum CloseReason {
        PLAYER_EXITED,
        SERVER_EXITED,
        SIGN_OPENED
    }
}
