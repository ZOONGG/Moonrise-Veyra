package dev.veyra.client.player.movement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimerRuntimeControllerTest {
    @Test void overridesRemainVisibleUntilTheLastLeaseCloses() {
        TimerRuntimeController timer = new TimerRuntimeController();
        timer.setManual(true, 1);
        assertFalse(timer.hasOverrides());
        var first = timer.override(1);
        var second = timer.override(0.03);
        assertTrue(timer.hasOverrides());
        first.close();
        assertTrue(timer.hasOverrides());
        second.close();
        assertFalse(timer.hasOverrides());
        assertEquals(1, timer.speed());
    }
    @Test void manualEnableDisablePreservesPrecision() {
        TimerRuntimeController timer = new TimerRuntimeController();
        assertEquals(1, timer.speed());
        timer.setManual(true, 0.04);
        assertEquals(0.04, timer.speed());
        timer.setManual(false, 0.04);
        assertEquals(1, timer.speed());
    }
    @Test void overrideRestoresEnabledAndDisabledState() {
        for (boolean enabled : new boolean[]{false, true}) {
            TimerRuntimeController timer = new TimerRuntimeController();
            timer.setManual(enabled, 0.10);
            try (var lease = timer.override(0.04)) {
                assertEquals(0.04, timer.speed());
            }
            assertEquals(enabled ? 0.10 : 1, timer.speed());
        }
    }
    @Test void nestedPauseOverridesAndManualChangesAreReversible() {
        TimerRuntimeController timer = new TimerRuntimeController();
        timer.setManual(true, 0.05);
        var first = timer.override(0.04);
        var pause = timer.override(0);
        assertEquals(0, timer.speed());
        first.close();
        first.close();
        assertEquals(0, timer.speed());
        timer.setManual(false, 0.05);
        pause.close();
        assertEquals(1, timer.speed());
    }
}
