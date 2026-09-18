package dev.veyra.client.combat.attack;

/** Immutable snapshot used by Hit Select. It deliberately has no Minecraft dependencies. */
public record HitSelectContext(
        String targetId,
        boolean validPlayerTarget,
        boolean missedSwing,
        int targetHurtTime,
        long nowMs,
        int chanceRoll,
        int selfHurtTime,
        boolean rising,
        boolean falling,
        boolean criticalPossible
) {
    public HitSelectContext {
        if (chanceRoll < 0 || chanceRoll > 99) {
            throw new IllegalArgumentException("chanceRoll must be between 0 and 99");
        }
    }

    public static HitSelectContext playerTarget(String targetId, int targetHurtTime,
                                                 long nowMs, int chanceRoll) {
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("targetId must not be blank");
        }
        return new HitSelectContext(targetId, true, false, Math.max(0, targetHurtTime),
                nowMs, chanceRoll, 0, false, true, true);
    }

    public static HitSelectContext missedSwing(long nowMs, int chanceRoll) {
        return new HitSelectContext(null, false, true, 0, nowMs, chanceRoll,
                0, false, false, false);
    }

    public HitSelectContext withSelfHurtTime(int value) {
        return new HitSelectContext(targetId, validPlayerTarget, missedSwing, targetHurtTime,
                nowMs, chanceRoll, Math.max(0, value), rising, falling, criticalPossible);
    }

    public HitSelectContext withVerticalState(boolean rising, boolean falling,
                                              boolean criticalPossible) {
        return new HitSelectContext(targetId, validPlayerTarget, missedSwing, targetHurtTime,
                nowMs, chanceRoll, selfHurtTime, rising, falling, criticalPossible);
    }
}
