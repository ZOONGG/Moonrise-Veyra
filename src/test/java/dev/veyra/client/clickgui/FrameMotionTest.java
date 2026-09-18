package dev.veyra.client.clickgui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameMotionTest {
    @Test
    void exponentialMotionIsIndependentFromRenderRate() {
        double atThirty = simulate(30);
        double atOneTwenty = simulate(120);
        double atTwoForty = simulate(240);

        assertEquals(atThirty, atOneTwenty, 0.0001D);
        assertEquals(atOneTwenty, atTwoForty, 0.0001D);
        assertTrue(atOneTwenty > 0.999D);
    }

    @Test
    void motionDoesNotOvershootAndSnapsNearTarget() {
        assertEquals(1.0F, FrameMotion.expApproach(0.9999F, 1.0F, 18.0F, 1.0F / 120.0F));
        assertTrue(FrameMotion.expApproach(0.0F, 1.0F, 18.0F, 0.5F) <= 1.0F);
    }

    private static double simulate(int framesPerSecond) {
        double value = 0.0D;
        for (int frame = 0; frame < framesPerSecond; frame++) {
            value = FrameMotion.expApproach(value, 1.0D, 9.0D, 1.0D / framesPerSecond);
        }
        return value;
    }
}
