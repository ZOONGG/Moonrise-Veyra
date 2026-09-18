package dev.veyra.client.combat.attack;

import java.util.Objects;

/**
 * Stateful decision engine for Hit Select.
 * Rejected attempts never move a cooldown, so continuous input can use the
 * first tick on which the target becomes damageable.
 */
public final class HitSelectEngine {
    private String currentTargetId;
    private long targetAcquiredAtMs;
    private long lastOwnAttackAtMs = Long.MIN_VALUE;
    private long lastServerDamageAtMs = Long.MIN_VALUE;
    private long pendingServerAttackAtMs = Long.MIN_VALUE;
    private int previousTargetHurtTime;
    private boolean waitingForTradeHit;
    private boolean openingWaitSatisfied;

    public synchronized HitSelectDecision decide(HitSelectContext context, HitSelectSettings settings) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(settings, "settings");

        if (context.missedSwing() || !context.validPlayerTarget()) {
            if (shouldCancel(settings.missedSwingCancelRate(), context.chanceRoll())) {
                return HitSelectDecision.cancel(settings.fakeSwing(),
                        HitSelectDecision.Reason.MISSED_SWING);
            }
            return HitSelectDecision.allow();
        }

        observeTarget(context.targetId(), context.nowMs());
        observeServerDamage(context);
        return settings.mode() == HitSelectMode.CRITICALS
                ? decideCriticals(context, settings)
                : decideBurst(context, settings);
    }

    private HitSelectDecision decideBurst(HitSelectContext context, HitSelectSettings settings) {
        if (context.selfHurtTime() > 0) openingWaitSatisfied = true;
        boolean openingPause = settings.waitForFirstHit()
                && !openingWaitSatisfied
                && context.nowMs() - targetAcquiredAtMs < settings.pauseDurationMs();
        if (openingPause
                && shouldCancel(settings.combatCancelRate(), context.chanceRoll())) {
            return HitSelectDecision.cancel(settings.fakeSwing(),
                    HitSelectDecision.Reason.OPENING_PAUSE);
        }

        if (waitingForTradeHit && (context.selfHurtTime() > 0
                || elapsedSince(lastOwnAttackAtMs, context.nowMs()) >= settings.pauseDurationMs())) {
            waitingForTradeHit = false;
        }
        boolean tradePause = settings.hitLaterInTrades() && waitingForTradeHit;
        boolean targetImmune = tradePause || predictedImmunity(context, settings);
        if (targetImmune
                && shouldCancel(settings.combatCancelRate(), context.chanceRoll())) {
            return HitSelectDecision.cancel(settings.fakeSwing(),
                    HitSelectDecision.Reason.TARGET_IMMUNE);
        }
        return HitSelectDecision.allow();
    }

    private HitSelectDecision decideCriticals(HitSelectContext context,
                                              HitSelectSettings settings) {
        HitSelectDecision burstDecision = decideBurst(context, settings);
        if (!burstDecision.attackAllowed()) return burstDecision;

        if (!context.criticalPossible()
                || (settings.onlyWhileDamaged() && context.selfHurtTime() == 0)
                || (settings.disableDuringKnockback() && context.selfHurtTime() > 0)) {
            return HitSelectDecision.allow();
        }
        if (context.rising()
                && shouldCancel(settings.combatCancelRate(), context.chanceRoll())) {
            return HitSelectDecision.cancel(settings.fakeSwing(),
                    HitSelectDecision.Reason.RISING);
        }
        return HitSelectDecision.allow();
    }

    private void observeTarget(String targetId, long nowMs) {
        if (!Objects.equals(currentTargetId, targetId)) {
            currentTargetId = targetId;
            targetAcquiredAtMs = nowMs;
            lastOwnAttackAtMs = Long.MIN_VALUE;
            lastServerDamageAtMs = Long.MIN_VALUE;
            pendingServerAttackAtMs = Long.MIN_VALUE;
            previousTargetHurtTime = 0;
            waitingForTradeHit = false;
            openingWaitSatisfied = false;
        }
    }

    private void observeServerDamage(HitSelectContext context) {
        if (context.targetHurtTime() > previousTargetHurtTime) {
            lastServerDamageAtMs = context.nowMs();
            pendingServerAttackAtMs = Long.MIN_VALUE;
        }
        previousTargetHurtTime = context.targetHurtTime();
    }

    private boolean predictedImmunity(HitSelectContext context, HitSelectSettings settings) {
        long reference = settings.useServerAttackTime()
                ? mostRecent(lastServerDamageAtMs, pendingServerAttackAtMs)
                : lastOwnAttackAtMs;
        return elapsedSince(reference, context.nowMs()) < settings.pauseDurationMs();
    }

    private static long mostRecent(long first, long second) {
        return Math.max(first, second);
    }

    private static long elapsedSince(long timestamp, long nowMs) {
        if (timestamp == Long.MIN_VALUE) return Long.MAX_VALUE;
        return Math.max(0L, nowMs - timestamp);
    }

    /** Records a real outgoing attack packet; cancelled and fake swings never call this. */
    public synchronized void recordOwnAttack(String targetId, long nowMs,
                                             boolean hitLaterInTrades) {
        if (!Objects.equals(currentTargetId, targetId)) observeTarget(targetId, nowMs);
        lastOwnAttackAtMs = nowMs;
        // Keep the first unconfirmed attempt as the bounded server-time
        // reference. Retrying after that window must not move the reference
        // forward forever when the server rejected or never confirmed it.
        if (pendingServerAttackAtMs == Long.MIN_VALUE) {
            pendingServerAttackAtMs = nowMs;
            waitingForTradeHit = hitLaterInTrades;
        }
    }

    private static boolean shouldCancel(int percent, int chanceRoll) {
        return percent > chanceRoll;
    }

    public synchronized String currentTargetId() {
        return currentTargetId;
    }

    public synchronized void reset() {
        currentTargetId = null;
        targetAcquiredAtMs = 0L;
        lastOwnAttackAtMs = Long.MIN_VALUE;
        lastServerDamageAtMs = Long.MIN_VALUE;
        pendingServerAttackAtMs = Long.MIN_VALUE;
        previousTargetHurtTime = 0;
        waitingForTradeHit = false;
        openingWaitSatisfied = false;
    }
}
