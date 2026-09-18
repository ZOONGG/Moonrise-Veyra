package dev.veyra.weave.mixins;

import dev.veyra.client.module.modules.player.Clutch;
import dev.veyra.weave.events.RenderLabelEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.weavemc.api.event.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RendererLivingEntity.class)
public abstract class RendererLivingEntityMixin<T extends EntityLivingBase> extends Render<T> {
    @Unique private boolean veyra$clutchRotationApplied;
    @Unique private float veyra$savedYaw;
    @Unique private float veyra$savedPreviousYaw;
    @Unique private float veyra$savedPitch;
    @Unique private float veyra$savedPreviousPitch;
    @Unique private float veyra$savedHeadYaw;
    @Unique private float veyra$savedPreviousHeadYaw;
    @Unique private float veyra$savedBodyYaw;
    @Unique private float veyra$savedPreviousBodyYaw;

    public RendererLivingEntityMixin(RenderManager renderManager) {
        super(renderManager);
    }

    @Inject(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V", at = @At("HEAD"), cancellable = true)
    public void onRenderLabel(EntityLivingBase entity, double x, double y, double z, CallbackInfo ci) {
        RenderLabelEvent e = new RenderLabelEvent(entity, x, y, z);
        EventBus.postEvent(e);
        if (e.isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * The first-person camera keeps its real rotation, while the local model
     * in F5 mirrors the rotation that other players receive from the server.
     */
    @Inject(
            method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
            at = @At("HEAD"),
            require = 0)
    private void veyra$applySilentClutchModelRotation(EntityLivingBase entity,
                                                       double x, double y, double z,
                                                       float entityYaw, float partialTicks,
                                                       CallbackInfo ci) {
        // This method is on the renderer used by every living entity. Keep the
        // inactive/first-person path to one cheap static branch so Silent
        // Clutch cannot tax normal gameplay FPS.
        if (!Clutch.hasSilentRenderRotation()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.gameSettings.thirdPersonView == 0
                || entity != minecraft.thePlayer) return;

        veyra$clutchRotationApplied = true;
        veyra$savedYaw = entity.rotationYaw;
        veyra$savedPreviousYaw = entity.prevRotationYaw;
        veyra$savedPitch = entity.rotationPitch;
        veyra$savedPreviousPitch = entity.prevRotationPitch;
        veyra$savedHeadYaw = entity.rotationYawHead;
        veyra$savedPreviousHeadYaw = entity.prevRotationYawHead;
        veyra$savedBodyYaw = entity.renderYawOffset;
        veyra$savedPreviousBodyYaw = entity.prevRenderYawOffset;

        float yaw = Clutch.getSilentRenderYaw();
        float pitch = Clutch.getSilentRenderPitch();
        entity.rotationYaw = yaw;
        entity.prevRotationYaw = yaw;
        entity.rotationPitch = pitch;
        entity.prevRotationPitch = pitch;
        entity.rotationYawHead = yaw;
        entity.prevRotationYawHead = yaw;
        entity.renderYawOffset = yaw;
        entity.prevRenderYawOffset = yaw;
    }

    @Inject(
            method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
            at = @At("RETURN"),
            require = 0)
    private void veyra$restoreSilentClutchModelRotation(EntityLivingBase entity,
                                                         double x, double y, double z,
                                                         float entityYaw, float partialTicks,
                                                         CallbackInfo ci) {
        if (!veyra$clutchRotationApplied) return;
        entity.rotationYaw = veyra$savedYaw;
        entity.prevRotationYaw = veyra$savedPreviousYaw;
        entity.rotationPitch = veyra$savedPitch;
        entity.prevRotationPitch = veyra$savedPreviousPitch;
        entity.rotationYawHead = veyra$savedHeadYaw;
        entity.prevRotationYawHead = veyra$savedPreviousHeadYaw;
        entity.renderYawOffset = veyra$savedBodyYaw;
        entity.prevRenderYawOffset = veyra$savedPreviousBodyYaw;
        veyra$clutchRotationApplied = false;
    }
}
