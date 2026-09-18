package dev.veyra.weave.mixins;

import dev.veyra.client.module.modules.player.SafeWalk;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Enables vanilla's own ledge clamp without changing the player's sneak key. */
@Mixin(Entity.class)
public abstract class BridgeMovementMixin {
    /*
     * Lunar changes the local-variable layout of moveEntity, so modifying the
     * vanilla boolean store is not reliable. Mark only the synchronous window
     * in which moveEntity asks EntityPlayerSP.isSneaking(); that method's mixin
     * then returns true solely for this call and leaves packet/server sneaking
     * untouched.
     */
    @Inject(method = "moveEntity", at = @At("HEAD"), require = 1)
    private void veyra$beginBridgeMovement(
            double x,
            double y,
            double z,
            CallbackInfo ci
    ) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if ((Object) this == minecraft.thePlayer
                && SafeWalk.shouldHardClampMovement()) {
            SafeWalk.enterMovementGuardScope();
        }
    }

    @Inject(method = "moveEntity", at = @At("RETURN"), require = 1)
    private void veyra$endBridgeMovement(
            double x,
            double y,
            double z,
            CallbackInfo ci
    ) {
        if ((Object) this == Minecraft.getMinecraft().thePlayer) {
            SafeWalk.exitMovementGuardScope();
        }
    }
}
