package dev.veyra.client.module.modules.movement;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

/** Removes the vanilla ten-tick jump cooldown without changing movement input. */
public final class NoJumpDelay extends Module {
    public NoJumpDelay() {
        super("NoJumpDelay", ModuleCategory.Movement, 0);
        registerSetting(new DescriptionSetting("Removes the vanilla delay between jumps."));
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        if (mc.thePlayer != null) mc.thePlayer.jumpTicks = 0;
    }
}
