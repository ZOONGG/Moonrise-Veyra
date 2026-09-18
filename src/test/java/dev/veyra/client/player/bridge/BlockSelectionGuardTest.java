package dev.veyra.client.player.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockSelectionGuardTest {
    @Test
    void manualNonBlockSelectionOverridesAutomaticBlockSelection() {
        BlockSelectionGuard guard = new BlockSelectionGuard();
        assertTrue(guard.allowAutomaticSelection(1, false));
        guard.recordAutomaticSelection(4);
        assertTrue(guard.allowAutomaticSelection(4, true));

        assertFalse(guard.allowAutomaticSelection(1, false));
        assertFalse(guard.allowAutomaticSelection(1, false));
    }

    @Test
    void manuallyReturningToBlocksRearmsAutomation() {
        BlockSelectionGuard guard = new BlockSelectionGuard();
        guard.recordAutomaticSelection(4);
        guard.allowAutomaticSelection(4, true);
        assertFalse(guard.allowAutomaticSelection(1, false));

        assertTrue(guard.allowAutomaticSelection(2, true));
        assertFalse(guard.allowAutomaticSelection(3, false));
    }
}
