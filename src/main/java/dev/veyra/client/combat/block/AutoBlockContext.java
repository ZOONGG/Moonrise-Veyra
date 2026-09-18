package dev.veyra.client.combat.block;

/** Runtime facts supplied by Minecraft; no game classes leak into the engine. */
public record AutoBlockContext(
        long nowMs,
        boolean basicValid,
        boolean canInjectBlock,
        boolean threatPresent,
        boolean physicalUseDown,
        boolean leftMouseDown,
        boolean rightMouseDown,
        boolean recentlyDamaged,
        boolean packetLagAvailable,
        int blockChanceRoll,
        int lagChanceRoll
) {
    public AutoBlockContext {
        blockChanceRoll = clampRoll(blockChanceRoll);
        lagChanceRoll = clampRoll(lagChanceRoll);
    }

    public static AutoBlockContext eligible(long nowMs, int blockChanceRoll, int lagChanceRoll) {
        return new AutoBlockContext(nowMs, true, true, true, false,
                false, false, false, true, blockChanceRoll, lagChanceRoll);
    }

    public AutoBlockContext withBasicValid(boolean value) {
        return copy(nowMs, value, canInjectBlock, threatPresent, physicalUseDown,
                leftMouseDown, rightMouseDown, recentlyDamaged, packetLagAvailable);
    }

    public AutoBlockContext withPhysicalUse(boolean value) {
        return copy(nowMs, basicValid, !value, threatPresent, value,
                leftMouseDown, rightMouseDown, recentlyDamaged, packetLagAvailable);
    }

    public AutoBlockContext withLeftMouse(boolean value) {
        return copy(nowMs, basicValid, canInjectBlock, threatPresent, physicalUseDown,
                value, rightMouseDown, recentlyDamaged, packetLagAvailable);
    }

    public AutoBlockContext withRecentDamage(boolean value) {
        return copy(nowMs, basicValid, canInjectBlock, threatPresent, physicalUseDown,
                leftMouseDown, rightMouseDown, value, packetLagAvailable);
    }

    public AutoBlockContext withPacketLagAvailable(boolean value) {
        return copy(nowMs, basicValid, canInjectBlock, threatPresent, physicalUseDown,
                leftMouseDown, rightMouseDown, recentlyDamaged, value);
    }

    private AutoBlockContext copy(long time, boolean valid, boolean canBlock, boolean threat,
                                  boolean physicalUse, boolean leftMouse, boolean rightMouse,
                                  boolean damaged, boolean lagAvailable) {
        return new AutoBlockContext(time, valid, canBlock, threat, physicalUse,
                leftMouse, rightMouse, damaged, lagAvailable,
                blockChanceRoll, lagChanceRoll);
    }

    private static int clampRoll(int value) {
        return Math.max(0, Math.min(99, value));
    }
}
