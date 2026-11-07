package gg.itzkatze.katze.bingo.match;

import gg.itzkatze.katze.results.SavedBattle;
import gg.itzkatze.katze.util.JsonFiles;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class BingoResultArchive {
    private final Path file;
    private final Predicate<SavedBattle> writer;
    private final Map<UUID, SavedBattle> pending = new LinkedHashMap<>();
    private record Pending(List<SavedBattle> results) {}

    public BingoResultArchive(Path file, Predicate<SavedBattle> writer) {
        this.file = file;
        this.writer = writer;
        try {
            Pending saved = JsonFiles.read(file, Pending.class);
            if (saved != null) for (SavedBattle battle : saved.results()) pending.put(battle.id(), battle);
        } catch (IOException exception) { throw new UncheckedIOException(exception); }
    }

    public void submit(SavedBattle battle) {
        if (!writer.test(battle)) {
            pending.put(battle.id(), battle);
            save();
        } else if (pending.remove(battle.id()) != null) save();
    }

    public void retry() {
        boolean changed = false;
        for (SavedBattle battle : List.copyOf(pending.values())) if (writer.test(battle)) {
            pending.remove(battle.id());
            changed = true;
        }
        if (changed) save();
    }

    private void save() {
        try { JsonFiles.write(file, new Pending(List.copyOf(pending.values()))); }
        catch (IOException exception) { throw new UncheckedIOException(exception); }
    }
}
