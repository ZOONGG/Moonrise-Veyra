package dev.veyra.client.player.movement;

/** Speed remains a double until Minecraft's float clock boundary. */
public final class TimerSpeedPolicy {
    public static double normalize(double speed) {
        return Double.isFinite(speed) ? Math.max(0.0D, Math.min(2.5D, speed)) : 1.0D;
    }

    public static Timing resolve(double configuredSpeed) {
        return new Timing((float) normalize(configuredSpeed), 1.0D);
    }

    private TimerSpeedPolicy() { }

    public record Timing(float clientClockSpeed, double horizontalMovementScale) { }
}
