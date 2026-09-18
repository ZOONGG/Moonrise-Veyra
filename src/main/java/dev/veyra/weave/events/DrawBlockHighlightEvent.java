package dev.veyra.weave.events;

import net.minecraft.util.BlockPos;
import net.weavemc.api.event.Event;

public class DrawBlockHighlightEvent extends Event {
    public final BlockPos blockPos;

    public DrawBlockHighlightEvent(final BlockPos blockPos) {
        this.blockPos = blockPos;
    }
}
