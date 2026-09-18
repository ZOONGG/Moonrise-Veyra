package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import dev.veyra.client.utils.world.BlockInteractionUtils;
import dev.veyra.weave.mixins.IC03PacketPlayer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Server-confirmed silent clutch.
 *
 * <p>The module only starts after the player has physically left a real
 * support edge. It steers the rotation sent to the server over normal movement
 * packets while keeping the first-person camera untouched. A placement is
 * never used as the support for another placement until the server confirms
 * that block. This makes a rejected placement end the sequence instead of
 * turning into an invalid-packet cascade.</p>
 */
@SuppressWarnings("unused")
public final class Clutch extends Module {
    private static final double REACH_EPSILON = 0.04D;
    private static final int CONFIRMATION_TIMEOUT_TICKS = 16;
    private static final int MISSED_TARGET_TIMEOUT_TICKS = 8;
    private static final EnumFacing[] SIDE_FACES = {
            EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST
    };
    private static final EnumFacing[] ALL_FACES = {
            EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST,
            EnumFacing.EAST, EnumFacing.DOWN, EnumFacing.UP
    };

    public static SliderSetting minimumHeight;
    public static SliderSetting reactionTicks;
    public static SliderSetting horizontalTurnSpeed;
    public static SliderSetting verticalTurnSpeed;
    public static ComboSetting<SelectBlocks> selectBlocks;
    public static TickSetting onlyPlaceSideways;
    public static TickSetting multipoint;
    public static TickSetting recentlyDamagedOnly;
    public static TickSetting backwardsOnly;

    private static volatile boolean silentRenderActive;
    private static volatile float silentRenderYaw;
    private static volatile float silentRenderPitch;

    private final AtomicReference<Confirmation> incomingConfirmation = new AtomicReference<>();
    private State state = State.IDLE;
    private SupportLevel takeoffSupport;
    private PlacementTarget target;
    private PendingPlacement awaiting;
    private long clientTick;
    private long airborneSinceTick;
    private long targetSinceTick;
    private long fallId;
    private int placedBlocks;
    private int originalSlot = -1;
    private float serverYaw;
    private float serverPitch;
    private boolean controlsServerRotation;
    private boolean failedThisFlight;

    public Clutch() {
        super("Clutch", ModuleCategory.Player, 0);
        registerSetting(new DescriptionSetting("Silent telly-style save with server-confirmed blocks."));
        registerSetting(minimumHeight = new SliderSetting("Minimum void height", 2.0D, 1.0D, 8.0D, 1.0D));
        registerSetting(reactionTicks = new SliderSetting("Reaction ticks", 0.0D, 0.0D, 3.0D, 1.0D));
        registerSetting(horizontalTurnSpeed = new SliderSetting("Horizontal turn per tick", 55.0D, 20.0D, 90.0D, 5.0D));
        registerSetting(verticalTurnSpeed = new SliderSetting("Vertical turn per tick", 40.0D, 15.0D, 90.0D, 5.0D));
        registerSetting(selectBlocks = new ComboSetting<>("Select blocks", SelectBlocks.Always));
        registerSetting(onlyPlaceSideways = new TickSetting("Only place sideways", true));
        registerSetting(multipoint = new TickSetting("Multipoint", true));
        registerSetting(recentlyDamagedOnly = new TickSetting("Recently damaged only", false));
        registerSetting(backwardsOnly = new TickSetting("Moving backwards only", false));
    }

    @SubscribeEvent
    public void onTickPre(TickEvent.Pre event) {
        clientTick++;
        processConfirmation();

        if (!canRun()) {
            restoreImmediately();
            reset(true);
            setSuffix("Ready");
            return;
        }

        if (mc.thePlayer.onGround) {
            takeoffSupport = findSupportImmediatelyBelow(mc.thePlayer.getEntityBoundingBox());
            if (state != State.IDLE || controlsServerRotation) finishSequence();
            failedThisFlight = false;
            airborneSinceTick = clientTick;
            setSuffix(hasSelectableBlocks() ? "Ready" : "No blocks");
            return;
        }

        if (takeoffSupport == null || failedThisFlight || state == State.FAILED) return;
        if (state == State.IDLE) {
            if (!shouldStartSequence()) return;
            startSequence();
        }

        if (state == State.AWAITING_CONFIRMATION) {
            if (awaiting != null && clientTick - awaiting.sentTick > CONFIRMATION_TIMEOUT_TICKS) {
                fail("server confirmation timeout");
            }
            return;
        }

        if (state == State.AIMING && target == null) {
            target = findNextTarget();
            if (target != null) {
                targetSinceTick = clientTick;
                setSuffix("Aim " + (placedBlocks + 1));
                log("target=" + target.target + " support=" + target.support
                        + " face=" + target.face);
            } else if (clientTick - airborneSinceTick > MISSED_TARGET_TIMEOUT_TICKS
                    && mc.thePlayer.motionY < -0.20D) {
                fail("no reachable server-confirmed face");
            }
        }

        if (state == State.AIMING && target != null
                && (!isTargetStillValid(target)
                || clientTick - targetSinceTick > MISSED_TARGET_TIMEOUT_TICKS)) {
            target = null;
        }
    }

    @SubscribeEvent
    public void onOutgoingMovement(PacketEvent.Send event) {
        if (!(event.getPacket() instanceof C03PacketPlayer packet)
                || !PlayerUtils.isPlayerInGame()) return;

        if (state == State.AIMING && target != null) {
            serverYaw = approachAngle(serverYaw, target.yaw,
                    (float) horizontalTurnSpeed.getInput());
            serverPitch = approachLinear(serverPitch, target.pitch,
                    (float) verticalTurnSpeed.getInput());
            serverYaw = quantizeRotation(serverYaw, silentRenderYaw);
            serverPitch = MathHelper.clamp_float(
                    quantizeRotation(serverPitch, silentRenderPitch), -90.0F, 90.0F);
            writeRotation(packet, serverYaw, serverPitch);
            controlsServerRotation = true;
            setRenderRotation(serverYaw, serverPitch);

            double reach = mc.playerController.getBlockReachDistance() + REACH_EPSILON;
            if (rotationRayHitsSupport(serverYaw, serverPitch, reach,
                    target.support, target.face)) {
                state = State.PLACE_PENDING;
                setSuffix("Place " + (placedBlocks + 1));
            }
            return;
        }

        if (state == State.PLACE_PENDING || state == State.AWAITING_CONFIRMATION) {
            writeRotation(packet, serverYaw, serverPitch);
            controlsServerRotation = true;
            setRenderRotation(serverYaw, serverPitch);
            return;
        }

        if (state == State.RESTORING && controlsServerRotation) {
            float cameraYaw = mc.thePlayer.rotationYaw;
            float cameraPitch = mc.thePlayer.rotationPitch;
            serverYaw = approachAngle(serverYaw, cameraYaw, 70.0F);
            serverPitch = approachLinear(serverPitch, cameraPitch, 55.0F);
            writeRotation(packet, serverYaw, serverPitch);
            setRenderRotation(serverYaw, serverPitch);
            if (angleDistance(serverYaw, cameraYaw) < 0.75F
                    && Math.abs(serverPitch - cameraPitch) < 0.75F) {
                controlsServerRotation = false;
                silentRenderActive = false;
                state = State.IDLE;
            }
        }
    }

    @SubscribeEvent
    public void onTickPost(TickEvent.Post event) {
        if (state != State.PLACE_PENDING || target == null) return;
        PlacementTarget placement = target;
        double reach = mc.playerController.getBlockReachDistance() + REACH_EPSILON;
        if (!isTargetStillValid(placement)
                || !rotationRayHitsSupport(serverYaw, serverPitch, reach,
                placement.support, placement.face)) {
            target = null;
            state = State.AIMING;
            return;
        }

        int slot = resolveBlockSlot(placement);
        if (slot < 0) {
            fail("no placeable blocks");
            return;
        }
        if (originalSlot < 0) originalSlot = mc.thePlayer.inventory.currentItem;
        boolean sent = BlockInteractionUtils.placeBlock(
                placement.support, placement.face, placement.hitVec, slot);
        if (!sent) {
            fail("client controller rejected placement");
            return;
        }

        awaiting = new PendingPlacement(placement.target, clientTick);
        incomingConfirmation.set(null);
        target = null;
        state = State.AWAITING_CONFIRMATION;
        setSuffix("Verify " + (placedBlocks + 1));
        log("placement sent; waiting for server block update");
    }

    @SubscribeEvent
    public void onIncoming(PacketEvent.Receive event) {
        PendingPlacement pending = awaiting;
        if (pending == null) return;

        if (event.getPacket() instanceof S23PacketBlockChange packet) {
            if (pending.position.equals(packet.getBlockPosition())) {
                publishConfirmation(pending, packet.getBlockState());
            }
            return;
        }

        if (event.getPacket() instanceof S22PacketMultiBlockChange packet) {
            for (S22PacketMultiBlockChange.BlockUpdateData update : packet.getChangedBlocks()) {
                if (pending.position.equals(update.getPos())) {
                    publishConfirmation(pending, update.getBlockState());
                    return;
                }
            }
        }
    }

    private void publishConfirmation(PendingPlacement pending, IBlockState blockState) {
        boolean accepted = blockState != null
                && !blockState.getBlock().getMaterial().isReplaceable();
        incomingConfirmation.compareAndSet(null,
                new Confirmation(pending.position, pending.sentTick, accepted));
    }

    private void processConfirmation() {
        Confirmation confirmation = incomingConfirmation.getAndSet(null);
        PendingPlacement pending = awaiting;
        if (confirmation == null || pending == null || !confirmation.matches(pending)) return;
        if (!confirmation.accepted) {
            fail("server rejected placement");
            return;
        }

        placedBlocks++;
        awaiting = null;
        state = State.AIMING;
        setSuffix("Saved " + placedBlocks);
        log("server confirmed block " + placedBlocks);
    }

    private boolean shouldStartSequence() {
        if (clientTick - airborneSinceTick < Math.round(reactionTicks.getInput())) return false;
        if (recentlyDamagedOnly.isToggled()
                && mc.thePlayer.hurtTime <= 0 && mc.thePlayer.hurtResistantTime <= 0) return false;
        if (backwardsOnly.isToggled() && mc.thePlayer.moveForward >= 0.0F) return false;
        if (!hasSelectableBlocks()) return false;

        AxisAlignedBB current = mc.thePlayer.getEntityBoundingBox();
        AxisAlignedBB next = current.offset(mc.thePlayer.motionX, 0.0D, mc.thePlayer.motionZ);
        // A normal jump over existing blocks is never a clutch. The sequence
        // only arms after both the current and next footprint have left the
        // actual takeoff support plane.
        return !hasSolidFootprint(current, takeoffSupport.blockY)
                && !hasSolidFootprint(next, takeoffSupport.blockY);
    }

    private void startSequence() {
        fallId++;
        placedBlocks = 0;
        target = null;
        awaiting = null;
        incomingConfirmation.set(null);
        originalSlot = mc.thePlayer.inventory.currentItem;
        serverYaw = mc.thePlayer.rotationYaw;
        serverPitch = mc.thePlayer.rotationPitch;
        silentRenderYaw = serverYaw;
        silentRenderPitch = serverPitch;
        state = State.AIMING;
        setSuffix("Telly");
        log("edge left; silent aim armed");
    }

    private PlacementTarget findNextTarget() {
        PlacementTarget best = null;
        int edgePlane = takeoffSupport.blockY;
        int feetPlane = MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minY) - 1;
        int lowestPlane = Math.max(edgePlane - 2, feetPlane - 1);

        for (int plane = edgePlane; plane >= lowestPlane; plane--) {
            for (BlockPos candidate : candidateFootBlocks(plane)) {
                PlacementTarget placement = directPlacementTarget(candidate);
                if (placement != null && (best == null || placement.compareTo(best) < 0)) {
                    best = placement;
                }
            }
        }
        return best;
    }

    private Set<BlockPos> candidateFootBlocks(int planeY) {
        LinkedHashSet<BlockPos> blocks = new LinkedHashSet<>();
        AxisAlignedBB current = mc.thePlayer.getEntityBoundingBox();
        addFootprint(blocks, current, planeY);
        addFootprint(blocks, current.offset(mc.thePlayer.motionX, 0.0D, mc.thePlayer.motionZ), planeY);
        addFootprint(blocks, predictBoundsAtPlane(planeY), planeY);
        return blocks;
    }

    private PlacementTarget directPlacementTarget(BlockPos targetPosition) {
        if (!isValidTargetBlock(targetPosition)) return null;
        PlacementTarget best = null;
        EnumFacing[] faces = onlyPlaceSideways.isToggled() ? SIDE_FACES : ALL_FACES;

        for (EnumFacing face : faces) {
            BlockPos support = targetPosition.offset(face.getOpposite());
            if (!isSafeClickableSupport(support)) continue;
            Vec3 hitVec = hitPoint(support, face);
            double distance = mc.thePlayer.getPositionEyes(1.0F).distanceTo(hitVec);
            if (distance > mc.playerController.getBlockReachDistance() + REACH_EPSILON) continue;
            if (resolveBlockSlot(targetPosition, support, face) < 0) continue;

            float[] rotations = rotationsTo(hitVec);
            PlacementTarget candidate = new PlacementTarget(targetPosition, support, face,
                    hitVec, rotations[0], rotations[1], distanceToTrajectory(targetPosition), distance);
            if (best == null || candidate.compareTo(best) < 0) best = candidate;
        }
        return best;
    }

    private boolean isValidTargetBlock(BlockPos position) {
        return BlockInteractionUtils.isReplaceable(position)
                && !intersectsPlayer(position)
                && countAirBelow(position) >= (int) Math.round(minimumHeight.getInput());
    }

    private boolean isTargetStillValid(PlacementTarget placement) {
        if (placement == null || !isValidTargetBlock(placement.target)
                || !isSafeClickableSupport(placement.support)) return false;
        double reach = mc.playerController.getBlockReachDistance() + REACH_EPSILON;
        return mc.thePlayer.getPositionEyes(1.0F).distanceTo(placement.hitVec) <= reach
                && resolveBlockSlot(placement) >= 0;
    }

    private SupportLevel findSupportImmediatelyBelow(AxisAlignedBB bounds) {
        int minX = MathHelper.floor_double(bounds.minX + 0.001D);
        int maxX = MathHelper.floor_double(bounds.maxX - 0.001D);
        int minZ = MathHelper.floor_double(bounds.minZ + 0.001D);
        int maxZ = MathHelper.floor_double(bounds.maxZ - 0.001D);
        int highestY = MathHelper.floor_double(bounds.minY + 0.05D);
        SupportLevel best = null;

        for (int y = highestY; y >= highestY - 2; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos position = new BlockPos(x, y, z);
                    IBlockState blockState = mc.theWorld.getBlockState(position);
                    AxisAlignedBB collision = blockState.getBlock()
                            .getCollisionBoundingBox(mc.theWorld, position, blockState);
                    if (collision == null || !horizontalIntersects(bounds, collision)) continue;
                    double gap = bounds.minY - collision.maxY;
                    if (gap < -0.02D || gap > 0.35D) continue;
                    if (best == null || collision.maxY > best.topY) {
                        best = new SupportLevel(position.getY(), collision.maxY);
                    }
                }
            }
        }
        return best;
    }

    private boolean hasSolidFootprint(AxisAlignedBB bounds, int planeY) {
        int minX = MathHelper.floor_double(bounds.minX + 0.001D);
        int maxX = MathHelper.floor_double(bounds.maxX - 0.001D);
        int minZ = MathHelper.floor_double(bounds.minZ + 0.001D);
        int maxZ = MathHelper.floor_double(bounds.maxZ - 0.001D);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos position = new BlockPos(x, planeY, z);
                IBlockState blockState = mc.theWorld.getBlockState(position);
                AxisAlignedBB collision = blockState.getBlock()
                        .getCollisionBoundingBox(mc.theWorld, position, blockState);
                if (collision != null && collision.maxY >= planeY + 0.45D
                        && horizontalIntersects(bounds, collision)) return true;
            }
        }
        return false;
    }

    private AxisAlignedBB predictBoundsAtPlane(int planeY) {
        AxisAlignedBB predicted = mc.thePlayer.getEntityBoundingBox();
        double motionX = mc.thePlayer.motionX;
        double motionY = mc.thePlayer.motionY;
        double motionZ = mc.thePlayer.motionZ;
        double planeTop = planeY + 1.0D;
        for (int tick = 0; tick < 10; tick++) {
            if (motionY <= 0.0D && predicted.minY <= planeTop + 0.08D) break;
            predicted = predicted.offset(motionX, motionY, motionZ);
            motionX *= 0.91D;
            motionZ *= 0.91D;
            motionY = (motionY - 0.08D) * 0.98D;
        }
        return predicted;
    }

    private void addFootprint(Set<BlockPos> output, AxisAlignedBB bounds, int y) {
        int minX = MathHelper.floor_double(bounds.minX + 0.001D);
        int maxX = MathHelper.floor_double(bounds.maxX - 0.001D);
        int minZ = MathHelper.floor_double(bounds.minZ + 0.001D);
        int maxZ = MathHelper.floor_double(bounds.maxZ - 0.001D);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) output.add(new BlockPos(x, y, z));
        }
    }

    private Vec3 hitPoint(BlockPos support, EnumFacing face) {
        double x = support.getX() + 0.5D + face.getFrontOffsetX() * 0.5D;
        double y = support.getY() + 0.5D + face.getFrontOffsetY() * 0.5D;
        double z = support.getZ() + 0.5D + face.getFrontOffsetZ() * 0.5D;
        if (multipoint.isToggled()) {
            Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
            if (face.getFrontOffsetX() == 0) x = clamp(eyes.xCoord, support.getX() + 0.18D, support.getX() + 0.82D);
            if (face.getFrontOffsetY() == 0) y = clamp(eyes.yCoord, support.getY() + 0.18D, support.getY() + 0.82D);
            if (face.getFrontOffsetZ() == 0) z = clamp(eyes.zCoord, support.getZ() + 0.18D, support.getZ() + 0.82D);
        }
        return new Vec3(x, y, z);
    }

    private boolean rotationRayHitsSupport(float yaw, float pitch, double reach,
                                           BlockPos support, EnumFacing expectedFace) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 look = vectorForRotation(pitch, yaw);
        Vec3 end = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
        MovingObjectPosition trace = mc.theWorld.rayTraceBlocks(eyes, end, false, true, false);
        return trace != null
                && trace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && support.equals(trace.getBlockPos())
                && trace.sideHit == expectedFace;
    }

    private static Vec3 vectorForRotation(float pitch, float yaw) {
        float yawCos = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float yawSin = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float pitchCos = -MathHelper.cos(-pitch * 0.017453292F);
        float pitchSin = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    private float[] rotationsTo(Vec3 point) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        double dx = point.xCoord - eyes.xCoord;
        double dy = point.yCoord - eyes.yCoord;
        double dz = point.zCoord - eyes.zCoord;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float rawYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float yaw = serverYaw + MathHelper.wrapAngleTo180_float(rawYaw - serverYaw);
        float pitch = MathHelper.clamp_float(
                (float) -Math.toDegrees(Math.atan2(dy, horizontal)), -90.0F, 90.0F);
        return new float[]{yaw, pitch};
    }

    private int resolveBlockSlot(PlacementTarget placement) {
        return resolveBlockSlot(placement.target, placement.support, placement.face);
    }

    private int resolveBlockSlot(BlockPos targetPosition, BlockPos support, EnumFacing face) {
        int current = mc.thePlayer.inventory.currentItem;
        ItemStack held = mc.thePlayer.inventory.getStackInSlot(current);
        if (canPlaceStack(held, targetPosition, support, face)) return current;
        if (selectBlocks.getMode() != SelectBlocks.Always) return -1;

        int bestSlot = -1;
        int bestSize = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (!canPlaceStack(stack, targetPosition, support, face) || stack.stackSize <= bestSize) continue;
            bestSlot = slot;
            bestSize = stack.stackSize;
        }
        return bestSlot;
    }

    private boolean canPlaceStack(ItemStack stack, BlockPos targetPosition,
                                  BlockPos support, EnumFacing face) {
        if (!BlockInteractionUtils.isPlaceableBlock(stack)) return false;
        ItemBlock item = (ItemBlock) stack.getItem();
        return item.canPlaceBlockOnSide(mc.theWorld, support, face, mc.thePlayer, stack)
                && mc.theWorld.canBlockBePlaced(item.getBlock(), targetPosition, false,
                face, mc.thePlayer, stack);
    }

    private boolean hasSelectableBlocks() {
        if (mc.thePlayer == null) return false;
        ItemStack held = mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem);
        if (BlockInteractionUtils.isPlaceableBlock(held)) return true;
        return selectBlocks.getMode() == SelectBlocks.Always
                && BlockInteractionUtils.findBlockSlot() >= 0;
    }

    private int countAirBelow(BlockPos first) {
        int air = 0;
        int limit = Math.max(12, (int) Math.round(minimumHeight.getInput()) + 2);
        for (int offset = 0; offset < limit; offset++) {
            if (!BlockInteractionUtils.isReplaceable(first.down(offset))) break;
            air++;
        }
        return air;
    }

    private boolean intersectsPlayer(BlockPos position) {
        AxisAlignedBB block = new AxisAlignedBB(position.getX(), position.getY(), position.getZ(),
                position.getX() + 1.0D, position.getY() + 1.0D, position.getZ() + 1.0D);
        return block.intersectsWith(mc.thePlayer.getEntityBoundingBox());
    }

    private boolean isSafeClickableSupport(BlockPos position) {
        if (!BlockInteractionUtils.canClick(position)) return false;
        Block block = mc.theWorld.getBlockState(position).getBlock();
        return !block.hasTileEntity();
    }

    private double distanceToTrajectory(BlockPos position) {
        AxisAlignedBB landing = predictBoundsAtPlane(position.getY());
        double centerX = (landing.minX + landing.maxX) * 0.5D;
        double centerZ = (landing.minZ + landing.maxZ) * 0.5D;
        return Math.hypot(position.getX() + 0.5D - centerX,
                position.getZ() + 0.5D - centerZ);
    }

    private boolean canRun() {
        return PlayerUtils.isPlayerInGame()
                && mc.currentScreen == null
                && mc.inGameHasFocus
                && mc.thePlayer.isEntityAlive()
                && !mc.thePlayer.isRiding()
                && !mc.thePlayer.capabilities.isFlying
                && !mc.thePlayer.isInWater()
                && !mc.thePlayer.isOnLadder();
    }

    private void fail(String reason) {
        log("aborted: " + reason);
        failedThisFlight = true;
        target = null;
        awaiting = null;
        incomingConfirmation.set(null);
        restoreHotbar();
        state = controlsServerRotation ? State.RESTORING : State.FAILED;
        setSuffix("Aborted");
    }

    private void finishSequence() {
        target = null;
        awaiting = null;
        incomingConfirmation.set(null);
        restoreHotbar();
        state = controlsServerRotation ? State.RESTORING : State.IDLE;
        if (!controlsServerRotation) silentRenderActive = false;
    }

    private void restoreHotbar() {
        if (!PlayerUtils.isPlayerInGame() || originalSlot < 0
                || mc.thePlayer.inventory.currentItem == originalSlot) {
            originalSlot = -1;
            return;
        }
        mc.thePlayer.inventory.currentItem = originalSlot;
        mc.playerController.updateController();
        mc.getItemRenderer().resetEquippedProgress();
        originalSlot = -1;
    }

    private void restoreImmediately() {
        restoreHotbar();
        if (!controlsServerRotation || !PlayerUtils.isPlayerInGame()) return;
        controlsServerRotation = false;
        silentRenderActive = false;
        failedThisFlight = false;
        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(
                mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround));
    }

    private void reset(boolean clearSupport) {
        state = State.IDLE;
        target = null;
        awaiting = null;
        incomingConfirmation.set(null);
        placedBlocks = 0;
        originalSlot = -1;
        controlsServerRotation = false;
        silentRenderActive = false;
        if (clearSupport) takeoffSupport = null;
    }

    private void writeRotation(C03PacketPlayer packet, float yaw, float pitch) {
        try {
            IC03PacketPlayer mutable = (IC03PacketPlayer) packet;
            mutable.setYaw(yaw);
            mutable.setPitch(pitch);
            mutable.setRotating(true);
        } catch (ClassCastException exception) {
            fail("movement packet is not mutable");
        }
    }

    private void setRenderRotation(float yaw, float pitch) {
        silentRenderYaw = yaw;
        silentRenderPitch = pitch;
        silentRenderActive = true;
    }

    private float quantizeRotation(float target, float base) {
        float sensitivity = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = sensitivity * sensitivity * sensitivity * 8.0F * 0.15F;
        if (gcd <= 0.0001F) return target;
        return base + Math.round((target - base) / gcd) * gcd;
    }

    private static float approachAngle(float current, float target, float maximum) {
        float difference = MathHelper.wrapAngleTo180_float(target - current);
        return current + MathHelper.clamp_float(difference, -maximum, maximum);
    }

    private static float approachLinear(float current, float target, float maximum) {
        return current + MathHelper.clamp_float(target - current, -maximum, maximum);
    }

    private static float angleDistance(float first, float second) {
        return Math.abs(MathHelper.wrapAngleTo180_float(second - first));
    }

    private static boolean horizontalIntersects(AxisAlignedBB first, AxisAlignedBB second) {
        return first.maxX > second.minX && first.minX < second.maxX
                && first.maxZ > second.minZ && first.minZ < second.maxZ;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private void log(String message) {
        System.out.println("[Veyra/SilentClutch #" + fallId + "] " + message);
    }

    public static boolean hasSilentRenderRotation() {
        return silentRenderActive;
    }

    public static float getSilentRenderYaw() {
        return silentRenderYaw;
    }

    public static float getSilentRenderPitch() {
        return silentRenderPitch;
    }

    @Override
    public void onEnable() {
        reset(true);
        airborneSinceTick = clientTick;
        setSuffix("Ready");
        PlayerUtils.sendMessageToSelf("&aSilent Clutch enabled &7(server-confirmed)");
    }

    @Override
    public void onDisable() {
        restoreImmediately();
        reset(true);
        setSuffix(null);
        PlayerUtils.sendMessageToSelf("&cSilent Clutch disabled");
    }

    public enum SelectBlocks {
        No("No"),
        OnDepletion("On depletion"),
        Always("Always");

        private final String label;

        SelectBlocks(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private enum State {
        IDLE,
        AIMING,
        PLACE_PENDING,
        AWAITING_CONFIRMATION,
        RESTORING,
        FAILED
    }

    private static final class SupportLevel {
        private final int blockY;
        private final double topY;

        private SupportLevel(int blockY, double topY) {
            this.blockY = blockY;
            this.topY = topY;
        }
    }

    private static final class PendingPlacement {
        private final BlockPos position;
        private final long sentTick;

        private PendingPlacement(BlockPos position, long sentTick) {
            this.position = position;
            this.sentTick = sentTick;
        }
    }

    private static final class Confirmation {
        private final BlockPos position;
        private final long sentTick;
        private final boolean accepted;

        private Confirmation(BlockPos position, long sentTick, boolean accepted) {
            this.position = position;
            this.sentTick = sentTick;
            this.accepted = accepted;
        }

        private boolean matches(PendingPlacement pending) {
            return position.equals(pending.position) && sentTick == pending.sentTick;
        }
    }

    private static final class PlacementTarget implements Comparable<PlacementTarget> {
        private final BlockPos target;
        private final BlockPos support;
        private final EnumFacing face;
        private final Vec3 hitVec;
        private final float yaw;
        private final float pitch;
        private final double trajectoryDistance;
        private final double reachDistance;

        private PlacementTarget(BlockPos target, BlockPos support, EnumFacing face,
                                Vec3 hitVec, float yaw, float pitch,
                                double trajectoryDistance, double reachDistance) {
            this.target = target;
            this.support = support;
            this.face = face;
            this.hitVec = hitVec;
            this.yaw = yaw;
            this.pitch = pitch;
            this.trajectoryDistance = trajectoryDistance;
            this.reachDistance = reachDistance;
        }

        @Override
        public int compareTo(PlacementTarget other) {
            int byTrajectory = Double.compare(trajectoryDistance, other.trajectoryDistance);
            if (byTrajectory != 0) return byTrajectory;
            return Double.compare(reachDistance, other.reachDistance);
        }
    }
}
