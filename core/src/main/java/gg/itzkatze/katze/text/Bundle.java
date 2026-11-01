package gg.itzkatze.katze.text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

record Bundle(@NotNull String name, @Nullable Map<String, String> english, @Nullable Map<String, String> german) {

    boolean isClient() {
        return name.equals("client");
    }
}
