package dev.veyra.client.player.movement;

import java.util.ArrayList;
import java.util.List;

/** Main-thread controller. Overrides never mutate the manual module state. */
public final class TimerRuntimeController {
    private boolean enabled;
    private double manualSpeed = 1.0D;
    private final List<Override> overrides = new ArrayList<>();

    public void setManual(boolean enabled, double speed) {
        this.enabled = enabled;
        manualSpeed = TimerSpeedPolicy.normalize(speed);
    }

    public boolean isActive() { return enabled || !overrides.isEmpty(); }
    public boolean hasOverrides() { return !overrides.isEmpty(); }

    public double speed() {
        return overrides.isEmpty() ? (enabled ? manualSpeed : 1.0D)
                : overrides.get(overrides.size() - 1).speed;
    }

    public Override override(double speed) {
        Override lease = new Override(TimerSpeedPolicy.normalize(speed));
        overrides.add(lease);
        return lease;
    }

    /** Closing is idempotent; nested overrides can also be released out of order. */
    public final class Override implements AutoCloseable {
        private final double speed;
        private Override(double speed) { this.speed = speed; }
        public void close() { overrides.remove(this); }
    }
}
