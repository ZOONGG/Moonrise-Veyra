package dev.veyra.weave.events;

import net.weavemc.api.event.Event;

public class LivingUpdateEvent extends Event {
    public Type type;

    public LivingUpdateEvent(Type type) {
        this.type = type;
    }

    public enum Type {
        PRE,
        POST
    }
}
