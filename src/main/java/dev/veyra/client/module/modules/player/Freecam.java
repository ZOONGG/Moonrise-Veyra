package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.network.play.client.*;
import net.minecraft.util.MathHelper;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;
import net.weavemc.api.event.WorldEvent;

public class Freecam extends Module {
    private static final int FAKE_ENTITY_ID = -13371;

    public static SliderSetting speed;
    public static TickSetting cancelInteractions;
    private EntityOtherPlayerMP body;
    private double startX, startY, startZ;
    private float startYaw, startPitch;

    public Freecam() {
        super("Freecam", ModuleCategory.Player, 0);
        registerSetting(new DescriptionSetting("Detaches the camera while your server position stays still."));
        registerSetting(speed = new SliderSetting("Speed", 0.8, 0.1, 3.0, 0.1));
        registerSetting(cancelInteractions = new TickSetting("Cancel interactions", true));
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            disable();
            return;
        }
        startX = mc.thePlayer.posX;
        startY = mc.thePlayer.posY;
        startZ = mc.thePlayer.posZ;
        startYaw = mc.thePlayer.rotationYaw;
        startPitch = mc.thePlayer.rotationPitch;

        body = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        body.copyLocationAndAnglesFrom(mc.thePlayer);
        body.rotationYawHead = mc.thePlayer.rotationYawHead;
        mc.theWorld.addEntityToWorld(FAKE_ENTITY_ID, body);
        mc.thePlayer.noClip = true;
        mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0.0D;
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.setPositionAndRotation(startX, startY, startZ, startYaw, startPitch);
            mc.thePlayer.noClip = false;
            mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0.0D;
            mc.thePlayer.fallDistance = 0.0F;
        }
        if (mc.theWorld != null) mc.theWorld.removeEntityFromWorld(FAKE_ENTITY_ID);
        body = null;
        setSuffix(null);
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            disable();
            return;
        }
        mc.thePlayer.noClip = true;
        mc.thePlayer.onGround = false;
        mc.thePlayer.fallDistance = 0.0F;

        double moveSpeed = speed.getInput();
        double forward = mc.thePlayer.movementInput.moveForward;
        double strafe = mc.thePlayer.movementInput.moveStrafe;
        float yaw = mc.thePlayer.rotationYaw;
        if (forward != 0.0D) {
            if (strafe > 0.0D) yaw += forward > 0.0D ? -45.0F : 45.0F;
            if (strafe < 0.0D) yaw += forward > 0.0D ? 45.0F : -45.0F;
            strafe = 0.0D;
            forward = Math.signum(forward);
        }
        strafe = Math.signum(strafe);
        double radians = Math.toRadians(yaw + 90.0F);
        mc.thePlayer.motionX = forward * moveSpeed * Math.cos(radians) + strafe * moveSpeed * Math.sin(radians);
        mc.thePlayer.motionZ = forward * moveSpeed * Math.sin(radians) - strafe * moveSpeed * Math.cos(radians);
        mc.thePlayer.motionY = mc.gameSettings.keyBindJump.isKeyDown() ? moveSpeed
                : mc.gameSettings.keyBindSneak.isKeyDown() ? -moveSpeed : 0.0D;

        double distance = Math.sqrt(mc.thePlayer.getDistanceSq(startX, startY, startZ));
        setSuffix(String.format(java.util.Locale.ROOT, "%.1fm", distance));
    }

    @SubscribeEvent
    public void onSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof C03PacketPlayer) {
            event.setCancelled(true);
            return;
        }
        if (cancelInteractions.isToggled() && (event.getPacket() instanceof C02PacketUseEntity
                || event.getPacket() instanceof C07PacketPlayerDigging
                || event.getPacket() instanceof C08PacketPlayerBlockPlacement
                || event.getPacket() instanceof C0APacketAnimation
                || event.getPacket() instanceof C0BPacketEntityAction)) {
            event.setCancelled(true);
        }
    }

    @SubscribeEvent
    public void onWorld(WorldEvent event) {
        disable();
    }
}
