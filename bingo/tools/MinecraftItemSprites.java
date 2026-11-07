import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class MinecraftItemSprites {
    private final MinecraftAssets assets;
    private final Map<String, Model> models = new HashMap<>();
    private record Model(Map<String, String> textures, boolean flat) {}
    record Sprite(String texture, BufferedImage image) {}

    MinecraftItemSprites(MinecraftAssets assets) { this.assets = assets; }

    Sprite resolve(String id) throws IOException {
        JsonObject definition = defaultDefinition(assets.item(id).getAsJsonObject("model"), 0);
        if (definition == null || definition.has("transformation")) return null;
        Model model = model(definition.get("model").getAsString(), 0);
        if (!model.flat() || !model.textures().containsKey("layer0") || model.textures().containsKey("layer1") || tinted(definition)) return null;
        String reference = "#layer0";
        for (int depth = 0; reference.startsWith("#"); depth++) {
            if (depth > 32) throw new IOException("Cyclic Minecraft texture reference");
            String resolved = model.textures().get(reference.substring(1));
            if (resolved == null) throw new IOException("Missing Minecraft texture reference: " + reference);
            reference = resolved;
        }
        String texture = MinecraftAssets.location(reference);
        BufferedImage image = assets.staticSprite(texture);
        return image == null ? null : new Sprite(texture + ".png", image);
    }

    private boolean tinted(JsonObject definition) {
        if (!definition.has("tints") || definition.getAsJsonArray("tints").isEmpty()) return false;
        JsonObject tint = definition.getAsJsonArray("tints").get(0).getAsJsonObject();
        return !tint.has("value") || (tint.get("value").getAsInt() & 0xFFFFFF) != 0xFFFFFF;
    }

    private JsonObject defaultDefinition(JsonObject definition, int depth) throws IOException {
        if (depth > 64) throw new IOException("Cyclic Minecraft item definition");
        return switch (definition.get("type").getAsString()) {
            case "minecraft:model" -> definition;
            case "minecraft:condition" -> defaultDefinition(definition.getAsJsonObject("on_false"), depth + 1);
            case "minecraft:select", "minecraft:range_dispatch" -> {
                if (definition.has("fallback")) yield defaultDefinition(definition.getAsJsonObject("fallback"), depth + 1);
                JsonArray choices = definition.getAsJsonArray(definition.has("cases") ? "cases" : "entries");
                yield choices.isEmpty() ? null : defaultDefinition(choices.get(0).getAsJsonObject().getAsJsonObject("model"), depth + 1);
            }
            default -> null;
        };
    }

    private Model model(String id, int depth) throws IOException {
        id = MinecraftAssets.location(id);
        if (models.containsKey(id)) return models.get(id);
        if (depth > 64) throw new IOException("Cyclic Minecraft model parent: " + id);
        if (id.equals("minecraft:builtin/generated")) return new Model(Map.of(), true);
        JsonObject source = assets.model(id);
        Model parent = source.has("parent") ? model(source.get("parent").getAsString(), depth + 1) : new Model(Map.of(), false);
        Map<String, String> textures = new HashMap<>(parent.textures());
        if (source.has("textures")) source.getAsJsonObject("textures").entrySet().forEach(entry -> {
            JsonElement value = entry.getValue();
            textures.put(entry.getKey(), value.isJsonObject() ? value.getAsJsonObject().get("sprite").getAsString() : value.getAsString());
        });
        Model model = new Model(textures, parent.flat());
        models.put(id, model);
        return model;
    }
}
