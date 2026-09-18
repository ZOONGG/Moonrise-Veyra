package dev.veyra.client.notification;

/** Continuous notification visibility with zero velocity at phase boundaries. */
public final class NotificationMotion {
    private NotificationMotion() {
    }

    public static float visibility(long ageMs, long enterMs, long holdMs, long exitMs) {
        long safeAge = Math.max(0L, ageMs);
        long safeEnter = Math.max(1L, enterMs);
        long safeHold = Math.max(0L, holdMs);
        long safeExit = Math.max(1L, exitMs);
        if (safeAge < safeEnter) return smootherStep((float) safeAge / safeEnter);
        if (safeAge <= safeEnter + safeHold) return 1.0F;
        return 1.0F - smootherStep((float) (safeAge - safeEnter - safeHold) / safeExit);
    }

    private static float smootherStep(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return clamped * clamped * clamped
                * (clamped * (clamped * 6.0F - 15.0F) + 10.0F);
    }
}
