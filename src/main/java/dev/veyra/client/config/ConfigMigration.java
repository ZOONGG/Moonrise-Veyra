package dev.veyra.client.config;

import com.google.gson.JsonObject;

import java.util.Map;

/** Non-destructive aliases for module names used by older Veyra builds. */
public final class ConfigMigration {
    private static final Map<String, String> LEGACY_MODULE_NAMES = Map.of(
            "AutoBlock", "BlockHit",
            "ESP", "PlayerESP"
    );

    public static JsonObject moduleData(JsonObject modules, String currentName) {
        if (modules == null || currentName == null) return null;
        if (modules.has(currentName) && modules.get(currentName).isJsonObject()) {
            return modules.getAsJsonObject(currentName);
        }
        String legacyName = LEGACY_MODULE_NAMES.get(currentName);
        return legacyName != null && modules.has(legacyName)
                && modules.get(legacyName).isJsonObject()
                ? modules.getAsJsonObject(legacyName) : null;
    }

    private ConfigMigration() {
    }
}
