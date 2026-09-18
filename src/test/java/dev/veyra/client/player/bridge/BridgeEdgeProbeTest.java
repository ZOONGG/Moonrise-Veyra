package dev.veyra.client.player.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeEdgeProbeTest {
    @Test
    void zeroOffsetKeepsOnlyASmallSafetyMargin() {
        assertEquals(0.06D, BridgeEdgeProbe.insetForAllowedOverhang(0.0D), 0.0001D);
    }

    @Test
    void largerOffsetAllowsLaterSneaking() {
        double safeInset = BridgeEdgeProbe.insetForAllowedOverhang(0.0D);
        double fastInset = BridgeEdgeProbe.insetForAllowedOverhang(0.20D);

        assertTrue(safeInset > fastInset);
        assertEquals(0.025D, BridgeEdgeProbe.insetForAllowedOverhang(0.30D), 0.0001D);
    }

    @Test
    void predictedDisplacementIncludesPotionAwareAccelerationAndMargin() {
        assertEquals(-0.355D, BridgeEdgeProbe.predictedAxisDisplacement(
                -0.14D, -1.0D, 0.16D, 0.055D), 0.0001D);
    }

    @Test
    void predictedDisplacementIsNotCappedAtOldFastMovementLimit() {
        assertEquals(0.555D, BridgeEdgeProbe.predictedAxisDisplacement(
                0.30D, 1.0D, 0.20D, 0.055D), 0.0001D);
    }

    @Test
    void invalidNegativeAccelerationAndMarginCannotReduceSafety() {
        assertEquals(0.10D, BridgeEdgeProbe.predictedAxisDisplacement(
                0.10D, 1.0D, -1.0D, -1.0D), 0.0001D);
    }
}
