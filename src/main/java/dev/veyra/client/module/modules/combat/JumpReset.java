package dev.veyra.client.module.modules.combat;

import dev.veyra.client.module.Module;
import dev.veyra.client.coordination.input.InputLeaseManager;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.DoubleSliderSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Performs a vanilla jump input after the local player receives meaningful
 * horizontal knockback. This module intentionally never edits/cancels the
 * velocity packet and never writes to the player's motion fields.
 */
@SuppressWarnings("unused")
public final class JumpReset extends Module {
    private static final int MIN_HORIZONTAL_PACKET_MOTION = 800;
    private static final long MAX_SIGNAL_AGE_MS = 1_000L;
    private static final String INPUT_OWNER = "jump-reset";

    private final SliderSetting chance;
    private final DoubleSliderSetting delayTicks;
    private final DoubleSliderSetting cooldownMs;
    private final TickSetting forwardOnly;
    private final TickSetting clickingOnly;
    private final TickSetting weaponOnly;
    private final TickSetting sprintingOnly;

    private final AtomicReference<VelocitySignal> velocitySignal = new AtomicReference<>();
    private PendingJump pendingJump;
    private long nextAllowedAtMs;
    private volatile int localPlayerEntityId = -1;
    private boolean injectedJump;
    private int releaseAtTick = Integer.MIN_VALUE;
    private int lastObservedTick = Integer.MIN_VALUE;

    public JumpReset() {
        super("JumpReset", ModuleCategory.Combat, 0);
        registerSetting(new DescriptionSetting("Uses a normal jump input after incoming knockback."));
        registerSetting(chance = new SliderSetting("Chance", 70.0D, 0.0D, 100.0D, 1.0D));
        registerSetting(delayTicks = new DoubleSliderSetting("Delay ticks", 0.0D, 0.0D, 0.0D, 3.0D, 1.0D));
        registerSetting(cooldownMs = new DoubleSliderSetting("Cooldown ms", 450.0D, 650.0D, 0.0D, 1200.0D, 25.0D));
        registerSetting(forwardOnly = new TickSetting("Forward only", true));
        registerSetting(clickingOnly = new TickSetting("Clicking only", false));
        registerSetting(weaponOnly = new TickSetting("Weapon only", true));
        registerSetting(sprintingOnly = new TickSetting("Sprinting only", false));
    }

    @SubscribeEvent
    public void onVelocity(PacketEvent.Receive event) {
        if (!(event.getPacket() instanceof S12PacketEntityVelocity packet)
                || packet.getEntityID() != localPlayerEntityId
                || packet.getMotionY() <= 0) {
            return;
        }

        long horizontalMotionSquared = (long) packet.getMotionX() * packet.getMotionX()
                + (long) packet.getMotionZ() * packet.getMotionZ();
        long minimumMotionSquared = (long) MIN_HORIZONTAL_PACKET_MOTION * MIN_HORIZONTAL_PACKET_MOTION;
        if (horizontalMotionSquared < minimumMotionSquared) {
            return;
        }
        // NetworkManager posts receive events on Netty's thread. Store only an
        // immutable signal here; all player/input access happens on TickEvent.
        velocitySignal.set(new VelocitySignal(System.currentTimeMillis()));
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        if (!PlayerUtils.isPlayerInGame() || mc.currentScreen != null) {
            clearState(true);
            return;
        }

        int currentTick = mc.thePlayer.ticksExisted;
        localPlayerEntityId = mc.thePlayer.getEntityId();
        if (lastObservedTick != Integer.MIN_VALUE && currentTick < lastObservedTick) {
            clearState(true);
            localPlayerEntityId = mc.thePlayer.getEntityId();
        }
        lastObservedTick = currentTick;

        releaseInjectedJumpIfDue(currentTick);
        consumeVelocitySignal(currentTick);

        PendingJump pending = pendingJump;
        if (pending == null || currentTick < pending.executeAtTick()) {
            return;
        }

        pendingJump = null;
        if (currentTick > pending.expiresAtTick()
                || !canPerformJump()
                || isPhysicalKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
            return;
        }

        beginInjectedJump(currentTick);
    }

    private void consumeVelocitySignal(int currentTick) {
        VelocitySignal signal = velocitySignal.getAndSet(null);
        if (signal == null || injectedJump || pendingJump != null) return;

        long now = System.currentTimeMillis();
        if (now - signal.receivedAtMs() > MAX_SIGNAL_AGE_MS
                || now < nextAllowedAtMs
                || !canScheduleJump()
                || !rollChance(chance.getInput())) {
            return;
        }

        int delay = randomInclusive(delayTicks.getInputMin(), delayTicks.getInputMax());
        if (delay == 0) {
            if (!beginInjectedJump(currentTick)) return;
        } else {
            int executeAtTick = currentTick + delay;
            pendingJump = new PendingJump(executeAtTick, executeAtTick + 2);
        }
        nextAllowedAtMs = now
                + randomInclusive(cooldownMs.getInputMin(), cooldownMs.getInputMax());
    }

    private boolean beginInjectedJump(int currentTick) {
        int jumpKey = mc.gameSettings.keyBindJump.getKeyCode();
        if (!canPerformJump() || isPhysicalKeyDown(jumpKey)) return false;

        // Input only: vanilla code decides whether and how the player jumps.
        InputLeaseManager.set(INPUT_OWNER, jumpKey, true, 70);
        injectedJump = true;
        releaseAtTick = currentTick + 1;
        return true;
    }

    private boolean canScheduleJump() {
        return mc.currentScreen == null
                && mc.thePlayer.onGround
                && canUseMovementInput()
                && !isPhysicalKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
    }

    private boolean canPerformJump() {
        return mc.currentScreen == null
                && mc.thePlayer.onGround
                && mc.thePlayer.isCollidedVertically
                && canUseMovementInput();
    }

    private boolean canUseMovementInput() {
        if (mc.thePlayer.isInWater()
                || mc.thePlayer.isInLava()
                || mc.thePlayer.isOnLadder()
                || mc.thePlayer.ridingEntity != null
                || mc.thePlayer.capabilities.isFlying) {
            return false;
        }
        if (forwardOnly.isToggled() && mc.thePlayer.moveForward <= 0.0F) {
            return false;
        }
        if (clickingOnly.isToggled() && !Mouse.isButtonDown(0)) {
            return false;
        }
        if (weaponOnly.isToggled() && !PlayerUtils.isPlayerHoldingWeapon()) {
            return false;
        }
        return !sprintingOnly.isToggled() || mc.thePlayer.isSprinting();
    }

    private void releaseInjectedJumpIfDue(int currentTick) {
        if (!injectedJump || currentTick < releaseAtTick) {
            return;
        }
        restorePhysicalJumpState();
        injectedJump = false;
        releaseAtTick = Integer.MIN_VALUE;
    }

    private void clearState(boolean restoreJumpKey) {
        velocitySignal.set(null);
        pendingJump = null;
        nextAllowedAtMs = 0L;
        localPlayerEntityId = -1;
        if (restoreJumpKey && injectedJump) {
            restorePhysicalJumpState();
        }
        injectedJump = false;
        releaseAtTick = Integer.MIN_VALUE;
        lastObservedTick = Integer.MIN_VALUE;
    }

    private void restorePhysicalJumpState() {
        InputLeaseManager.releaseOwner(INPUT_OWNER);
    }

    private static boolean rollChance(double percent) {
        return percent >= 100.0D
                || (percent > 0.0D && ThreadLocalRandom.current().nextDouble(100.0D) < percent);
    }

    private static int randomInclusive(double minValue, double maxValue) {
        int min = (int) Math.round(Math.min(minValue, maxValue));
        int max = (int) Math.round(Math.max(minValue, maxValue));
        return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static boolean isPhysicalKeyDown(int keyCode) {
        return InputLeaseManager.physicalState(keyCode);
    }

    @Override
    public void onEnable() {
        clearState(false);
    }

    @Override
    public void onDisable() {
        clearState(true);
    }

    private record PendingJump(int executeAtTick, int expiresAtTick) {
    }

    private record VelocitySignal(long receivedAtMs) {
    }
}
