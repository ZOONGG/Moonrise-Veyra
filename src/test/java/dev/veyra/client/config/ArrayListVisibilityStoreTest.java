package dev.veyra.client.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrayListVisibilityStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesAndLoadsNormalizedUniqueModuleNames() throws Exception {
        ArrayListVisibilityStore store = new ArrayListVisibilityStore(
                temporaryDirectory.resolve("nested/arraylist-modules.txt"));

        store.save(List.of(" AimAssist ", "BRIDGEASSIST", "aimassist", "PlayerESP"));

        assertEquals(List.of("aimassist", "bridgeassist", "esp"), List.copyOf(store.load()));
        assertTrue(store.exists());
    }

    @Test
    void anEmptySelectionRemainsAuthoritative() throws Exception {
        ArrayListVisibilityStore store = new ArrayListVisibilityStore(
                temporaryDirectory.resolve("arraylist-modules.txt"));

        store.save(List.of());

        assertTrue(store.exists());
        assertTrue(store.load().isEmpty());
    }
}
