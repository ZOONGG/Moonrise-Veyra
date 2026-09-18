package dev.veyra.client.config;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ConfigMigrationTest {
    @Test
    void currentNameWinsOverLegacyAlias() {
        JsonObject modules = new JsonObject();
        JsonObject current = new JsonObject();
        JsonObject legacy = new JsonObject();
        modules.add("AutoBlock", current);
        modules.add("BlockHit", legacy);

        assertSame(current, ConfigMigration.moduleData(modules, "AutoBlock"));
    }

    @Test
    void importsLegacyBlockHitAndPlayerEspNames() {
        JsonObject modules = new JsonObject();
        JsonObject blockHit = new JsonObject();
        JsonObject playerEsp = new JsonObject();
        modules.add("BlockHit", blockHit);
        modules.add("PlayerESP", playerEsp);

        assertSame(blockHit, ConfigMigration.moduleData(modules, "AutoBlock"));
        assertSame(playerEsp, ConfigMigration.moduleData(modules, "ESP"));
    }
}
