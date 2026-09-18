package dev.veyra.weave.mixins;

import com.mojang.authlib.GameProfile;
import dev.veyra.client.module.modules.player.SafeWalk;
import dev.veyra.weave.events.SlowdownEvent;
import dev.veyra.weave.events.UpdateEvent;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovementInput;
import net.minecraft.world.World;
import net.weavemc.api.event.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Small, opt-in hooks for local-player movement.
 *
 * <p>The old Stormy implementation overwrote all of onLivingUpdate even when
 * every movement module was disabled. That discarded Lunar's own changes to
 * this hot method and made the client disagree with the server during combat.
 * Keep vanilla/Lunar movement intact and alter only the two slowdown constants
 * when an enabled module explicitly cancels the slowdown event.</p>
 */
@Mixin(EntityPlayerSP.class)
public abstract class EntityPlayerSPMixin extends AbstractClientPlayer {
    @Shadow public MovementInput movementInput;

    @Unique
    private boolean veyra$skipVanillaSlowdown;

    protected EntityPlayerSPMixin(World world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @Inject(
            method = "onLivingUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V",
                    shift = At.Shift.AFTER))
    private void veyra$afterMovementInput(CallbackInfo ci) {
        veyra$skipVanillaSlowdown = false;
        if (isUsingItem() && !isRiding()) {
            SlowdownEvent event = new SlowdownEvent();
            EventBus.postEvent(event);
            veyra$skipVanillaSlowdown = event.isCancelled();
        }
    }

    @ModifyConstant(method = "onLivingUpdate", constant = @Constant(floatValue = 0.2F))
    private float veyra$movementSlowdown(float vanillaMultiplier) {
        return veyra$skipVanillaSlowdown ? 1.0F : vanillaMultiplier;
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"), cancellable = true)
    private void veyra$walkingUpdatePre(CallbackInfo ci) {
        UpdateEvent event = new UpdateEvent.Pre(
                posX, posY, posZ, rotationYaw, rotationPitch, onGround);
        EventBus.postEvent(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("RETURN"))
    private void veyra$walkingUpdatePost(CallbackInfo ci) {
        EventBus.postEvent(new UpdateEvent.Post(
                posX, posY, posZ, rotationYaw, rotationPitch, onGround));
    }

    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true, require = 1)
    private void veyra$bridgeMovementSneaking(CallbackInfoReturnable<Boolean> cir) {
        if (SafeWalk.isInsideMovementGuardScope()) {
            SafeWalk.recordMovementGuardInvocation();
            cir.setReturnValue(true);
        }
    }
}
