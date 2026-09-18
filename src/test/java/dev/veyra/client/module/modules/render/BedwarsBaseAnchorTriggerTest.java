package dev.veyra.client.module.modules.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedwarsBaseAnchorTriggerTest {
    @Test
    void recognizesGameStartAndRespawnMessages() {
        assertTrue(BedwarsBaseAnchorTrigger.matches(
                "Protect your bed and destroy the enemy beds."));
        assertTrue(BedwarsBaseAnchorTrigger.matches("You have respawned!"));
        assertFalse(BedwarsBaseAnchorTrigger.matches("Your bed was destroyed!"));
    }
}
