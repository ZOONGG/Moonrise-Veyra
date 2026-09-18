package dev.veyra.client.combat.attack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitSelectEngineTest {
    private final HitSelectEngine engine = new HitSelectEngine();

    @Test
    void continuousRejectedClicksDoNotDelayFirstDamageableTick() {
        HitSelectSettings settings = HitSelectSettings.burstDefaults()
                .withPauseDurationMs(10);

        assertFalse(engine.decide(target("one", 8, 1_000L), settings).attackAllowed());
        assertFalse(engine.decide(target("one", 4, 1_009L), settings).attackAllowed());
        assertTrue(engine.decide(target("one", 0, 1_010L), settings).attackAllowed());
    }

    @Test
    void targetChangeDoesNotInheritPreviousTargetsImmunity() {
        HitSelectSettings settings = HitSelectSettings.burstDefaults()
                .withUseServerAttackTime(false);

        assertTrue(engine.decide(target("one", 0, 1_000L), settings).attackAllowed());
        engine.recordOwnAttack("one", 1_000L, false);
        assertTrue(engine.decide(target("two", 0, 1_001L), settings).attackAllowed());
        assertEquals("two", engine.currentTargetId());
    }

    @Test
    void missedSwingPolicyCannotChangeValidTargetDecision() {
        HitSelectSettings settings = HitSelectSettings.burstDefaults()
                .withMissedSwingCancelRate(100);

        assertFalse(engine.decide(miss(1_000L, 0), settings).attackAllowed());
        assertTrue(engine.decide(target("one", 0, 1_001L), settings).attackAllowed());
    }

    @Test
    void missedSwingsDoNotEraseAnActiveTargetWindow() {
        HitSelectSettings settings = HitSelectSettings.burstDefaults()
                .withUseServerAttackTime(false);

        assertTrue(engine.decide(target("one", 0, 1_000L), settings).attackAllowed());
        engine.recordOwnAttack("one", 1_000L, false);
        assertTrue(engine.decide(miss(1_010L, 0), settings).attackAllowed());
        assertFalse(engine.decide(target("one", 0, 1_020L), settings).attackAllowed());
    }

    @Test
    void fakeSwingAnimatesWithoutSendingAnAttack() {
        HitSelectSettings settings = HitSelectSettings.burstDefaults()
                .withFakeSwing(true);

        HitSelectDecision decision = engine.decide(target("one", 8, 1_000L), settings);

        assertFalse(decision.attackAllowed());
        assertTrue(decision.swingClientSide());
        assertEquals(HitSelectDecision.Reason.TARGET_IMMUNE, decision.reason());
    }

    @Test
    void waitForFirstHitEndsImmediatelyWhenPlayerIsDamaged() {
        HitSelectSettings settings = HitSelectSettings.burstDefaults()
                .withWaitForFirstHit(true)
                .withPauseDurationMs(250);

        assertFalse(engine.decide(target("one", 0, 1_000L), settings).attackAllowed());
        assertFalse(engine.decide(target("one", 0, 1_249L), settings).attackAllowed());
        assertTrue(engine.decide(target("one", 0, 1_250L), settings).attackAllowed());

        engine.reset();
        assertFalse(engine.decide(target("one", 0, 2_000L), settings).attackAllowed());
        assertTrue(engine.decide(target("one", 0, 2_001L).withSelfHurtTime(5), settings).attackAllowed());
        assertTrue(engine.decide(target("one", 0, 2_002L), settings).attackAllowed());
    }

    @Test
    void criticalModeWaitsWhileRisingAndHitsWhileFalling() {
        HitSelectSettings settings = HitSelectSettings.criticalsDefaults();

        assertFalse(engine.decide(target("one", 0, 1_000L).withVerticalState(true, false, true), settings).attackAllowed());
        assertTrue(engine.decide(target("one", 0, 1_001L).withVerticalState(false, true, true), settings).attackAllowed());
    }

    @Test
    void criticalModeBypassesDelayWhenCriticalIsImpossible() {
        HitSelectSettings settings = HitSelectSettings.criticalsDefaults();

        assertTrue(engine.decide(target("one", 0, 1_000L).withVerticalState(true, false, false), settings).attackAllowed());
    }

    @Test
    void cancelRatesAreDeterministicAtTheirBoundaries() {
        HitSelectSettings neverCancel = HitSelectSettings.burstDefaults().withCombatCancelRate(0);
        HitSelectSettings alwaysCancel = HitSelectSettings.burstDefaults().withCombatCancelRate(100);

        assertTrue(engine.decide(target("one", 8, 1_000L), neverCancel).attackAllowed());
        assertFalse(engine.decide(target("one", 8, 1_001L), alwaysCancel).attackAllowed());
    }

    @Test
    void localPredictionStartsOnlyAfterARealOutgoingAttack() {
        HitSelectSettings settings = HitSelectSettings.burstDefaults()
                .withUseServerAttackTime(false)
                .withPauseDurationMs(250);

        assertTrue(engine.decide(target("one", 0, 1_000L), settings).attackAllowed());
        engine.recordOwnAttack("one", 1_000L, false);
        assertFalse(engine.decide(target("one", 0, 1_010L), settings).attackAllowed());
        assertFalse(engine.decide(target("one", 0, 1_249L), settings).attackAllowed());
        assertTrue(engine.decide(target("one", 0, 1_250L), settings).attackAllowed());
    }

    @Test
    void serverTimingStartsFromObservedDamageIncludingNonMeleeDamage() {
        HitSelectSettings settings = HitSelectSettings.burstDefaults()
                .withUseServerAttackTime(true)
                .withPauseDurationMs(200);

        assertTrue(engine.decide(target("one", 0, 1_000L), settings).attackAllowed());
        assertFalse(engine.decide(target("one", 10, 1_050L), settings).attackAllowed());
        assertFalse(engine.decide(target("one", 0, 1_249L), settings).attackAllowed());
        assertTrue(engine.decide(target("one", 0, 1_250L), settings).attackAllowed());
    }

    @Test
    void serverTimingWaitsForConfirmationWithoutFreezingForever() {
        HitSelectSettings settings = HitSelectSettings.burstDefaults()
                .withUseServerAttackTime(true)
                .withPauseDurationMs(200);

        assertTrue(engine.decide(target("one", 0, 1_000L), settings).attackAllowed());
        engine.recordOwnAttack("one", 1_000L, false);
        assertFalse(engine.decide(target("one", 0, 1_050L), settings).attackAllowed());
        assertTrue(engine.decide(target("one", 0, 1_200L), settings).attackAllowed());
    }

    @Test
    void unconfirmedRetryDoesNotRearmAnExpiredServerWindow() {
        HitSelectSettings settings = HitSelectSettings.burstDefaults()
                .withUseServerAttackTime(true)
                .withPauseDurationMs(200);

        assertTrue(engine.decide(target("one", 0, 1_000L), settings).attackAllowed());
        engine.recordOwnAttack("one", 1_000L, false);
        assertTrue(engine.decide(target("one", 0, 1_200L), settings).attackAllowed());

        engine.recordOwnAttack("one", 1_200L, false);

        assertTrue(engine.decide(target("one", 0, 1_201L), settings).attackAllowed());
    }

    @Test
    void serverConfirmationAfterRetryStartsOneFreshDamageWindow() {
        HitSelectSettings settings = HitSelectSettings.burstDefaults()
                .withUseServerAttackTime(true)
                .withPauseDurationMs(200);
        engine.decide(target("one", 0, 1_000L), settings);
        engine.recordOwnAttack("one", 1_000L, false);
        engine.decide(target("one", 0, 1_200L), settings);
        engine.recordOwnAttack("one", 1_200L, false);

        assertFalse(engine.decide(target("one", 10, 1_210L), settings).attackAllowed());
        assertFalse(engine.decide(target("one", 0, 1_409L), settings).attackAllowed());
        assertTrue(engine.decide(target("one", 0, 1_410L), settings).attackAllowed());
    }

    private static HitSelectContext target(String id, int hurtTime, long nowMs) {
        return HitSelectContext.playerTarget(id, hurtTime, nowMs, 0);
    }

    private static HitSelectContext miss(long nowMs, int chanceRoll) {
        return HitSelectContext.missedSwing(nowMs, chanceRoll);
    }
}
