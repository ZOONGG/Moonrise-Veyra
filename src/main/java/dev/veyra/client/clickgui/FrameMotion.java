package dev.veyra.client.clickgui;

/** Frame-rate independent motion helpers shared by HUD and menu animations. */
public final class FrameMotion {
    private static final double SNAP_EPSILON = 0.0005D;

    private FrameMotion() {
    }

    public static float expApproach(float current, float target, float response,
                                    float deltaSeconds) {
        return (float) expApproach((double) current, target, response, deltaSeconds);
    }

    public static double expApproach(double current, double target, double response,
                                     double deltaSeconds) {
        if (!Double.isFinite(current) || !Double.isFinite(target)) return target;
        if (response <= 0.0D || deltaSeconds <= 0.0D) return current;
        double alpha = 1.0D - Math.exp(-response * Math.min(deltaSeconds, 0.1D));
        double next = current + (target - current) * alpha;
        return Math.abs(target - next) <= SNAP_EPSILON ? target : next;
    }
}
