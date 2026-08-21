package mccfk.DeltaForce.java.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PlayerCharacterStore {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ID_KEY = "id";
    private static final String LEGACY_ID_KEY = "character_id";
    private static Path playerDataDir;

    private PlayerCharacterStore() {
    }

    public static void setPlayerDataDir(Path directory) {
        playerDataDir = directory;
    }

    public static void init() {
        if (playerDataDir == null) {
            return;
        }
        try {
            Files.createDirectories(playerDataDir);
        } catch (IOException exception) {
            LOGGER.error("[DF] Failed to create operator data directory: {}", playerDataDir, exception);
        }
    }

    public static String getSelectedCharacterId(String playerName) {
        if (playerDataDir == null) {
            return null;
        }
        Path file = playerDataDir.resolve(playerName + ".json");
        if (!Files.exists(file)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            String key = object.has(ID_KEY) ? ID_KEY : LEGACY_ID_KEY;
            if (!object.has(key)) {
                return null;
            }
            String id = object.get(key).getAsString();
            return id == null || id.isBlank() ? null : id;
        } catch (IOException | JsonSyntaxException | IllegalStateException exception) {
            LOGGER.warn("[DF] Failed to read operator data: {}", file, exception);
            return null;
        }
    }

    public static boolean setSelectedCharacter(String playerName, String characterId) {
        if (playerDataDir == null) {
            return false;
        }
        try {
            Files.createDirectories(playerDataDir);
            JsonObject object = new JsonObject();
            object.addProperty(ID_KEY, characterId == null ? "" : characterId);
            try (BufferedWriter writer = Files.newBufferedWriter(playerDataDir.resolve(playerName + ".json"))) {
                new GsonBuilder().setPrettyPrinting().create().toJson(object, writer);
            }
            return true;
        } catch (IOException exception) {
            LOGGER.error("[DF] Failed to save operator data for {}", playerName, exception);
            return false;
        }
    }

    public static boolean clearSelectedCharacter(String playerName) {
        return setSelectedCharacter(playerName, "");
    }
}
