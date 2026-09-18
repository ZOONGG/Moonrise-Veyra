package dev.veyra.weave.mixins;

import dev.veyra.weave.events.AimUpdateEvent;
import net.minecraft.client.renderer.EntityRenderer;
import net.weavemc.api.event.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps assisted camera rotation and Minecraft's click ray trace in sync. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "getMouseOver", at = @At("HEAD"), require = 0)
    private void veyra$beforeMouseOver(float partialTicks, CallbackInfo ci) {
        EventBus.postEvent(new AimUpdateEvent(partialTicks));
    }
}
