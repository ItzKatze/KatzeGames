package gg.itzkatze.katze.bingo.pack;

import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BingoPackAdmission {
    public enum Outcome { WAITING, READY, REJECT, IGNORE }
    private final Map<UUID, Integer> pending = new HashMap<>();
    private final Set<UUID> ready = new HashSet<>();

    public void begin(UUID player, int tick) { ready.remove(player); pending.put(player, tick); }
    public boolean ready(UUID player) { return ready.contains(player); }
    public void forget(UUID player) { ready.remove(player); pending.remove(player); }
    public void clear() { ready.clear(); pending.clear(); }

    public Outcome update(UUID player, Status status) {
        if (!pending.containsKey(player) && !ready.contains(player)) return Outcome.IGNORE;
        return switch (status) {
            case ACCEPTED, DOWNLOADED -> Outcome.WAITING;
            case SUCCESSFULLY_LOADED -> { pending.remove(player); ready.add(player); yield Outcome.READY; }
            default -> { forget(player); yield Outcome.REJECT; }
        };
    }

    public List<UUID> expired(int tick, int timeoutTicks) {
        List<UUID> expired = pending.entrySet().stream().filter(entry -> tick - entry.getValue() >= timeoutTicks).map(Map.Entry::getKey).toList();
        expired.forEach(this::forget);
        return expired;
    }
}
