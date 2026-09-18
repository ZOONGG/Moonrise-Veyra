package dev.veyra.weave.events;

import net.weavemc.api.event.Event;

public class HitSlowDownEvent extends Event {
    public double slowDown = 0.6D;
    public boolean sprinting = false;

    public HitSlowDownEvent(double v, boolean b) {
        this.slowDown = v;
        this.sprinting = b;
    }

    public double getSlowDown() {
        return slowDown;
    }

    public void setSlowDown(double slowDown) {
        this.slowDown = slowDown;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }
}
