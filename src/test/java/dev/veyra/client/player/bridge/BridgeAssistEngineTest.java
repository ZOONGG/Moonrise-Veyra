package dev.veyra.client.player.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeAssistEngineTest {
    private final BridgeAssistEngine engine = new BridgeAssistEngine();
    private final BridgeAssistSettings settings = new BridgeAssistSettings(
            50L, false, true, false);
    private final BridgeAssistSettings adaptiveSettings = new BridgeAssistSettings(
            50L, false, true, true);

    @Test
    void startsSneakingOnlyAfterTheConfiguredEdgeIsReached() {
        assertFalse(engine.tick(context(1_000L, false), settings));
        assertTrue(engine.tick(context(1_001L, true), settings));
        assertTrue(engine.ownsSneak());
    }

    @Test
    void successfulPlacementReleasesAfterUnsneakDelayWithoutDoubleSneaking() {
        assertTrue(engine.tick(context(1_000L, true), settings));
        engine.onBlockPlaced(1_010L, settings);

        assertTrue(engine.tick(context(1_059L, true), settings));
        assertTrue(engine.tick(context(1_060L, true), settings));
        assertTrue(engine.tick(context(1_061L, true), settings));

        assertFalse(engine.tick(context(1_062L, false), settings));
        assertTrue(engine.tick(context(1_063L, true), settings));
    }

    @Test
    void invalidContextReleasesOnlySneakOwnedByBridgeAssist() {
        assertTrue(engine.tick(context(1_000L, true), settings));

        assertFalse(engine.tick(new BridgeAssistContext(
                1_001L, false, true, false, false), settings));
        assertFalse(engine.ownsSneak());
    }

    @Test
    void physicalSneakIsNeverClaimedByBridgeAssist() {
        BridgeAssistContext physicalSneak = new BridgeAssistContext(
                1_000L, true, true, false, true);

        assertFalse(engine.tick(physicalSneak, settings));
        assertFalse(engine.ownsSneak());
    }

    @Test
    void sneakOnJumpIsExplicitAndReleasesWhenSupportReturns() {
        BridgeAssistSettings jumpSettings = new BridgeAssistSettings(0L, true, true);
        BridgeAssistContext jumpingOverAir = new BridgeAssistContext(
                1_000L, true, false, true, false);

        assertTrue(engine.tick(jumpingOverAir, jumpSettings));
        assertFalse(engine.tick(context(1_001L, false), jumpSettings));
    }

    @Test
    void doubleSneakGuardCanBeDisabledForBlatantTiming() {
        BridgeAssistSettings blatant = new BridgeAssistSettings(0L, false, false, false);
        assertTrue(engine.tick(context(1_000L, true), blatant));
        engine.onBlockPlaced(1_001L, blatant);

        assertTrue(engine.tick(context(1_050L, true), blatant));
        assertTrue(engine.tick(context(1_051L, true), blatant));
        assertFalse(engine.tick(context(1_052L, false), blatant));
        assertTrue(engine.tick(context(1_053L, true), blatant));
    }

    @Test
    void zeroUnsneakDelayStillWaitsForConfirmedSupport() {
        BridgeAssistSettings zeroDelay = new BridgeAssistSettings(0L, false, true);
        assertEquals(BridgeAssistSettings.MIN_SAFE_UNSNEAK_DELAY_MS,
                zeroDelay.unsneakDelayMs());

        assertTrue(engine.tick(context(2_000L, true), zeroDelay));
        engine.onBlockPlaced(2_000L, zeroDelay);
        assertTrue(engine.tick(context(2_001L, true), zeroDelay));
        assertTrue(engine.tick(context(2_500L, true), zeroDelay));
        assertFalse(engine.tick(context(2_501L, false), zeroDelay));
    }

    @Test
    void adaptiveRhythmSkipsTheEarlyEdgeAfterOneConfirmedPlacement() {
        assertTrue(engine.tick(context(3_000L, true, true), adaptiveSettings));
        engine.onBlockPlaced(3_001L, adaptiveSettings);
        assertFalse(engine.tick(context(3_051L, false, false), adaptiveSettings));

        assertFalse(engine.tick(context(3_052L, true, false), adaptiveSettings));
        assertFalse(engine.ownsSneak());
    }

    @Test
    void adaptiveRhythmDoesNotSneakAtTheSecondEdge() {
        assertTrue(engine.tick(context(4_000L, true, true), adaptiveSettings));
        engine.onBlockPlaced(4_001L, adaptiveSettings);
        assertFalse(engine.tick(context(4_051L, false, false), adaptiveSettings));

        assertFalse(engine.tick(context(4_052L, true, false), adaptiveSettings));
        assertFalse(engine.tick(context(4_053L, true, true), adaptiveSettings));
        assertFalse(engine.ownsSneak());
    }

    @Test
    void successfulFastPassIsConsumedSoTheFollowingEdgeSneaksNormally() {
        assertTrue(engine.tick(context(5_000L, true, true), adaptiveSettings));
        engine.onBlockPlaced(5_001L, adaptiveSettings);
        assertFalse(engine.tick(context(5_051L, false, false), adaptiveSettings));

        assertFalse(engine.tick(context(5_052L, true, false), adaptiveSettings));
        assertFalse(engine.tick(context(5_053L, false, false), adaptiveSettings));
        assertTrue(engine.tick(context(5_054L, true, false), adaptiveSettings));
    }

    @Test
    void fastPassPlacementAttemptCompletesPassAfterSupportAppears() {
        assertTrue(engine.tick(context(6_000L, true, true), adaptiveSettings));
        engine.onBlockPlaced(6_001L, adaptiveSettings);
        assertFalse(engine.tick(context(6_051L, false, false), adaptiveSettings));

        engine.onFastPassPlacementAttempt();
        assertFalse(engine.tick(context(6_052L, false, false), adaptiveSettings));
        assertTrue(engine.tick(context(6_053L, true, false), adaptiveSettings));
    }

    private static BridgeAssistContext context(long nowMs, boolean edgeReached) {
        return new BridgeAssistContext(nowMs, true, edgeReached, false, false);
    }

    private static BridgeAssistContext context(
            long nowMs,
            boolean edgeReached,
            boolean emergencyEdgeReached
    ) {
        return new BridgeAssistContext(
                nowMs, true, edgeReached, emergencyEdgeReached, false, false);
    }
}
