package dev.veyra.client.combat.block;

public record AutoBlockSettings(
        AutoBlockMode mode,
        long startDelayMs,
        long holdDurationMs,
        long cooldownMs,
        int blockChance,
        boolean requireLeftMouse,
        boolean requireRightMouse,
        boolean requireRecentDamage,
        int lagChance,
        long lagDurationMs,
        boolean preventDelayingAttacks,
        boolean blockAgainImmediately,
        boolean flushBeforeAttack
) {
    public AutoBlockSettings {
        if (mode == null) throw new IllegalArgumentException("mode must not be null");
        startDelayMs = Math.max(0L, startDelayMs);
        holdDurationMs = Math.max(1L, holdDurationMs);
        cooldownMs = Math.max(0L, cooldownMs);
        blockChance = clampChance(blockChance);
        lagChance = clampChance(lagChance);
        lagDurationMs = Math.max(1L, lagDurationMs);
    }

    public static AutoBlockSettings blockHit(long delayMs, long holdMs, long cooldownMs) {
        return defaults(AutoBlockMode.BLOCK_HIT, delayMs, holdMs, cooldownMs);
    }

    public static AutoBlockSettings predictive(long holdMs, long cooldownMs) {
        return defaults(AutoBlockMode.PREDICTIVE, 0L, holdMs, cooldownMs);
    }

    public static AutoBlockSettings blatant(long holdMs, long cooldownMs) {
        return defaults(AutoBlockMode.BLATANT, 0L, holdMs, cooldownMs);
    }

    private static AutoBlockSettings defaults(AutoBlockMode mode, long delayMs,
                                              long holdMs, long cooldownMs) {
        return new AutoBlockSettings(mode, delayMs, holdMs, cooldownMs, 100,
                false, false, false, 0, 150L, true, false, true);
    }

    public AutoBlockSettings withRequireLeftMouse(boolean value) {
        return copy(value, requireRightMouse, requireRecentDamage,
                lagChance, lagDurationMs, preventDelayingAttacks, blockAgainImmediately);
    }

    public AutoBlockSettings withBlockChance(int value) {
        return new AutoBlockSettings(mode, startDelayMs, holdDurationMs, cooldownMs,
                value, requireLeftMouse, requireRightMouse, requireRecentDamage,
                lagChance, lagDurationMs, preventDelayingAttacks, blockAgainImmediately,
                flushBeforeAttack);
    }

    public AutoBlockSettings withRequireRightMouse(boolean value) {
        return copy(requireLeftMouse, value, requireRecentDamage,
                lagChance, lagDurationMs, preventDelayingAttacks, blockAgainImmediately);
    }

    public AutoBlockSettings withRequireRecentDamage(boolean value) {
        return copy(requireLeftMouse, requireRightMouse, value,
                lagChance, lagDurationMs, preventDelayingAttacks, blockAgainImmediately);
    }

    public AutoBlockSettings withLag(int chance, long durationMs,
                                     boolean preventAttackDelay, boolean blockAgain) {
        return copy(requireLeftMouse, requireRightMouse, requireRecentDamage,
                chance, durationMs, preventAttackDelay, blockAgain);
    }

    public AutoBlockSettings withFlushBeforeAttack(boolean value) {
        return new AutoBlockSettings(mode, startDelayMs, holdDurationMs, cooldownMs,
                blockChance, requireLeftMouse, requireRightMouse, requireRecentDamage,
                lagChance, lagDurationMs, preventDelayingAttacks,
                blockAgainImmediately, value);
    }

    private AutoBlockSettings copy(boolean left, boolean right, boolean damaged,
                                   int newLagChance, long newLagDuration,
                                   boolean preventAttackDelay, boolean blockAgain) {
        return new AutoBlockSettings(mode, startDelayMs, holdDurationMs, cooldownMs,
                blockChance, left, right, damaged, newLagChance, newLagDuration,
                preventAttackDelay, blockAgain, flushBeforeAttack);
    }

    private static int clampChance(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
