package dev.veyra.client.compat;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BwhBowAlertStateTest {
    @Test
    void announcesEachTierOncePerPlayerAndGame() {
        BwhBowAlertState state = new BwhBowAlertState();
        UUID player = UUID.randomUUID();

        assertTrue(state.update(player, "Bow"));
        assertFalse(state.update(player, "Bow"));
        assertTrue(state.update(player, "Power Bow"));
        assertFalse(state.update(player, "Power Bow"));
        assertTrue(state.update(UUID.randomUUID(), "Bow"));
    }

    @Test
    void resetRearmsAlertsForTheNextGame() {
        BwhBowAlertState state = new BwhBowAlertState();
        UUID player = UUID.randomUUID();
        assertTrue(state.update(player, "Punch Bow"));
        state.reset();
        assertTrue(state.update(player, "Punch Bow"));
    }
}
