package dev.veyra.client.player.place;

/** Applies the original delay-tick FastPlace behavior without a wall-clock CPS cap. */
public final class FastPlaceEngine {
    public static final int INACTIVE = -1;

    public int timerCapTicks(FastPlaceContext context, FastPlaceSettings settings) {
        if (!context.eligible()) {
            return INACTIVE;
        }
        return settings.delayTicks();
    }
}
