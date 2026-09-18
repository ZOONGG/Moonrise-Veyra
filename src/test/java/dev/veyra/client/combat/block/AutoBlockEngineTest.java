package dev.veyra.client.combat.block;

import org.junit.jupiter.api.Test;

import static dev.veyra.client.combat.block.AutoBlockAction.START_BLOCK;
import static dev.veyra.client.combat.block.AutoBlockAction.START_LAG;
import static dev.veyra.client.combat.block.AutoBlockAction.STOP_BLOCK;
import static dev.veyra.client.combat.block.AutoBlockAction.STOP_LAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoBlockEngineTest {
    private final AutoBlockEngine engine = new AutoBlockEngine();

    @Test
    void blockHitPreservesDelayedShortBlockBehavior() {
        AutoBlockSettings settings = AutoBlockSettings.blockHit(100, 150, 300);

        assertTrue(engine.onAcceptedAttack(context(1_000L), settings).isEmpty());
        assertEquals(AutoBlockPhase.PENDING, engine.phase());
        assertTrue(engine.tick(context(1_099L), settings).isEmpty());
        assertTrue(engine.tick(context(1_100L), settings).contains(START_BLOCK));
        assertTrue(engine.tick(context(1_249L), settings).isEmpty());
        assertTrue(engine.tick(context(1_250L), settings).contains(STOP_BLOCK));
        assertEquals(AutoBlockPhase.IDLE, engine.phase());
    }

    @Test
    void predictiveBlocksBeforeAnAttackWhenThreatIsEligible() {
        AutoBlockSettings settings = AutoBlockSettings.predictive(200, 100);

        assertTrue(engine.tick(context(1_000L), settings).contains(START_BLOCK));
        assertEquals(AutoBlockPhase.BLOCKING, engine.phase());
    }

    @Test
    void configuredConditionsMustAllBeSatisfied() {
        AutoBlockSettings settings = AutoBlockSettings.predictive(200, 100)
                .withRequireLeftMouse(true)
                .withRequireRecentDamage(true);

        assertTrue(engine.tick(context(1_000L), settings).isEmpty());
        assertTrue(engine.tick(context(1_001L).withLeftMouse(true), settings).isEmpty());
        assertTrue(engine.tick(context(1_002L)
                .withLeftMouse(true).withRecentDamage(true), settings).contains(START_BLOCK));
    }

    @Test
    void failedPredictiveChanceWaitsForCooldownInsteadOfRerollingEveryTick() {
        AutoBlockSettings settings = AutoBlockSettings.predictive(200, 100)
                .withBlockChance(50);

        assertTrue(engine.tick(AutoBlockContext.eligible(1_000L, 99, 0), settings).isEmpty());
        assertTrue(engine.tick(AutoBlockContext.eligible(1_001L, 0, 0), settings).isEmpty());
        assertTrue(engine.tick(AutoBlockContext.eligible(1_100L, 0, 0), settings)
                .contains(START_BLOCK));
    }

    @Test
    void blatantTransfersOwnedBlockIntoOwnedLagAndBack() {
        AutoBlockSettings settings = AutoBlockSettings.blatant(50, 150)
                .withLag(100, 200, false, true);

        assertTrue(engine.tick(context(1_000L), settings).contains(START_BLOCK));
        AutoBlockStep unblock = engine.tick(context(1_050L), settings);
        assertTrue(unblock.contains(STOP_BLOCK));
        assertTrue(unblock.contains(START_LAG));
        assertEquals(AutoBlockPhase.LAGGING, engine.phase());

        AutoBlockStep resume = engine.tick(context(1_250L), settings);
        assertTrue(resume.contains(STOP_LAG));
        assertTrue(resume.contains(START_BLOCK));
        assertEquals(AutoBlockPhase.BLOCKING, engine.phase());
    }

    @Test
    void attackFlushesOwnedLagWhenPreventDelayIsEnabled() {
        AutoBlockSettings settings = AutoBlockSettings.blatant(50, 150)
                .withLag(100, 300, true, true);
        engine.tick(context(1_000L), settings);
        engine.tick(context(1_050L), settings);

        AutoBlockStep attack = engine.onAcceptedAttack(context(1_060L), settings);

        assertTrue(attack.contains(STOP_LAG));
        assertTrue(attack.contains(START_BLOCK));
        assertFalse(attack.contains(START_LAG));
    }

    @Test
    void optionalPreAttackFlushRunsBeforeTheAttackPacket() {
        AutoBlockSettings settings = AutoBlockSettings.blatant(50, 150)
                .withLag(100, 300, true, true)
                .withFlushBeforeAttack(true);
        engine.tick(context(1_000L), settings);
        engine.tick(context(1_050L), settings);

        assertTrue(engine.beforeAttack(context(1_060L), settings).contains(STOP_LAG));
    }

    @Test
    void externalPacketOwnerPreventsLagFromStarting() {
        AutoBlockSettings settings = AutoBlockSettings.blatant(50, 100)
                .withLag(100, 200, false, true);
        engine.tick(context(1_000L), settings);

        AutoBlockStep unblock = engine.tick(context(1_050L)
                .withPacketLagAvailable(false), settings);

        assertTrue(unblock.contains(STOP_BLOCK));
        assertFalse(unblock.contains(START_LAG));
        assertFalse(unblock.contains(STOP_LAG));
        assertEquals(AutoBlockPhase.IDLE, engine.phase());
    }

    @Test
    void externalPacketOwnerStopsOnlyAutoBlocksActiveLag() {
        AutoBlockSettings settings = AutoBlockSettings.blatant(50, 100)
                .withLag(100, 200, false, true);
        engine.tick(context(1_000L), settings);
        engine.tick(context(1_050L), settings);

        AutoBlockStep handoff = engine.tick(context(1_060L)
                .withPacketLagAvailable(false), settings);

        assertTrue(handoff.contains(STOP_LAG));
        assertFalse(handoff.contains(START_BLOCK));
        assertEquals(AutoBlockPhase.IDLE, engine.phase());
    }

    @Test
    void blockHitCooldownDoesNotGetRearmedByRejectedAttacks() {
        AutoBlockSettings settings = AutoBlockSettings.blockHit(0, 50, 300);
        engine.onAcceptedAttack(context(1_000L), settings);
        engine.tick(context(1_050L), settings);

        assertTrue(engine.onAcceptedAttack(context(1_100L), settings).isEmpty());
        assertEquals(AutoBlockPhase.IDLE, engine.phase());
        assertTrue(engine.onAcceptedAttack(context(1_350L), settings).contains(START_BLOCK));
    }

    @Test
    void invalidContextReleasesOnlyStateOwnedByAutoBlock() {
        AutoBlockSettings settings = AutoBlockSettings.predictive(200, 100);
        engine.tick(context(1_000L), settings);

        AutoBlockStep release = engine.tick(context(1_010L).withBasicValid(false), settings);

        assertTrue(release.contains(STOP_BLOCK));
        assertFalse(release.contains(STOP_LAG));
        assertEquals(AutoBlockPhase.IDLE, engine.phase());
    }

    @Test
    void physicalUseInputIsNeverClaimedOrReleased() {
        AutoBlockSettings settings = AutoBlockSettings.predictive(200, 100);

        AutoBlockStep step = engine.tick(context(1_000L).withPhysicalUse(true), settings);

        assertTrue(step.isEmpty());
        assertEquals(AutoBlockPhase.IDLE, engine.phase());
    }

    @Test
    void predictiveUnblocksBeforeAttackAndReblocksAfterConfiguredGap() {
        AutoBlockSettings settings = new AutoBlockSettings(
                AutoBlockMode.PREDICTIVE, 50L, 150L, 200L, 100,
                false, false, false, 0, 150L, true, false, true);
        engine.tick(context(1_000L), settings);
        assertTrue(engine.tick(context(1_050L), settings).contains(START_BLOCK));

        assertTrue(engine.beforeAttack(context(1_050L), settings).contains(STOP_BLOCK));
        assertTrue(engine.onAcceptedAttack(context(1_050L), settings).isEmpty());
        assertEquals(AutoBlockPhase.PENDING, engine.phase());
        assertTrue(engine.tick(context(1_099L), settings).isEmpty());
        assertTrue(engine.tick(context(1_100L), settings).contains(START_BLOCK));
    }

    @Test
    void pendingPredictiveBlockIsCancelledBeforeAcceptedAttack() {
        AutoBlockSettings settings = new AutoBlockSettings(
                AutoBlockMode.PREDICTIVE, 100L, 150L, 200L, 100,
                false, false, false, 0, 150L, true, false, true);
        engine.tick(context(1_000L), settings);
        assertEquals(AutoBlockPhase.PENDING, engine.phase());

        assertTrue(engine.beforeAttack(context(1_050L), settings).isEmpty());

        assertEquals(AutoBlockPhase.IDLE, engine.phase());
    }

    private static AutoBlockContext context(long nowMs) {
        return AutoBlockContext.eligible(nowMs, 0, 0);
    }
}
