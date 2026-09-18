package dev.veyra.client.module.modules.combat;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.DoubleSliderSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import dev.veyra.weave.events.AimUpdateEvent;
import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.weavemc.api.event.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.concurrent.ThreadLocalRandom;

/** Smooth camera-only aim assistance with a shared, sticky target selection. */
@SuppressWarnings("unused")
public final class AimAssist extends Module {
    private final ComboSetting<Mode> mode;
    private final SliderSetting horizontalSpeed;
    private final SliderSetting verticalSpeed;
    private final DoubleSliderSetting fov;
    private final SliderSetting distance;
    private final SliderSetting predict;
    private final SliderSetting multipoint;
    private final SliderSetting aimHeight;
    private final SliderSetting randomization;
    private final TickSetting clickingOnly;
    private final TickSetting weaponOnly;
    private final TickSetting sprintingOnly;
    private final TickSetting disableWhileBreaking;

    private EntityPlayer lockedTarget;
    private long lastFrameNanos;
    private long nextNoiseAtMs;
    private double noiseYaw;
    private double noisePitch;

    public AimAssist() {
        super("AimAssist", ModuleCategory.Combat, 0);
        registerSetting(new DescriptionSetting("Smoothly assists your own camera aim."));
        registerSetting(mode = new ComboSetting<>("Mode", Mode.Regular));
        registerSetting(horizontalSpeed = new SliderSetting("Horizontal speed", 75.0D, 5.0D, 240.0D, 5.0D));
        registerSetting(verticalSpeed = new SliderSetting("Vertical speed", 45.0D, 0.0D, 180.0D, 5.0D));
        registerSetting(fov = new DoubleSliderSetting("FOV", 2.0D, 70.0D, 0.0D, 180.0D, 1.0D));
        registerSetting(distance = new SliderSetting("Distance", 3.5D, 1.0D, 6.0D, 0.1D));
        registerSetting(predict = new SliderSetting("Predict", 20.0D, 0.0D, 100.0D, 5.0D));
        registerSetting(multipoint = new SliderSetting("Multipoint", 30.0D, 0.0D, 100.0D, 5.0D));
        registerSetting(aimHeight = new SliderSetting("Aim height", 62.0D, 20.0D, 90.0D, 1.0D));
        registerSetting(randomization = new SliderSetting("Randomization", 25.0D, 0.0D, 100.0D, 5.0D));
        registerSetting(clickingOnly = new TickSetting("Clicking only", true));
        registerSetting(weaponOnly = new TickSetting("Weapon only", true));
        registerSetting(sprintingOnly = new TickSetting("Sprinting only", false));
        registerSetting(disableWhileBreaking = new TickSetting("Disable while breaking", true));
    }

    @SubscribeEvent
    public void onAimUpdate(AimUpdateEvent event) {
        long frameNanos = System.nanoTime();
        double deltaSeconds = lastFrameNanos == 0L
                ? 1.0D / 60.0D
                : Math.min(0.05D, Math.max(0.001D, (frameNanos - lastFrameNanos) / 1_000_000_000.0D));
        lastFrameNanos = frameNanos;

        if (!canAssist()) {
            clearTarget();
            return;
        }

        if (!isStillValid(lockedTarget, true)) {
            lockedTarget = findBestTarget();
        }
        if (lockedTarget == null || isCrosshairInside(lockedTarget, event.getPartialTicks())) {
            return;
        }

        updateNoiseIfDue();
        AimPoint point = getAimPoint(lockedTarget, event.getPartialTicks());
        double desiredYaw = Math.toDegrees(Math.atan2(point.z() - mc.thePlayer.posZ, point.x() - mc.thePlayer.posX)) - 90.0D + noiseYaw;
        double eyeY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        double horizontalDistance = Math.hypot(point.x() - mc.thePlayer.posX, point.z() - mc.thePlayer.posZ);
        double desiredPitch = -Math.toDegrees(Math.atan2(point.y() - eyeY, horizontalDistance)) + noisePitch;

        double yawError = MathHelper.wrapAngleTo180_double(desiredYaw - mc.thePlayer.rotationYaw);
        double pitchError = desiredPitch - mc.thePlayer.rotationPitch;
        double deadzone = fov.getInputMin();
        // Treat the axes independently. A large horizontal correction must not
        // drag pitch toward a fixed chest point when the player's own vertical
        // aim was already good.
        double yawStep = Math.abs(yawError) > deadzone
                ? calculateStep(yawError, horizontalSpeed.getInput(), deltaSeconds)
                : 0.0D;
        double pitchDeadzone = Math.max(1.25D, deadzone);
        double pitchStep = Math.abs(pitchError) > pitchDeadzone
                ? calculateStep(pitchError, verticalSpeed.getInput(), deltaSeconds)
                : 0.0D;
        if (yawStep == 0.0D && pitchStep == 0.0D) return;
        mc.thePlayer.rotationYaw += (float) yawStep;
        mc.thePlayer.rotationPitch = MathHelper.clamp_float(
                mc.thePlayer.rotationPitch + (float) pitchStep,
                -90.0F,
                90.0F);
    }

    private boolean canAssist() {
        return PlayerUtils.isPlayerInGame()
                && mc.currentScreen == null
                && mc.inGameHasFocus
                && (!clickingOnly.isToggled() || Mouse.isButtonDown(0))
                && (!weaponOnly.isToggled() || PlayerUtils.isPlayerHoldingWeapon())
                && (!sprintingOnly.isToggled() || mc.thePlayer.isSprinting())
                && (!disableWhileBreaking.isToggled() || !isBreakingBlock());
    }

    private EntityPlayer findBestTarget() {
        EntityPlayer best = null;
        double bestScore = Double.MAX_VALUE;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isStillValid(player, false)) continue;
            AimPoint point = getAimPoint(player, 1.0F);
            double yaw = Math.toDegrees(Math.atan2(point.z() - mc.thePlayer.posZ, point.x() - mc.thePlayer.posX)) - 90.0D;
            double angle = Math.abs(MathHelper.wrapAngleTo180_double(yaw - mc.thePlayer.rotationYaw));
            double score = angle + mc.thePlayer.getDistanceToEntity(player) * 0.15D;
            if (score < bestScore) {
                bestScore = score;
                best = player;
            }
        }
        return best;
    }

    private boolean isStillValid(EntityPlayer player, boolean alreadyLocked) {
        if (!TargetFilter.isValidPlayer(player)
                || mc.thePlayer.getDistanceToEntity(player) > distance.getInput()) {
            return false;
        }
        AimPoint point = getAimPoint(player, 1.0F);
        double targetYaw = Math.toDegrees(Math.atan2(point.z() - mc.thePlayer.posZ, point.x() - mc.thePlayer.posX)) - 90.0D;
        double yawDifference = Math.abs(MathHelper.wrapAngleTo180_double(targetYaw - mc.thePlayer.rotationYaw));
        double allowedHalfFov = fov.getInputMax() * 0.5D + (alreadyLocked ? 15.0D : 0.0D);
        return yawDifference <= allowedHalfFov;
    }

    private AimPoint getAimPoint(EntityPlayer target, float partialTicks) {
        double interpolation = Math.max(0.0D, Math.min(1.0D, partialTicks));
        double baseX = target.prevPosX + (target.posX - target.prevPosX) * interpolation;
        double baseY = target.prevPosY + (target.posY - target.prevPosY) * interpolation;
        double baseZ = target.prevPosZ + (target.posZ - target.prevPosZ) * interpolation;

        double leadTicks = predict.getInput() / 100.0D * 2.0D;
        double predictedX = baseX + (target.posX - target.prevPosX) * leadTicks;
        double predictedZ = baseZ + (target.posZ - target.prevPosZ) * leadTicks;

        AxisAlignedBB box = target.getEntityBoundingBox();
        double halfWidth = (box.maxX - box.minX) * 0.5D;
        double height = box.maxY - box.minY;
        double minX = predictedX - halfWidth;
        double maxX = predictedX + halfWidth;
        double minZ = predictedZ - halfWidth;
        double maxZ = predictedZ + halfWidth;
        double centerY = baseY + height * (aimHeight.getInput() / 100.0D);

        double eyesX = mc.thePlayer.posX;
        double eyesY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        double eyesZ = mc.thePlayer.posZ;
        double closestX = clamp(eyesX, minX, maxX);
        double closestY = clamp(eyesY, baseY, baseY + height);
        double closestZ = clamp(eyesZ, minZ, maxZ);
        double blend = multipoint.getInput() / 100.0D;
        return new AimPoint(
                lerp(predictedX, closestX, blend),
                lerp(centerY, closestY, blend),
                lerp(predictedZ, closestZ, blend));
    }

    private boolean isCrosshairInside(EntityPlayer target, float partialTicks) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 look = mc.thePlayer.getLook(partialTicks);
        double reach = Math.min(distance.getInput() + 0.5D, mc.thePlayer.getDistanceToEntity(target) + 1.0D);
        Vec3 end = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
        return target.getEntityBoundingBox().expand(0.05D, 0.05D, 0.05D).calculateIntercept(eyes, end) != null;
    }

    private double calculateStep(double error, double speedDegreesPerSecond, double deltaSeconds) {
        if (speedDegreesPerSecond <= 0.0D) return 0.0D;
        double maximum = speedDegreesPerSecond * deltaSeconds;
        if (mode.getMode() == Mode.Regular) {
            double easing = clamp(Math.abs(error) / 25.0D, 0.12D, 1.0D);
            maximum *= easing;
        }
        return clamp(error, -maximum, maximum);
    }

    private void updateNoiseIfDue() {
        long now = System.currentTimeMillis();
        if (now < nextNoiseAtMs) return;
        double strength = randomization.getInput() / 100.0D * 0.7D;
        noiseYaw = ThreadLocalRandom.current().nextDouble(-strength, Math.nextUp(strength));
        noisePitch = ThreadLocalRandom.current().nextDouble(-strength * 0.6D, Math.nextUp(strength * 0.6D));
        nextNoiseAtMs = now + ThreadLocalRandom.current().nextLong(400L, 701L);
    }

    private boolean isBreakingBlock() {
        if (!Mouse.isButtonDown(0) || mc.objectMouseOver == null) return false;
        BlockPos position = mc.objectMouseOver.getBlockPos();
        if (position == null) return false;
        return mc.theWorld.getBlockState(position).getBlock() != Blocks.air
                && !(mc.theWorld.getBlockState(position).getBlock() instanceof BlockLiquid);
    }

    private void clearTarget() {
        lockedTarget = null;
        noiseYaw = 0.0D;
        noisePitch = 0.0D;
        nextNoiseAtMs = 0L;
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * amount;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onEnable() {
        lastFrameNanos = 0L;
        clearTarget();
    }

    @Override
    public void onDisable() {
        lastFrameNanos = 0L;
        clearTarget();
    }

    public enum Mode {
        Regular,
        Linear
    }

    private record AimPoint(double x, double y, double z) {
    }
}
