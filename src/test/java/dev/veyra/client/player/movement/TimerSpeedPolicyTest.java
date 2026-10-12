package dev.veyra.client.player.movement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimerSpeedPolicyTest {
    @Test void everyHundredthReachesTheClock() {
        for (int i = 0; i <= 100; i++) {
            double speed = i / 100.0D;
            assertEquals(speed, TimerSpeedPolicy.resolve(speed).clientClockSpeed(), 0.0000001D);
            assertEquals(1.0D, TimerSpeedPolicy.resolve(speed).horizontalMovementScale());
        }
    }
    @Test void requestedSpeedsHaveDistinctTickRates() {
        float previous = 0;
        for (double speed : new double[]{0.01, 0.05, 0.10, 0.50, 1.00}) {
            float clock = TimerSpeedPolicy.resolve(speed).clientClockSpeed();
            assertTrue(clock > previous);
            assertEquals(speed * 20, clock * 20, 0.000001);
            previous = clock;
        }
    }
    @Test void fractionalTicksAccumulateInsteadOfRoundingEachFrame() {
        for (double speed : new double[]{0, 0.01, 0.05, 0.10, 0.50, 1.00}) {
            float partial = 0;
            int ticks = 0;
            float clock = TimerSpeedPolicy.resolve(speed).clientClockSpeed();
            for (int frame = 0; frame < 3600; frame++) {
                partial += (float) ((1.0D / 60) * clock * 20);
                int elapsed = (int) partial;
                partial -= elapsed;
                ticks += elapsed;
            }
            assertEquals(speed * 1200, ticks + partial, 0.002);
        }
    }
    @Test void zeroIsFiniteAndProducesNoElapsedTicks() {
        float clock = TimerSpeedPolicy.resolve(0).clientClockSpeed();
        assertTrue(Float.isFinite(clock));
        assertEquals(0, clock);
        assertEquals(0, (int) (20 * 60 * clock));
    }
    @Test void invalidAndOutOfRangeValuesAreSafe() {
        assertEquals(1, TimerSpeedPolicy.resolve(Double.NaN).clientClockSpeed());
        assertEquals(1, TimerSpeedPolicy.resolve(Double.POSITIVE_INFINITY).clientClockSpeed());
        assertEquals(0, TimerSpeedPolicy.resolve(-1).clientClockSpeed());
        assertEquals(2.5F, TimerSpeedPolicy.resolve(3).clientClockSpeed());
    }
}
