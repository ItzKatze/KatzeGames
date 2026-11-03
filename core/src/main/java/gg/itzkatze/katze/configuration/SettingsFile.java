package gg.itzkatze.katze.configuration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gg.itzkatze.katze.challenges.Challenge;
import gg.itzkatze.katze.challenges.ChallengeRegistry;
import gg.itzkatze.katze.util.JsonFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public final class SettingsFile {

    private static final Logger logger = LoggerFactory.getLogger("KatzeGames/SettingsFile");

    private SettingsFile() {}

    public static void loadSettingsFromFile(File file) throws IOException {
        JsonObject settings = JsonFiles.read(file.toPath(), JsonObject.class);
        if (settings == null) return;
        load(SettingsManager.getGlobalSettingsManager(), section(settings, "GLOBAL"));
        for (Challenge challenge : ChallengeRegistry.getAllChallenges()) {
            load(challenge.getSettingsManager(), section(settings, challenge.getType().name()));
        }
    }

    private static JsonObject section(JsonObject settings, String key) {
        JsonElement section = settings.get(key);
        return section != null && section.isJsonObject() ? section.getAsJsonObject() : settings;
    }

    private static void load(ChallengeSettingsManager manager, JsonObject settings) {
        if (manager == null) return;
        for (Setting setting : manager.getAllSettings()) {
            JsonElement value = settings.get(setting.getId());
            if (value == null) continue;
            try {
                if (!manager.importState(setting, value.getAsString())) {
                    logger.warn("Invalid state for {}, keeping its default", setting.getId());
                }
            } catch (RuntimeException e) {
                logger.warn("Could not load setting {}", setting.getId(), e);
            }
        }
    }

    public static void saveToFile(File file) throws IOException {
        JsonObject settings = new JsonObject();
        save(settings, "GLOBAL", SettingsManager.getGlobalSettingsManager());
        for (Challenge challenge : ChallengeRegistry.getAllChallenges()) {
            save(settings, challenge.getType().name(), challenge.getSettingsManager());
        }
        JsonFiles.write(file.toPath(), settings);
    }

    private static void save(JsonObject settings, String key, ChallengeSettingsManager manager) {
        if (manager == null) return;
        JsonObject section = new JsonObject();
        manager.exportStates().forEach(section::addProperty);
        settings.add(key, section);
    }
}