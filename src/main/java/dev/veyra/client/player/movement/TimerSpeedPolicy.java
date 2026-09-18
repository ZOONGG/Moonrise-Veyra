package dev.veyra.client.player.movement;

/** Keeps deep slow-motion controls usable while preserving the requested movement ratio. */
public final class TimerSpeedPolicy {
    public static final double RESPONSIVE_CLOCK_FLOOR = 0.5D;

    public static Timing resolve(double configuredSpeed) {
        double speed = Double.isFinite(configuredSpeed)
                ? Math.max(0.1D, Math.min(2.5D, configuredSpeed))
                : 1.0D;
        double clockSpeed = Math.max(RESPONSIVE_CLOCK_FLOOR, speed);
        double movementScale = speed < RESPONSIVE_CLOCK_FLOOR
                ? speed / RESPONSIVE_CLOCK_FLOOR
                : 1.0D;
        return new Timing((float) clockSpeed, movementScale);
    }

    private TimerSpeedPolicy() {
    }

    public record Timing(float clientClockSpeed, double horizontalMovementScale) {
    }
}
