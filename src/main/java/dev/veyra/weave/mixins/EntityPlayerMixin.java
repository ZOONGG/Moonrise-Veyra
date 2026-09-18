package dev.veyra.weave.mixins;

import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.modules.movement.KeepSprint;
import dev.veyra.weave.events.HitSlowDownEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.weavemc.api.event.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the vanilla/Lunar attack implementation intact. The old mixin replaced
 * the complete method for every attack, even while KeepSprint was disabled.
 */
@Mixin(priority = 995, value = EntityPlayer.class)
public abstract class EntityPlayerMixin {
    @Unique private double veyra$motionXBeforeAttack;
    @Unique private double veyra$motionZBeforeAttack;
    @Unique private boolean veyra$wasSprinting;

    @Inject(method = "attackTargetEntityWithCurrentItem", at = @At("HEAD"))
    private void veyra$captureAttackState(Entity targetEntity, CallbackInfo ci) {
        EntityPlayer self = (EntityPlayer) (Object) this;
        veyra$motionXBeforeAttack = self.motionX;
        veyra$motionZBeforeAttack = self.motionZ;
        veyra$wasSprinting = self.isSprinting();
    }

    @Inject(method = "attackTargetEntityWithCurrentItem", at = @At("RETURN"))
    private void veyra$applyKeepSprint(Entity targetEntity, CallbackInfo ci) {
        if (!veyra$wasSprinting || Veyra.moduleManager == null) {
            return;
        }

        Module keepSprint = Veyra.moduleManager.getModuleByClazz(KeepSprint.class);
        if (keepSprint == null || !keepSprint.isEnabled()) {
            return;
        }

        EntityPlayer self = (EntityPlayer) (Object) this;
        // A successful vanilla sprint hit applies the slowdown and stops sprinting.
        // Do nothing for misses/cancelled hits or for another mod's sprint behavior.
        if (self.isSprinting()) {
            return;
        }

        HitSlowDownEvent slowDown = new HitSlowDownEvent(0.6D, true);
        EventBus.postEvent(slowDown);
        self.motionX = veyra$motionXBeforeAttack * slowDown.getSlowDown();
        self.motionZ = veyra$motionZBeforeAttack * slowDown.getSlowDown();
        self.setSprinting(slowDown.isSprinting());
    }
}
