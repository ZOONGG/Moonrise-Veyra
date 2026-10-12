package dev.veyra.client.player.movement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FreezeRecoveryControllerTest {
    @Test void resumesAfterConfiguredDelay() {
        var recovery = new FreezeRecoveryController();
        recovery.arm(); recovery.corrected(true, 100, 100_000_000L);
        recovery.advance(100_000_099L);
        assertTrue(recovery.blocksFreeze());
        assertFalse(recovery.shouldDisable());
        recovery.advance(100_000_100L);
        assertFalse(recovery.blocksFreeze());
    }
    @Test void manualDisableCancelsPendingResume() {
        var recovery = new FreezeRecoveryController();
        recovery.arm(); recovery.corrected(true, 0, 100_000_000L); recovery.disarm();
        recovery.advance(5_000_000_000L);
        assertTrue(recovery.blocksFreeze());
        recovery.arm(); assertFalse(recovery.blocksFreeze());
    }
    @Test void stopModeNeverRearmsItself() {
        var recovery = new FreezeRecoveryController();
        recovery.arm(); recovery.corrected(false, 0, 100_000_000L);
        recovery.advance(5_000_000_000L);
        assertTrue(recovery.shouldDisable());
        recovery.corrected(true, 6_000_000_000L, 100_000_000L);
        recovery.advance(8_000_000_000L);
        assertTrue(recovery.shouldDisable());
    }
    @Test void furtherCorrectionsRestartQuietPeriod() {
        var recovery = new FreezeRecoveryController();
        recovery.arm(); recovery.corrected(true, 0, 100_000_000L);
        recovery.corrected(true, 800_000_000L, 100_000_000L);
        recovery.advance(899_999_999L); assertTrue(recovery.blocksFreeze());
        recovery.advance(900_000_000L); assertFalse(recovery.blocksFreeze());
    }
}
