package dev.veyra.client.utils.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSlime;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

public final class BlockInteractionUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private BlockInteractionUtils() {
    }

    public static int findBestToolSlot(BlockPos pos) {
        if (mc.thePlayer == null || mc.theWorld == null || pos == null) return -1;
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        int bestSlot = mc.thePlayer.inventory.currentItem;
        float bestSpeed = toolSpeed(mc.thePlayer.inventory.getStackInSlot(bestSlot), block);

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            float speed = toolSpeed(stack, block);
            if (speed > bestSpeed + 0.001F) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    public static int findBlockSlot() {
        if (mc.thePlayer == null) return -1;
        int bestSlot = -1;
        int largestStack = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (!isPlaceableBlock(stack)) continue;
            if (stack.stackSize > largestStack) {
                largestStack = stack.stackSize;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    public static boolean selectHotbarSlot(int slot) {
        if (mc.thePlayer == null || slot < 0 || slot > 8) return false;
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        if (!isPlaceableBlock(stack)) return false;
        if (mc.thePlayer.inventory.currentItem != slot) {
            mc.thePlayer.inventory.currentItem = slot;
            mc.playerController.updateController();
            mc.getItemRenderer().resetEquippedProgress();
        }
        return true;
    }

    public static boolean placeBlock(BlockPos support, EnumFacing side, Vec3 hitVec, int slot) {
        if (mc.thePlayer == null || mc.theWorld == null || support == null || side == null || hitVec == null
                || !canClick(support) || !selectHotbarSlot(slot)) {
            return false;
        }

        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, stack,
                support, side, hitVec)) {
            mc.thePlayer.swingItem();
            mc.getItemRenderer().resetEquippedProgress();
            return true;
        }
        return false;
    }

    public static boolean placeBlock(BlockPos target, boolean sidewaysOnly) {
        if (mc.thePlayer == null || mc.theWorld == null || target == null || !isReplaceable(target)) return false;
        int slot = findBlockSlot();
        if (slot < 0) return false;

        int previousSlot = mc.thePlayer.inventory.currentItem;
        if (!selectHotbarSlot(slot)) return false;

        EnumFacing[] facings = sidewaysOnly
                ? new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST}
                : new EnumFacing[]{EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH,
                EnumFacing.WEST, EnumFacing.EAST, EnumFacing.UP};

        for (EnumFacing side : facings) {
            BlockPos neighbour = target.offset(side.getOpposite());
            if (!canClick(neighbour)) continue;

            Vec3iOffset offset = new Vec3iOffset(side);
            Vec3 hitVec = new Vec3(
                    neighbour.getX() + 0.5D + offset.x * 0.5D,
                    neighbour.getY() + 0.5D + offset.y * 0.5D,
                    neighbour.getZ() + 0.5D + offset.z * 0.5D);
            if (placeBlock(neighbour, side, hitVec, slot)) return true;
        }

        mc.thePlayer.inventory.currentItem = previousSlot;
        mc.playerController.updateController();
        return false;
    }

    public static boolean isReplaceable(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block.getMaterial().isReplaceable() || block instanceof BlockLiquid;
    }

    public static boolean canClick(BlockPos pos) {
        if (mc.theWorld == null || pos == null) return false;
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return !block.getMaterial().isReplaceable() && !(block instanceof BlockLiquid);
    }

    public static boolean isPlaceableBlock(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemBlock itemBlock)) return false;
        Block block = itemBlock.getBlock();
        return block.isFullBlock()
                && !(block instanceof BlockTNT)
                && !(block instanceof BlockSlime)
                && !(block instanceof BlockFalling);
    }

    private static float toolSpeed(ItemStack stack, Block block) {
        if (stack == null) return 1.0F;
        float speed = stack.getStrVsBlock(block);
        if (speed > 1.0F) {
            int efficiency = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);
            if (efficiency > 0) speed += efficiency * efficiency + 1.0F;
        }
        return speed;
    }

    private static final class Vec3iOffset {
        private final int x;
        private final int y;
        private final int z;

        private Vec3iOffset(EnumFacing facing) {
            x = facing.getFrontOffsetX();
            y = facing.getFrontOffsetY();
            z = facing.getFrontOffsetZ();
        }
    }
}
