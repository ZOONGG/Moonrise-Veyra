package dev.veyra.weave.events;

import net.weavemc.api.event.Event;

/** Fired immediately before Minecraft recalculates the crosshair ray trace. */
public final class AimUpdateEvent extends Event {
    private final float partialTicks;

    public AimUpdateEvent(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
