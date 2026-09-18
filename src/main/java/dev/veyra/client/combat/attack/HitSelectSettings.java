package dev.veyra.client.combat.attack;

/** Pure settings snapshot so combat timing can be tested independently of the GUI. */
public record HitSelectSettings(
        HitSelectMode mode,
        int combatCancelRate,
        int missedSwingCancelRate,
        long pauseDurationMs,
        boolean waitForFirstHit,
        boolean hitLaterInTrades,
        boolean useServerAttackTime,
        boolean fakeSwing,
        boolean disableDuringKnockback,
        boolean onlyWhileDamaged
) {
    public HitSelectSettings {
        if (mode == null) throw new IllegalArgumentException("mode must not be null");
        combatCancelRate = clampRate(combatCancelRate);
        missedSwingCancelRate = clampRate(missedSwingCancelRate);
        pauseDurationMs = Math.max(0L, pauseDurationMs);
    }

    public static HitSelectSettings burstDefaults() {
        return new HitSelectSettings(HitSelectMode.BURST, 100, 0,
                250L, false, false, true, false, false, false);
    }

    public static HitSelectSettings criticalsDefaults() {
        return new HitSelectSettings(HitSelectMode.CRITICALS, 100, 0,
                250L, false, false, true, false, true, false);
    }

    public HitSelectSettings withCombatCancelRate(int value) {
        return copy(mode, value, missedSwingCancelRate,
                pauseDurationMs, waitForFirstHit, hitLaterInTrades,
                useServerAttackTime, fakeSwing,
                disableDuringKnockback, onlyWhileDamaged);
    }

    public HitSelectSettings withMissedSwingCancelRate(int value) {
        return copy(mode, combatCancelRate, value,
                pauseDurationMs, waitForFirstHit, hitLaterInTrades,
                useServerAttackTime, fakeSwing,
                disableDuringKnockback, onlyWhileDamaged);
    }

    public HitSelectSettings withPauseDurationMs(long value) {
        return copy(mode, combatCancelRate, missedSwingCancelRate,
                value, waitForFirstHit, hitLaterInTrades, useServerAttackTime, fakeSwing,
                disableDuringKnockback, onlyWhileDamaged);
    }

    public HitSelectSettings withWaitForFirstHit(boolean value) {
        return copy(mode, combatCancelRate, missedSwingCancelRate,
                pauseDurationMs, value, hitLaterInTrades, useServerAttackTime, fakeSwing,
                disableDuringKnockback, onlyWhileDamaged);
    }

    public HitSelectSettings withFakeSwing(boolean value) {
        return copy(mode, combatCancelRate, missedSwingCancelRate,
                pauseDurationMs, waitForFirstHit, hitLaterInTrades, useServerAttackTime, value,
                disableDuringKnockback, onlyWhileDamaged);
    }

    public HitSelectSettings withUseServerAttackTime(boolean value) {
        return copy(mode, combatCancelRate, missedSwingCancelRate,
                pauseDurationMs, waitForFirstHit, hitLaterInTrades, value, fakeSwing,
                disableDuringKnockback, onlyWhileDamaged);
    }

    public HitSelectSettings withHitLaterInTrades(boolean value) {
        return copy(mode, combatCancelRate, missedSwingCancelRate,
                pauseDurationMs, waitForFirstHit, value, useServerAttackTime, fakeSwing,
                disableDuringKnockback, onlyWhileDamaged);
    }

    private HitSelectSettings copy(HitSelectMode newMode, int combatRate,
                                   int missRate, long pauseMs, boolean wait,
                                   boolean hitLater, boolean serverTime, boolean fake,
                                   boolean disableKnockback, boolean damagedOnly) {
        return new HitSelectSettings(newMode, combatRate, missRate,
                pauseMs, wait, hitLater, serverTime, fake, disableKnockback, damagedOnly);
    }

    private static int clampRate(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
