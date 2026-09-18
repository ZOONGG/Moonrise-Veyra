package dev.veyra.weave.mixins;

import dev.veyra.weave.events.MoveEvent;
import net.weavemc.api.event.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.entity.EntityPlayerSP.class)
public class MixinEntityPlayerSP {
    @Inject(method = "onLivingUpdate", at = @At("HEAD"))
    public void onLivingUpdate(CallbackInfo info) {
        // Preserve the event used by Strafe without mutating lastReportedYaw.
        // The previous hook added 45 degrees to the packet-reporting yaw and
        // never restored it, even though the calculated X/Z values were unused.
        EventBus.postEvent(new MoveEvent(0.0D, 0.0D, 0.0D));
    }
}
