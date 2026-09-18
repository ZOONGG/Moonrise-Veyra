package dev.veyra.client.combat.attack;

public record HitSelectDecision(boolean attackAllowed, boolean swingClientSide, Reason reason) {
    public static HitSelectDecision allow() {
        return new HitSelectDecision(true, false, Reason.ALLOWED);
    }

    public static HitSelectDecision cancel(boolean fakeSwing, Reason reason) {
        return new HitSelectDecision(false, fakeSwing, reason);
    }

    public enum Reason {
        ALLOWED,
        TARGET_IMMUNE,
        OPENING_PAUSE,
        RISING,
        MISSED_SWING
    }
}
