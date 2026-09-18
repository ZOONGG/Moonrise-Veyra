package dev.veyra.client.player.bridge;

import java.util.function.BiPredicate;

/** Pure implementation of Minecraft's ledge-safe horizontal movement clamp. */
public final class BridgeMovementClamp {
    private static final double STEP = 0.05D;

    private BridgeMovementClamp() {
    }

    /**
     * Reduces the requested movement until the resulting footprint has
     * support. This works on the actual movement arguments, so it cannot be
     * skipped by potion speed, low FPS, input timing, or a missed edge probe.
     */
    public static HorizontalMovement clamp(
            double requestedX,
            double requestedZ,
            BiPredicate<Double, Double> hasSupport
    ) {
        double x = requestedX;
        double z = requestedZ;

        while (x != 0.0D && !hasSupport.test(x, 0.0D)) {
            x = stepTowardsZero(x);
        }
        while (z != 0.0D && !hasSupport.test(0.0D, z)) {
            z = stepTowardsZero(z);
        }
        while (x != 0.0D && z != 0.0D && !hasSupport.test(x, z)) {
            x = stepTowardsZero(x);
            z = stepTowardsZero(z);
        }
        return new HorizontalMovement(x, z);
    }

    private static double stepTowardsZero(double value) {
        if (value > -STEP && value < STEP) return 0.0D;
        return value > 0.0D ? value - STEP : value + STEP;
    }

    public record HorizontalMovement(double x, double z) {
    }
}
