package dev.veyra.weave.mixins;

import dev.veyra.client.module.modules.player.SafeWalk;
import dev.veyra.client.module.modules.movement.Timer;
import dev.veyra.client.player.bridge.BridgeMovementClamp;
import dev.veyra.weave.events.LivingUpdateEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.weavemc.api.event.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EntityLivingBase.class)
public class EntityLivingBaseMixin {
    @ModifyArgs(
            method = "moveEntityWithHeading",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;moveEntity(DDD)V"))
    private void veyra$adjustLocalMovement(Args args) {
        EntityLivingBase self = (EntityLivingBase) (Object) this;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (self != minecraft.thePlayer) return;

        double requestedX = args.get(0);
        double requestedZ = args.get(2);
        double timerScale = Timer.localHorizontalMovementScale();
        if (timerScale != 1.0D) {
            requestedX *= timerScale;
            requestedZ *= timerScale;
        }

        if (!self.onGround || !SafeWalk.shouldHardClampMovement()) {
            args.set(0, requestedX);
            args.set(2, requestedZ);
            return;
        }

        BridgeMovementClamp.HorizontalMovement safe = BridgeMovementClamp.clamp(
                requestedX,
                requestedZ,
                (offsetX, offsetZ) -> SafeWalk.hasTrustedBridgeSupport(
                        self,
                        self.getEntityBoundingBox().offset(offsetX, -1.0D, offsetZ)
                ));
        args.set(0, safe.x());
        args.set(2, safe.z());
    }

    @Inject(method = "onUpdate", at = @At("HEAD"))
    public void injectLivingUpdateEventPre(final CallbackInfo ci) {
        if ((Object) this == Minecraft.getMinecraft().thePlayer) {
            EventBus.postEvent(new LivingUpdateEvent(LivingUpdateEvent.Type.PRE));
        }
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    public void injectLivingUpdateEventPost(final CallbackInfo ci) {
        if ((Object) this == Minecraft.getMinecraft().thePlayer) {
            EventBus.postEvent(new LivingUpdateEvent(LivingUpdateEvent.Type.POST));
        }
    }
}
