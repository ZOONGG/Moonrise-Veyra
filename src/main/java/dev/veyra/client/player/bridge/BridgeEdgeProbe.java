package dev.veyra.client.player.bridge;

/** Pure geometry policy for the projected support probe used by Bridge Assist. */
public final class BridgeEdgeProbe {
    private static final double MAX_EDGE_OFFSET = 0.30D;
    public static final double STANDARD_MOTION_MARGIN = 0.055D;
    public static final double EMERGENCY_MOTION_MARGIN = 0.018D;
    // A small geometry margin plus velocity look-ahead is enough to catch the
    // final safe tick. Using almost half the player width here made bridging
    // safe but kept sneak active for far too much of every block.
    private static final double SAFE_INSET_AT_ZERO = 0.06D;
    private static final double MINIMUM_SAFE_INSET = 0.025D;

    private BridgeEdgeProbe() {
    }

    /**
     * Edge offset is allowed overhang: zero keeps a small safety margin and
     * larger values progressively move activation closer to the true edge.
     */
    public static double insetForAllowedOverhang(double edgeOffset) {
        double allowedOverhang = Math.max(0.0D, Math.min(MAX_EDGE_OFFSET, edgeOffset));
        double lateActivation = allowedOverhang / MAX_EDGE_OFFSET;
        return MINIMUM_SAFE_INSET
                + (SAFE_INSET_AT_ZERO - MINIMUM_SAFE_INSET) * (1.0D - lateActivation);
    }

    /**
     * Predicts the displacement used by the next ground movement update.
     * Minecraft adds the player's movement attribute to the carried motion;
     * that attribute already contains sprint and speed-potion modifiers.
     * Keeping this uncapped is important: a fixed cap is exactly what made
     * the old probe late at higher speeds.
     */
    public static double predictedAxisDisplacement(
            double carriedMotion,
            double inputDirection,
            double movementAcceleration,
            double safetyMargin
    ) {
        double acceleration = Math.max(0.0D, movementAcceleration);
        double margin = Math.max(0.0D, safetyMargin);
        return carriedMotion + inputDirection * (acceleration + margin);
    }
}
