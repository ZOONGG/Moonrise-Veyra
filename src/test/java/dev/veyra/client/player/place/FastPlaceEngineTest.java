package dev.veyra.client.player.place;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FastPlaceEngineTest {
    private final FastPlaceEngine engine = new FastPlaceEngine();

    @Test
    void zeroDelayRestoresTheOriginalFastestMode() {
        assertEquals(0, engine.timerCapTicks(
                context(true), new FastPlaceSettings(0)));
    }

    @Test
    void configuredDelayIsPassedThroughExactly() {
        for (int delay = 0; delay <= 4; delay++) {
            assertEquals(delay, engine.timerCapTicks(
                    context(true), new FastPlaceSettings(delay)));
        }
    }

    @Test
    void ineligibleContextDoesNotTouchTheVanillaTimer() {
        FastPlaceSettings settings = new FastPlaceSettings(0);
        assertEquals(-1, engine.timerCapTicks(context(false), settings));
    }

    @Test
    void settingsClampValuesToTheSupportedDelayRange() {
        assertEquals(0, new FastPlaceSettings(-10).delayTicks());
        assertEquals(4, new FastPlaceSettings(10).delayTicks());
    }

    private static FastPlaceContext context(boolean eligible) {
        return new FastPlaceContext(eligible);
    }
}
