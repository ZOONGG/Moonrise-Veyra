package dev.veyra.client.combat.block;

import static dev.veyra.client.combat.block.AutoBlockAction.START_BLOCK;
import static dev.veyra.client.combat.block.AutoBlockAction.START_LAG;
import static dev.veyra.client.combat.block.AutoBlockAction.STOP_BLOCK;
import static dev.veyra.client.combat.block.AutoBlockAction.STOP_LAG;

/** Owns AutoBlock's timing without owning Minecraft input or packet queues. */
public final class AutoBlockEngine {
    private AutoBlockPhase phase = AutoBlockPhase.IDLE;
    private long phaseUntilMs;
    private long cooldownUntilMs;
    private long pendingHoldMs;
    private boolean reblockAfterAttack;

    public synchronized AutoBlockStep beforeAttack(AutoBlockContext context,
                                                   AutoBlockSettings settings) {
        if (!context.basicValid()) {
            reblockAfterAttack = false;
            return releaseOwnedState();
        }
        if (phase == AutoBlockPhase.PENDING) {
            phase = AutoBlockPhase.IDLE;
            pendingHoldMs = 0L;
            reblockAfterAttack = false;
            return AutoBlockStep.empty();
        }
        if (phase == AutoBlockPhase.BLOCKING) {
            phase = AutoBlockPhase.IDLE;
            reblockAfterAttack = true;
            return AutoBlockStep.of(STOP_BLOCK);
        }
        if (phase == AutoBlockPhase.LAGGING && settings.flushBeforeAttack()) {
            phase = AutoBlockPhase.IDLE;
            reblockAfterAttack = settings.blockAgainImmediately();
            return AutoBlockStep.of(STOP_LAG);
        }
        reblockAfterAttack = false;
        return AutoBlockStep.empty();
    }

    public synchronized AutoBlockStep onAcceptedAttack(AutoBlockContext context,
                                                       AutoBlockSettings settings) {
        if (!context.basicValid()) return releaseOwnedState();

        if (phase == AutoBlockPhase.LAGGING && settings.preventDelayingAttacks()) {
            phase = AutoBlockPhase.IDLE;
            if (settings.blockAgainImmediately() && activationAllowed(context, settings)) {
                beginBlock(context.nowMs(), settings.holdDurationMs());
                return AutoBlockStep.of(STOP_LAG, START_BLOCK);
            }
            cooldownUntilMs = context.nowMs() + settings.cooldownMs();
            return AutoBlockStep.of(STOP_LAG);
        }

        if (reblockAfterAttack && settings.mode() != AutoBlockMode.BLOCK_HIT) {
            reblockAfterAttack = false;
            if (activationAllowed(context, settings)) {
                return scheduleBlock(context.nowMs(), settings.startDelayMs(),
                        settings.holdDurationMs());
            }
            cooldownUntilMs = context.nowMs() + settings.cooldownMs();
            return AutoBlockStep.empty();
        }

        if (settings.mode() != AutoBlockMode.BLOCK_HIT
                || phase != AutoBlockPhase.IDLE
                || context.nowMs() < cooldownUntilMs
                || !activationAllowed(context, settings)
                || !passes(settings.blockChance(), context.blockChanceRoll())) {
            return AutoBlockStep.empty();
        }
        return scheduleBlock(context.nowMs(), settings.startDelayMs(),
                settings.holdDurationMs());
    }

    public synchronized AutoBlockStep tick(AutoBlockContext context,
                                           AutoBlockSettings settings) {
        if (!context.basicValid()) return releaseOwnedState();

        if (phase == AutoBlockPhase.PENDING) {
            if (!activationAllowed(context, settings)) {
                phase = AutoBlockPhase.IDLE;
                return AutoBlockStep.empty();
            }
            if (context.nowMs() >= phaseUntilMs) {
                beginBlock(context.nowMs(), pendingHoldMs);
                return AutoBlockStep.of(START_BLOCK);
            }
            return AutoBlockStep.empty();
        }

        if (phase == AutoBlockPhase.BLOCKING) {
            boolean predictiveThreatGone = settings.mode() != AutoBlockMode.BLOCK_HIT
                    && !context.threatPresent();
            if (context.nowMs() >= phaseUntilMs || predictiveThreatGone) {
                phase = AutoBlockPhase.IDLE;
                cooldownUntilMs = context.nowMs() + settings.cooldownMs();
                if (!predictiveThreatGone
                        && settings.mode() == AutoBlockMode.BLATANT
                        && context.packetLagAvailable()
                        && passes(settings.lagChance(), context.lagChanceRoll())) {
                    phase = AutoBlockPhase.LAGGING;
                    phaseUntilMs = context.nowMs() + settings.lagDurationMs();
                    return AutoBlockStep.of(STOP_BLOCK, START_LAG);
                }
                return AutoBlockStep.of(STOP_BLOCK);
            }
            return AutoBlockStep.empty();
        }

        if (phase == AutoBlockPhase.LAGGING) {
            if (!context.packetLagAvailable() || context.nowMs() >= phaseUntilMs) {
                phase = AutoBlockPhase.IDLE;
                cooldownUntilMs = context.nowMs() + settings.cooldownMs();
                if (context.packetLagAvailable()
                        && settings.blockAgainImmediately()
                        && activationAllowed(context, settings)) {
                    beginBlock(context.nowMs(), settings.holdDurationMs());
                    return AutoBlockStep.of(STOP_LAG, START_BLOCK);
                }
                return AutoBlockStep.of(STOP_LAG);
            }
            return AutoBlockStep.empty();
        }

        if (settings.mode() != AutoBlockMode.BLOCK_HIT
                && context.nowMs() >= cooldownUntilMs
                && activationAllowed(context, settings)) {
            if (passes(settings.blockChance(), context.blockChanceRoll())) {
                return scheduleBlock(context.nowMs(), settings.startDelayMs(),
                        settings.holdDurationMs());
            }
            cooldownUntilMs = context.nowMs() + settings.cooldownMs();
        }
        return AutoBlockStep.empty();
    }

    private AutoBlockStep scheduleBlock(long nowMs, long delayMs, long holdMs) {
        if (delayMs == 0L) {
            beginBlock(nowMs, holdMs);
            return AutoBlockStep.of(START_BLOCK);
        }
        phase = AutoBlockPhase.PENDING;
        phaseUntilMs = nowMs + delayMs;
        pendingHoldMs = holdMs;
        return AutoBlockStep.empty();
    }

    private void beginBlock(long nowMs, long holdMs) {
        phase = AutoBlockPhase.BLOCKING;
        phaseUntilMs = nowMs + Math.max(1L, holdMs);
        pendingHoldMs = 0L;
    }

    private AutoBlockStep releaseOwnedState() {
        AutoBlockStep release = switch (phase) {
            case BLOCKING -> AutoBlockStep.of(STOP_BLOCK);
            case LAGGING -> AutoBlockStep.of(STOP_LAG);
            default -> AutoBlockStep.empty();
        };
        reset();
        return release;
    }

    private static boolean activationAllowed(AutoBlockContext context,
                                             AutoBlockSettings settings) {
        return context.basicValid()
                && context.canInjectBlock()
                && !context.physicalUseDown()
                && context.threatPresent()
                && (!settings.requireLeftMouse() || context.leftMouseDown())
                && (!settings.requireRightMouse() || context.rightMouseDown())
                && (!settings.requireRecentDamage() || context.recentlyDamaged());
    }

    private static boolean passes(int chance, int roll) {
        return chance > roll;
    }

    public synchronized AutoBlockPhase phase() {
        return phase;
    }

    public synchronized void reset() {
        phase = AutoBlockPhase.IDLE;
        phaseUntilMs = 0L;
        cooldownUntilMs = 0L;
        pendingHoldMs = 0L;
        reblockAfterAttack = false;
    }
}
