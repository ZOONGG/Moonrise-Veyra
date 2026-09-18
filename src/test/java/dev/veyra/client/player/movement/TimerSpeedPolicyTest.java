package dev.veyra.client.player.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimerSpeedPolicyTest {
    @Test
    void deepSlowValuesKeepTheClientClockAtTheResponsiveFloor() {
        assertTiming(0.1D, 0.5F, 0.2D);
        assertTiming(0.2D, 0.5F, 0.4D);
        assertTiming(0.3D, 0.5F, 0.6D);
        assertTiming(0.4D, 0.5F, 0.8D);
    }

    @Test
    void halfSpeedAndFasterUseTheOriginalTimerBehavior() {
        assertTiming(0.5D, 0.5F, 1.0D);
        assertTiming(1.0D, 1.0F, 1.0D);
        assertTiming(2.5D, 2.5F, 1.0D);
    }

    @Test
    void invalidValuesFallBackToNormalSpeed() {
        assertTiming(Double.NaN, 1.0F, 1.0D);
    }

    private static void assertTiming(double configured, float expectedClock,
                                     double expectedMovementScale) {
        TimerSpeedPolicy.Timing timing = TimerSpeedPolicy.resolve(configured);
        assertEquals(expectedClock, timing.clientClockSpeed(), 0.0001F);
        assertEquals(expectedMovementScale, timing.horizontalMovementScale(), 0.0001D);
    }
}
