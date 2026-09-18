package dev.veyra.client.player.bridge;

/** Runtime facts supplied by Minecraft; the timing engine stays game-independent. */
public record BridgeAssistContext(
        long nowMs,
        boolean valid,
        boolean edgeReached,
        boolean emergencyEdgeReached,
        boolean jumpNeedsSneak,
        boolean physicalSneakDown
) {
    public BridgeAssistContext(
            long nowMs,
            boolean valid,
            boolean edgeReached,
            boolean jumpNeedsSneak,
            boolean physicalSneakDown
    ) {
        this(nowMs, valid, edgeReached, edgeReached, jumpNeedsSneak, physicalSneakDown);
    }
}
