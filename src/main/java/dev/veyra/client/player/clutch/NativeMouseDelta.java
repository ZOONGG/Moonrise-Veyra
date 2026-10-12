package dev.veyra.client.player.clutch;

/** Inverse conversion to integer mouse counts, matching Minecraft 1.8.9 exactly.
 * The camera still performs the actual sensitivity scaling, inversion and pitch clamping.
 * Lunar rewrites the camera call, so its scaled local cannot be captured reliably.
 */
public final class NativeMouseDelta {
    private NativeMouseDelta() {}

    public static float sensitivityGain(float sensitivity) {
        // EntityRenderer.updateCameraAndRender in 1.8.9, with the same float operations.
        float base = sensitivity * 0.6F + 0.2F;
        return base * base * base * 8.0F;
    }

    public static int upwardCounts(float pitch, float target, float gain, boolean inverted) {
        if (!Float.isFinite(pitch) || !Float.isFinite(target) || !Float.isFinite(gain) || gain <= 0)
            return 0;
        // Entity.setAngles subtracts scaled mouse Y * 0.15D from pitch.
        int counts = (int) Math.ceil((pitch - target) / (gain * 0.15D));
        return inverted ? -counts : counts;
    }

    /** Reach vanilla's upper pitch clamp, including a count for float rounding. */
    public static int fullUpwardCounts(float pitch, float sensitivity, boolean inverted) {
        if (pitch <= -90) return 0;
        int counts = upwardCounts(pitch, -90, sensitivityGain(sensitivity), inverted);
        return counts + (inverted ? -1 : 1);
    }
}
