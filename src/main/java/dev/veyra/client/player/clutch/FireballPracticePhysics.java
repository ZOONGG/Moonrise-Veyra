package dev.veyra.client.player.clutch;
/** Airborne constant-speed initialization under native EntityFireball integration. */
public final class FireballPracticePhysics {
    private FireballPracticePhysics() {}
    public static double amplifyImpulse(double before, double after, double multiplier) {
        return before + (after - before) * multiplier;
    }
    public static double accelerationForSpeed(double speed) {
        return speed * (1.0 / (double) 0.95F - 1.0);
    }
}
