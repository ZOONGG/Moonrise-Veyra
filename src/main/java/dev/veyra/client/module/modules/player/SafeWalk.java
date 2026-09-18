package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.coordination.input.InputLeaseManager;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.DoubleSliderSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.player.bridge.BridgeAssistContext;
import dev.veyra.client.player.bridge.BridgeEdgeProbe;
import dev.veyra.client.player.bridge.BridgeAssistEngine;
import dev.veyra.client.player.bridge.BridgeAssistSettings;
import dev.veyra.client.player.bridge.BlockSelectionGuard;
import dev.veyra.client.player.bridge.BridgeSupportTracker;
import dev.veyra.client.utils.player.PlayerUtils;
import dev.veyra.client.utils.world.BlockInteractionUtils;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovementInput;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.RenderWorldEvent;
import net.weavemc.api.event.TickEvent;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.util.concurrent.ThreadLocalRandom;

/** Edge-triggered bridge assist with an unconditional local ledge failsafe. */
public final class SafeWalk extends Module {
    private static final String INPUT_OWNER = "bridge-assist";
    private static final int INPUT_PRIORITY = 50;
    private static final long MOVEMENT_GUARD_FRESH_MS = 500L;
    private static final String ACTIVE_PROPERTY =
            "dev.veyra.bridgeAssist.active";
    private static final String SCOPE_THREAD_PROPERTY =
            "dev.veyra.bridgeAssist.movementThread";
    private static final String LAST_GUARD_PROPERTY =
            "dev.veyra.bridgeAssist.lastGuardMs";
    private static volatile SafeWalk activeInstance;
    private static volatile long lastMovementGuardInvocationMs;
    private static final ThreadLocal<Boolean> MOVEMENT_GUARD_SCOPE =
            ThreadLocal.withInitial(() -> false);
    private static final BridgeSupportTracker SUPPORT_TRACKER = new BridgeSupportTracker();
    private final DoubleSliderSetting edgeOffset;
    private final DoubleSliderSetting unsneakDelayMs;
    private final ComboSetting<SelectBlocks> selectBlocks;
    private final TickSetting randomize;
    private final TickSetting sneakOnJump;
    private final TickSetting avoidDoubleSneaking;
    private final SliderSetting twoBlockChance;
    private final TickSetting requireSneakKey;
    private final TickSetting holdingBlocks;
    private final TickSetting lookingDown;
    private final DoubleSliderSetting pitchRange;
    private final TickSetting notMovingForward;

    private final BridgeAssistEngine engine = new BridgeAssistEngine();
    private final BlockSelectionGuard blockSelectionGuard = new BlockSelectionGuard();
    private boolean lastEdgeReached;
    private boolean resetEdgeSampleWhenReleased;
    private double sampledEdgeOffset = Double.NaN;
    private long sampledUnsneakDelayMs = -1L;
    private Boolean sampledTwoBlockPass;
    private int previousBlockCount;
    private boolean placementSignalled;
    private long lastSkipAutoPlaceMs;

    public SafeWalk() {
        super("BridgeAssist", ModuleCategory.Player, 0);
        registerSetting(edgeOffset = new DoubleSliderSetting(
                "Edge offset", 0.16D, 0.20D, 0.0D, 0.30D, 0.01D));
        registerSetting(unsneakDelayMs = new DoubleSliderSetting(
                "Unsneak delay ms", 25.0D, 55.0D, 0.0D, 250.0D, 5.0D));
        registerSetting(selectBlocks = new ComboSetting<>("Select blocks", SelectBlocks.No));
        registerSetting(randomize = new TickSetting("Randomize", true));
        registerSetting(sneakOnJump = new TickSetting("Sneak on jump", false));
        registerSetting(avoidDoubleSneaking = new TickSetting("Avoid double-sneaking", true));
        registerSetting(twoBlockChance = new SliderSetting(
                "No-shift chance %", 40.0D, 0.0D, 100.0D, 5.0D));
        registerSetting(requireSneakKey = new TickSetting("Sneak key pressed", false));
        registerSetting(holdingBlocks = new TickSetting("Holding blocks", true));
        registerSetting(lookingDown = new TickSetting("Looking down", true));
        registerSetting(pitchRange = new DoubleSliderSetting(
                "Pitch range", 70.0D, 90.0D, 0.0D, 90.0D, 1.0D));
        registerSetting(notMovingForward = new TickSetting("Not moving forward", false));
    }

    @Override
    public void onEnable() {
        activeInstance = this;
        lastMovementGuardInvocationMs = 0L;
        MOVEMENT_GUARD_SCOPE.remove();
        System.setProperty(ACTIVE_PROPERTY, Boolean.TRUE.toString());
        System.clearProperty(SCOPE_THREAD_PROPERTY);
        System.clearProperty(LAST_GUARD_PROPERTY);
        setSuffix(null);
        resetState();
    }

    @Override
    public void onDisable() {
        activeInstance = null;
        lastMovementGuardInvocationMs = 0L;
        MOVEMENT_GUARD_SCOPE.remove();
        System.clearProperty(ACTIVE_PROPERTY);
        System.clearProperty(SCOPE_THREAD_PROPERTY);
        System.clearProperty(LAST_GUARD_PROPERTY);
        setSuffix(null);
        releaseSneak();
        resetState();
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        long nowMs = System.currentTimeMillis();
        boolean environmentValid = isEnvironmentValid();
        // This is the non-negotiable safety layer. The moveEntity hook uses
        // vanilla's own ledge clamp for every movement tick while the module
        // is active, so a missed prediction can slow/stop but cannot drop us.
        boolean activationValid = environmentValid && prepareAndCheckConditions();
        boolean valid = activationValid
                || (environmentValid && engine.requiresSafetyContinuation());
        MovementInput input = environmentValid ? mc.thePlayer.movementInput : null;
        boolean edgeReached = valid && mc.thePlayer.onGround && isEdgeReached(
                input, edgeOffset(), BridgeEdgeProbe.STANDARD_MOTION_MARGIN);
        boolean emergencyEdgeReached = valid && mc.thePlayer.onGround && isEdgeReached(
                input, 0.30D, BridgeEdgeProbe.EMERGENCY_MOTION_MARGIN);
        boolean jumpNeedsSneak = valid && !mc.thePlayer.onGround
                && sneakOnJump.isToggled() && lacksSupportBelowPlayer();

        BridgeAssistSettings settings = settings();
        if (valid && lastEdgeReached && !edgeReached && engine.ownsSneak()
                && !placementSignalled) {
            engine.onBlockPlaced(nowMs, settings);
            resetEdgeSampleWhenReleased = true;
        }

        boolean ownedBeforeTick = engine.ownsSneak();
        boolean assistedSneak = engine.tick(new BridgeAssistContext(
                nowMs, valid, edgeReached, emergencyEdgeReached,
                jumpNeedsSneak, isSneakPhysicallyDown()), settings);
        setShift(assistedSneak);
        setSuffix(engine.fastPassArmed() ? "Skip AutoPlace" : null);

        if (!ownedBeforeTick && engine.ownsSneak()) placementSignalled = false;

        if (!valid || (resetEdgeSampleWhenReleased && !engine.ownsSneak())) {
            sampledEdgeOffset = Double.NaN;
            sampledUnsneakDelayMs = -1L;
            sampledTwoBlockPass = null;
            placementSignalled = false;
            resetEdgeSampleWhenReleased = false;
        }
        if (!environmentValid) SUPPORT_TRACKER.clear();
        lastEdgeReached = edgeReached;
    }

    private boolean isEnvironmentValid() {
        return PlayerUtils.isPlayerInGame() && mc.inGameHasFocus
                && mc.currentScreen == null && !mc.thePlayer.capabilities.isFlying;
    }

    private boolean prepareAndCheckConditions() {
        ItemStack held = mc.thePlayer.getHeldItem();
        boolean heldBlock = isBlock(held);
        boolean mayAutoSelect = blockSelectionGuard.allowAutomaticSelection(
                mc.thePlayer.inventory.currentItem, heldBlock);
        if (!heldBlock && shouldSelectBlock() && mayAutoSelect) {
            int slot = BlockInteractionUtils.findBlockSlot();
            if (slot >= 0 && BlockInteractionUtils.selectHotbarSlot(slot)) {
                blockSelectionGuard.recordAutomaticSelection(slot);
                held = mc.thePlayer.getHeldItem();
                heldBlock = isBlock(held);
            }
        }

        int currentCount = heldBlock ? held.stackSize : 0;
        boolean depleted = !heldBlock && previousBlockCount == 1;
        previousBlockCount = currentCount;
        if (depleted && selectBlocks.getMode() == SelectBlocks.OnDepletion) {
            int slot = BlockInteractionUtils.findBlockSlot();
            if (slot >= 0 && BlockInteractionUtils.selectHotbarSlot(slot)) {
                blockSelectionGuard.recordAutomaticSelection(slot);
                held = mc.thePlayer.getHeldItem();
                heldBlock = isBlock(held);
                previousBlockCount = heldBlock ? held.stackSize : 0;
            }
        }

        if (holdingBlocks.isToggled() && !heldBlock) return false;
        if (requireSneakKey.isToggled() && !isSneakPhysicallyDown()) return false;
        if (lookingDown.isToggled()
                && (mc.thePlayer.rotationPitch < pitchRange.getInputMin()
                || mc.thePlayer.rotationPitch > pitchRange.getInputMax())) return false;
        return !notMovingForward.isToggled()
                || mc.thePlayer.movementInput == null
                || mc.thePlayer.movementInput.moveForward <= 0.01F;
    }

    private boolean shouldSelectBlock() {
        return selectBlocks.getMode() == SelectBlocks.Always;
    }

    private double edgeOffset() {
        if (!Double.isNaN(sampledEdgeOffset)) return sampledEdgeOffset;
        sampledEdgeOffset = randomize.isToggled()
                ? randomBetween(edgeOffset.getInputMin(), edgeOffset.getInputMax())
                : (edgeOffset.getInputMin() + edgeOffset.getInputMax()) * 0.5D;
        return sampledEdgeOffset;
    }

    private BridgeAssistSettings settings() {
        if (sampledUnsneakDelayMs < 0L) {
            if (randomize.isToggled()) {
                sampledUnsneakDelayMs = Math.round(randomBetween(
                        unsneakDelayMs.getInputMin(), unsneakDelayMs.getInputMax()));
            } else {
                sampledUnsneakDelayMs = Math.round(
                        (unsneakDelayMs.getInputMin() + unsneakDelayMs.getInputMax()) * 0.5D);
            }
        }
        if (sampledTwoBlockPass == null) {
            double chance = Math.max(0.0D, Math.min(100.0D, twoBlockChance.getInput()));
            sampledTwoBlockPass = chance >= 100.0D
                    || (chance > 0.0D
                    && ThreadLocalRandom.current().nextDouble(100.0D) < chance);
        }
        return new BridgeAssistSettings(sampledUnsneakDelayMs,
                sneakOnJump.isToggled(), avoidDoubleSneaking.isToggled(),
                sampledTwoBlockPass);
    }

    /** Larger offsets permit more overhang, so sneak begins later and bridging is faster. */
    private boolean isEdgeReached(
            MovementInput input,
            double allowedOffset,
            double motionMargin
    ) {
        if (input == null) return false;
        double strafe = input.moveStrafe;
        double forward = input.moveForward;
        double length = Math.sqrt(strafe * strafe + forward * forward);
        if (length < 0.01D) return false;
        strafe /= length;
        forward /= length;

        double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
        double directionX = strafe * Math.cos(yaw) - forward * Math.sin(yaw);
        double directionZ = forward * Math.cos(yaw) + strafe * Math.sin(yaw);
        AxisAlignedBB playerBox = mc.thePlayer.getEntityBoundingBox();
        // Zero must be the safest value. The old direct mapping made zero use
        // the entire player box, so the probe stayed supported until the
        // player was almost completely off the block.
        double inset = BridgeEdgeProbe.insetForAllowedOverhang(allowedOffset);
        double movementAcceleration = mc.thePlayer.getAIMoveSpeed();
        double offsetX = BridgeEdgeProbe.predictedAxisDisplacement(
                mc.thePlayer.motionX, directionX, movementAcceleration, motionMargin);
        double offsetZ = BridgeEdgeProbe.predictedAxisDisplacement(
                mc.thePlayer.motionZ, directionZ, movementAcceleration, motionMargin);
        double feetY = playerBox.minY;
        AxisAlignedBB supportProbe = new AxisAlignedBB(
                playerBox.minX + inset + offsetX, feetY - 0.08D,
                playerBox.minZ + inset + offsetZ,
                playerBox.maxX - inset + offsetX, feetY - 0.01D,
                playerBox.maxZ - inset + offsetZ);
        return mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, supportProbe).isEmpty();
    }

    private boolean lacksSupportBelowPlayer() {
        AxisAlignedBB feet = mc.thePlayer.getEntityBoundingBox().offset(0.0D, -0.08D, 0.0D);
        return mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, feet).isEmpty();
    }

    private void setShift(boolean assistedSneak) {
        int keyCode = mc.gameSettings.keyBindSneak.getKeyCode();
        if (assistedSneak) {
            InputLeaseManager.set(INPUT_OWNER, keyCode, true, INPUT_PRIORITY);
        } else {
            // In inventories and containers the physical Shift key belongs to
            // shift-click. Restoring it as a gameplay binding would make the
            // player sneak behind the GUI and can leave the binding stuck.
            InputLeaseManager.releaseOwner(INPUT_OWNER, mc.currentScreen == null);
        }
    }

    private void releaseSneak() {
        InputLeaseManager.releaseOwner(INPUT_OWNER, mc.currentScreen == null);
    }

    private boolean isSneakPhysicallyDown() {
        return InputLeaseManager.physicalState(mc.gameSettings.keyBindSneak.getKeyCode());
    }

    private void resetState() {
        engine.reset();
        SUPPORT_TRACKER.clear();
        lastEdgeReached = false;
        sampledEdgeOffset = Double.NaN;
        sampledUnsneakDelayMs = -1L;
        sampledTwoBlockPass = null;
        resetEdgeSampleWhenReleased = false;
        previousBlockCount = 0;
        placementSignalled = false;
        lastSkipAutoPlaceMs = 0L;
        blockSelectionGuard.reset();
    }

    /** Used only by the local-player moveEntity safety redirect. */
    public static boolean shouldHardClampMovement() {
        return activeInstance != null || Boolean.getBoolean(ACTIVE_PROPERTY);
    }

    public static void enterMovementGuardScope() {
        MOVEMENT_GUARD_SCOPE.set(true);
        System.setProperty(SCOPE_THREAD_PROPERTY,
                Long.toString(Thread.currentThread().getId()));
    }

    public static void exitMovementGuardScope() {
        MOVEMENT_GUARD_SCOPE.remove();
        String ownerThread = System.getProperty(SCOPE_THREAD_PROPERTY);
        if (Long.toString(Thread.currentThread().getId()).equals(ownerThread)) {
            System.clearProperty(SCOPE_THREAD_PROPERTY);
        }
    }

    public static boolean isInsideMovementGuardScope() {
        if (!shouldHardClampMovement()) return false;
        if (MOVEMENT_GUARD_SCOPE.get()) return true;
        return Long.toString(Thread.currentThread().getId()).equals(
                System.getProperty(SCOPE_THREAD_PROPERTY));
    }

    /** Called from the exact vanilla SafeWalk flag store in Entity.moveEntity. */
    public static void recordMovementGuardInvocation() {
        if (lastMovementGuardInvocationMs == 0L) {
            System.out.println("[Veyra] BridgeAssist vanilla movement guard active");
        }
        long nowMs = System.currentTimeMillis();
        lastMovementGuardInvocationMs = nowMs;
        System.setProperty(LAST_GUARD_PROPERTY, Long.toString(nowMs));
    }

    private static boolean movementGuardOperational() {
        long invocationMs = Math.max(lastMovementGuardInvocationMs,
                sharedGuardInvocationMs());
        return shouldHardClampMovement() && invocationMs > 0L
                && System.currentTimeMillis() - invocationMs <= MOVEMENT_GUARD_FRESH_MS;
    }

    private static long sharedGuardInvocationMs() {
        String value = System.getProperty(LAST_GUARD_PROPERTY);
        if (value == null) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    /**
     * A client ghost block is not support. Existing world collision becomes
     * trusted only when at least one colliding shape is not awaiting a server
     * block-change confirmation.
     */
    public static boolean hasTrustedBridgeSupport(
            net.minecraft.entity.Entity entity,
            AxisAlignedBB projectedFootprint
    ) {
        if (entity == null || entity.worldObj == null || projectedFootprint == null) {
            return false;
        }
        java.util.List<AxisAlignedBB> collisions = entity.worldObj
                .getCollidingBoundingBoxes(entity, projectedFootprint);
        for (AxisAlignedBB collision : collisions) {
            if (!SUPPORT_TRACKER.isUnconfirmedCollision(
                    collision.minX, collision.minY, collision.minZ,
                    collision.maxX, collision.maxY, collision.maxZ)) return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onPlacement(PacketEvent.Send event) {
        if (!(event.getPacket() instanceof C08PacketPlayerBlockPlacement placement)
                || placement.getStack() == null
                || !(placement.getStack().getItem() instanceof ItemBlock)
                || placement.getPosition() == null
                || placement.getPosition().getY() < 0) {
            return;
        }
        int direction = placement.getPlacedBlockDirection();
        if (direction >= 0 && direction < EnumFacing.values().length) {
            BlockPos target = placement.getPosition().offset(EnumFacing.getFront(direction));
            SUPPORT_TRACKER.recordAttempt(target.getX(), target.getY(), target.getZ());
        }

        if (!engine.ownsSneak() && !engine.fastPassArmed()) return;
        placementSignalled = true;
        resetEdgeSampleWhenReleased = true;
        if (engine.ownsSneak()) {
            engine.onBlockPlaced(System.currentTimeMillis(), settings());
        } else {
            engine.onFastPassPlacementAttempt();
        }
    }

    @SubscribeEvent
    public void onServerBlockUpdate(PacketEvent.Receive event) {
        if (event.getPacket() instanceof S23PacketBlockChange packet) {
            confirmServerSupport(packet.getBlockPosition(), packet.getBlockState());
            return;
        }
        if (event.getPacket() instanceof S22PacketMultiBlockChange packet) {
            for (S22PacketMultiBlockChange.BlockUpdateData update : packet.getChangedBlocks()) {
                confirmServerSupport(update.getPos(), update.getBlockState());
            }
        }
    }

    /** Stormy-style world-render placement, active only for the skipped block. */
    @SubscribeEvent
    public void onSkipAutoPlace(RenderWorldEvent event) {
        if (!engine.fastPassArmed() || !isEnvironmentValid()
                || !Mouse.isButtonDown(1)) return;

        ItemStack stack = mc.thePlayer.getHeldItem();
        if (!isBlock(stack)) return;

        long nowMs = System.currentTimeMillis();
        if (nowMs - lastSkipAutoPlaceMs < 8L) return;
        lastSkipAutoPlaceMs = nowMs;

        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit != null && hit.hitVec != null
                && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && hit.sideHit != EnumFacing.UP && hit.sideHit != EnumFacing.DOWN) {
            BlockPos support = hit.getBlockPos();
            BlockPos target = support.offset(hit.sideHit);
            if (BlockInteractionUtils.canClick(support)
                    && BlockInteractionUtils.isReplaceable(target)
                    && mc.playerController.onPlayerRightClick(
                    mc.thePlayer, mc.theWorld, stack,
                    support, hit.sideHit, hit.hitVec)) {
                finishSkipPlacement();
                return;
            }
        }

        placePredictedSkipBlock();
    }

    private void placePredictedSkipBlock() {
        MovementInput input = mc.thePlayer.movementInput;
        if (input == null) return;
        double strafe = input.moveStrafe;
        double forward = input.moveForward;
        double length = Math.sqrt(strafe * strafe + forward * forward);
        if (length < 0.01D) return;
        strafe /= length;
        forward /= length;

        double yaw = Math.toRadians(mc.thePlayer.rotationYaw);
        double directionX = strafe * Math.cos(yaw) - forward * Math.sin(yaw);
        double directionZ = forward * Math.cos(yaw) + strafe * Math.sin(yaw);
        double feetY = mc.thePlayer.getEntityBoundingBox().minY - 1.0D;

        // Try the farthest reachable candidate first. At high speed this
        // creates the next support before the crosshair loses the old face.
        double[] lookAhead = {1.05D, 0.80D, 0.55D};
        for (double distance : lookAhead) {
            BlockPos target = new BlockPos(
                    mc.thePlayer.posX + directionX * distance,
                    feetY,
                    mc.thePlayer.posZ + directionZ * distance);
            if (!BlockInteractionUtils.isReplaceable(target)) continue;
            if (BlockInteractionUtils.placeBlock(target, true)) {
                return;
            }
        }
    }

    private void finishSkipPlacement() {
        mc.thePlayer.swingItem();
        mc.getItemRenderer().resetEquippedProgress();
    }

    private static void confirmServerSupport(
            BlockPos position,
            net.minecraft.block.state.IBlockState state
    ) {
        boolean accepted = state != null
                && !state.getBlock().getMaterial().isReplaceable();
        SUPPORT_TRACKER.confirm(
                position.getX(), position.getY(), position.getZ(), accepted);
    }

    private static boolean isBlock(ItemStack stack) {
        return stack != null && stack.stackSize > 0 && stack.getItem() instanceof ItemBlock;
    }

    private static double randomBetween(double min, double max) {
        if (max <= min) return min;
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public enum SelectBlocks {
        No,
        OnDepletion,
        Always
    }
}
