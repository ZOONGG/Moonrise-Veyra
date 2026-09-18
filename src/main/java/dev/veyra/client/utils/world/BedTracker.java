package dev.veyra.client.utils.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Incrementally discovers bed heads without stalling the render thread.
 *
 * The scan walks outwards from the player, so close beds are discovered first.
 * Results are retained while a new scan is running to avoid ESP flicker whenever
 * the player crosses the scan recenter threshold.
 */
public final class BedTracker {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int DEFAULT_RADIUS = 32;
    private static final int MAX_RADIUS = 128;
    private static final int VERTICAL_RADIUS = 16;
    // World#getBlockState is a main-thread operation. The previous 12k/tick
    // budget produced 100-1100 ms frame stalls and restarted forever after a
    // complete pass. A bounded incremental pass keeps combat ticks responsive.
    private static final int BLOCKS_PER_TICK = 2048;
    private static final double RECENTER_DISTANCE_SQ = 64.0D;

    private static final Set<BlockPos> beds = new LinkedHashSet<>();
    private static World trackedWorld;
    private static BlockPos scanCenter;
    private static int activeRadius = DEFAULT_RADIUS;
    private static int[] spiralX = new int[0];
    private static int[] spiralZ = new int[0];
    private static int cursor;
    private static int lastTick = Integer.MIN_VALUE;

    private BedTracker() {
    }

    public static void update() {
        update(DEFAULT_RADIUS);
    }

    public static void update(int requestedRadius) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            reset();
            return;
        }

        int clampedRadius = Math.max(DEFAULT_RADIUS, Math.min(MAX_RADIUS, requestedRadius));
        if (clampedRadius != activeRadius) {
            activeRadius = clampedRadius;
            rebuildSpiral();
            cursor = 0;
        }

        if (lastTick == mc.thePlayer.ticksExisted) return;
        lastTick = mc.thePlayer.ticksExisted;

        BlockPos playerPos = mc.thePlayer.getPosition();
        if (trackedWorld != mc.theWorld) {
            trackedWorld = mc.theWorld;
            beds.clear();
            scanCenter = playerPos;
            activeRadius = clampedRadius;
            rebuildSpiral();
            cursor = 0;
        } else if (scanCenter == null || horizontalDistanceSq(scanCenter, playerPos) > RECENTER_DISTANCE_SQ) {
            scanCenter = playerPos;
            cursor = 0;
        }

        pruneBeds(playerPos);
        scanNextBatch();
    }

    public static List<BedEntry> getBeds() {
        if (trackedWorld == null || beds.isEmpty()) return Collections.emptyList();
        List<BedEntry> result = new ArrayList<>(beds.size());
        for (BlockPos head : beds) {
            BlockPos foot = findFoot(head);
            if (foot != null) result.add(new BedEntry(head, foot));
        }
        return Collections.unmodifiableList(result);
    }

    public static AxisAlignedBB getBounds(BedEntry bed) {
        int minX = Math.min(bed.head().getX(), bed.foot().getX());
        int minZ = Math.min(bed.head().getZ(), bed.foot().getZ());
        int maxX = Math.max(bed.head().getX(), bed.foot().getX()) + 1;
        int maxZ = Math.max(bed.head().getZ(), bed.foot().getZ()) + 1;
        double y = bed.head().getY();
        // BlockBed's collision/selection height in 1.8.9 is 9/16 of a block.
        return new AxisAlignedBB(minX, y, minZ, maxX, y + 0.5625D, maxZ);
    }

    public static AxisAlignedBB getPartBounds(BlockPos bedPart) {
        if (bedPart == null) return null;
        return new AxisAlignedBB(
                bedPart.getX(), bedPart.getY(), bedPart.getZ(),
                bedPart.getX() + 1.0D, bedPart.getY() + 0.5625D, bedPart.getZ() + 1.0D);
    }

    /**
     * Collects protection materials around both halves of a bed. Entries retain
     * block metadata so colored wool, clay and glass render with the right icon.
     */
    public static List<ProtectionEntry> getProtection(BedEntry bed, int radius) {
        if (mc.theWorld == null || bed == null) return Collections.emptyList();
        int clampedRadius = Math.max(1, Math.min(6, radius));
        int minX = Math.min(bed.head().getX(), bed.foot().getX()) - clampedRadius;
        int maxX = Math.max(bed.head().getX(), bed.foot().getX()) + clampedRadius;
        int minZ = Math.min(bed.head().getZ(), bed.foot().getZ()) - clampedRadius;
        int maxZ = Math.max(bed.head().getZ(), bed.foot().getZ()) + clampedRadius;
        int bedY = bed.head().getY();
        int minY = bedY;
        int maxY = bedY + Math.min(4, clampedRadius);

        Map<ProtectionKey, MutableProtection> found = new LinkedHashMap<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!isProtectionShellPosition(pos, bed, clampedRadius)) continue;
                    IBlockState state = mc.theWorld.getBlockState(pos);
                    Block block = state.getBlock();
                    if (!isBedwarsDefenseMaterial(block)) continue;
                    Item item = Item.getItemFromBlock(block);
                    if (item == null) continue;

                    // Block-state metadata may contain placement-only bits (for
                    // example the upper-half bit on slabs). Those values have no
                    // corresponding item model and render as a pink/black cube.
                    int metadata = block.damageDropped(state);
                    int layer = Math.min(layerFrom(pos, bed.head()), layerFrom(pos, bed.foot()));
                    ProtectionKey key = new ProtectionKey(block, metadata);
                    MutableProtection value = found.get(key);
                    if (value == null) {
                        found.put(key, new MutableProtection(block, metadata, layer));
                    } else {
                        value.count++;
                        value.nearestLayer = Math.min(value.nearestLayer, layer);
                    }
                }
            }
        }

        List<ProtectionEntry> result = new ArrayList<>(found.size());
        for (MutableProtection entry : found.values()) {
            ItemStack stack = new ItemStack(entry.block, Math.min(64, entry.count), entry.metadata);
            result.add(new ProtectionEntry(stack, entry.count, entry.nearestLayer));
        }
        result.sort(Comparator.comparingInt(ProtectionEntry::layer)
                .thenComparing(Comparator.comparingInt(ProtectionEntry::count).reversed()));
        return Collections.unmodifiableList(result);
    }

    /**
     * Bed plates describe purchasable BedWars defense layers, not every solid
     * map block inside the detection cube. Keeping this list explicit prevents
     * leaves, terrain, ores and decorative blocks from appearing as defenses.
     */
    static boolean isBedwarsDefenseMaterial(Block block) {
        return block == Blocks.wool
                || block == Blocks.stained_hardened_clay
                || block == Blocks.hardened_clay
                || block == Blocks.stained_glass
                || block == Blocks.glass
                || block == Blocks.planks
                || block == Blocks.end_stone
                || block == Blocks.obsidian;
    }

    /**
     * Only examines the build envelope around the two bed blocks. The support
     * floor is one block below bedY and is deliberately never sampled. Higher
     * layers narrow toward the bed, matching normal BedWars defenses instead
     * of treating nearby map walls as another protection material.
     */
    private static boolean isProtectionShellPosition(BlockPos pos, BedEntry bed, int radius) {
        int dy = pos.getY() - bed.head().getY();
        if (dy < 0) return false;
        int horizontal = Math.min(horizontalChebyshev(pos, bed.head()),
                horizontalChebyshev(pos, bed.foot()));
        if (horizontal == 0 && dy == 0) return false;
        int allowedHorizontal = Math.max(1, radius - Math.max(0, dy - 1));
        return horizontal <= allowedHorizontal;
    }

    private static int horizontalChebyshev(BlockPos first, BlockPos second) {
        return Math.max(Math.abs(first.getX() - second.getX()),
                Math.abs(first.getZ() - second.getZ()));
    }

    public static void reset() {
        trackedWorld = null;
        scanCenter = null;
        activeRadius = DEFAULT_RADIUS;
        spiralX = new int[0];
        spiralZ = new int[0];
        cursor = 0;
        lastTick = Integer.MIN_VALUE;
        beds.clear();
    }

    private static void scanNextBatch() {
        if (scanCenter == null) return;
        if (spiralX.length == 0) rebuildSpiral();
        int horizontalSpan = spiralX.length;
        int verticalSpan = VERTICAL_RADIUS * 2 + 1;
        int volume = horizontalSpan * verticalSpan;
        int end = Math.min(volume, cursor + BLOCKS_PER_TICK);
        for (; cursor < end; cursor++) {
            // Scan a complete Y layer before moving vertically. BedWars beds
            // are normally close to the player's Y level, so even a bed at the
            // edge of loaded chunks is discovered in roughly one second rather
            // than after every vertical position in every nearer column.
            int verticalIndex = cursor / horizontalSpan;
            int horizontalIndex = cursor % horizontalSpan;
            int yOffset = alternatingOffset(verticalIndex);
            BlockPos pos = scanCenter.add(spiralX[horizontalIndex], yOffset, spiralZ[horizontalIndex]);
            if (!trackedWorld.isBlockLoaded(pos, false)) continue;
            IBlockState state = trackedWorld.getBlockState(pos);
            if (state.getBlock() == Blocks.bed && isHead(state)) beds.add(pos);
        }
        // Do not immediately rescan the same half-million blocks. Destruction
        // is handled by pruneBeds; moving eight blocks recentres a fresh pass.
        if (cursor >= volume) cursor = volume;
    }

    private static void pruneBeds(BlockPos playerPos) {
        double maxDistanceSq = (activeRadius + 8.0D) * (activeRadius + 8.0D);
        Iterator<BlockPos> iterator = beds.iterator();
        while (iterator.hasNext()) {
            BlockPos head = iterator.next();
            if (horizontalDistanceSq(head, playerPos) > maxDistanceSq
                    || trackedWorld.getBlockState(head).getBlock() != Blocks.bed
                    || !isHead(trackedWorld.getBlockState(head))) {
                iterator.remove();
            }
        }
    }

    private static BlockPos findFoot(BlockPos head) {
        IBlockState state = trackedWorld.getBlockState(head);
        if (state.getBlock() != Blocks.bed || !isHead(state)) return null;
        int direction = Blocks.bed.getMetaFromState(state) & 3;
        int dx;
        int dz;
        switch (direction) {
            case 0 -> { dx = 0; dz = 1; }  // south
            case 1 -> { dx = -1; dz = 0; } // west
            case 2 -> { dx = 0; dz = -1; } // north
            default -> { dx = 1; dz = 0; } // east
        }
        BlockPos foot = head.add(-dx, 0, -dz);
        return trackedWorld.getBlockState(foot).getBlock() == Blocks.bed ? foot : null;
    }

    private static boolean isHead(IBlockState state) {
        return (Blocks.bed.getMetaFromState(state) & 8) != 0;
    }

    private static int layerFrom(BlockPos pos, BlockPos bedPart) {
        return Math.max(Math.max(Math.abs(pos.getX() - bedPart.getX()),
                Math.abs(pos.getY() - bedPart.getY())), Math.abs(pos.getZ() - bedPart.getZ()));
    }

    private static int alternatingOffset(int index) {
        if (index == 0) return 0;
        int magnitude = (index + 1) / 2;
        return (index & 1) == 1 ? -magnitude : magnitude;
    }

    private static double horizontalDistanceSq(BlockPos first, BlockPos second) {
        double x = first.getX() - second.getX();
        double z = first.getZ() - second.getZ();
        return x * x + z * z;
    }

    private static void rebuildSpiral() {
        int side = activeRadius * 2 + 1;
        spiralX = new int[side * side];
        spiralZ = new int[side * side];
        int index = 0;
        spiralX[index] = 0;
        spiralZ[index++] = 0;
        for (int radius = 1; radius <= activeRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                spiralX[index] = x;
                spiralZ[index++] = -radius;
            }
            for (int z = -radius + 1; z <= radius; z++) {
                spiralX[index] = radius;
                spiralZ[index++] = z;
            }
            for (int x = radius - 1; x >= -radius; x--) {
                spiralX[index] = x;
                spiralZ[index++] = radius;
            }
            for (int z = radius - 1; z > -radius; z--) {
                spiralX[index] = -radius;
                spiralZ[index++] = z;
            }
        }
    }

    public record BedEntry(BlockPos head, BlockPos foot) {
        public BlockPos centerBlock() {
            return head;
        }
    }

    public record ProtectionEntry(ItemStack stack, int count, int layer) {
    }

    private record ProtectionKey(Block block, int metadata) {
    }

    private static final class MutableProtection {
        private final Block block;
        private final int metadata;
        private int count = 1;
        private int nearestLayer;

        private MutableProtection(Block block, int metadata, int nearestLayer) {
            this.block = block;
            this.metadata = metadata;
            this.nearestLayer = nearestLayer;
        }
    }
}
