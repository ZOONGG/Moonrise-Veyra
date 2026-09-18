package dev.veyra.client.player.bridge;

/**
 * Owns only the sneak state injected by Bridge Assist. A completed placement
 * releases after a short delay and cannot re-trigger until the old edge clears.
 */
public final class BridgeAssistEngine {
    private boolean ownsSneak;
    private boolean releasingAfterPlacement;
    private boolean fastPassArmed;
    private boolean fastPassSawEdge;
    private long releaseAtMs;

    public boolean tick(BridgeAssistContext context, BridgeAssistSettings settings) {
        if (!context.valid()) {
            reset();
            return false;
        }

        if (context.physicalSneakDown()) {
            ownsSneak = false;
            releasingAfterPlacement = false;
            return false;
        }

        if (releasingAfterPlacement) {
            // A placement packet is only an attempt. Never release the owned
            // sneak while the projected feet probe still sees an edge; the
            // client world must contain support first, regardless of settings.
            if (context.edgeReached() || context.nowMs() < releaseAtMs) {
                ownsSneak = true;
                return true;
            }
            ownsSneak = false;
            releasingAfterPlacement = false;
            fastPassArmed = settings.adaptiveTwoBlockRhythm();
            fastPassSawEdge = false;
            return false;
        }

        if (fastPassArmed) {
            return tickFastPass(context, settings);
        }

        ownsSneak = context.edgeReached()
                || (settings.sneakOnJump() && context.jumpNeedsSneak());
        return ownsSneak;
    }

    private boolean tickFastPass(
            BridgeAssistContext context,
            BridgeAssistSettings settings
    ) {
        if (context.edgeReached()) fastPassSawEdge = true;

        if (fastPassSawEdge && !context.edgeReached()) {
            fastPassArmed = false;
            fastPassSawEdge = false;
        }
        ownsSneak = settings.sneakOnJump() && context.jumpNeedsSneak();
        return ownsSneak;
    }

    public void onBlockPlaced(long nowMs, BridgeAssistSettings settings) {
        if (!ownsSneak) return;
        releasingAfterPlacement = true;
        releaseAtMs = nowMs + settings.unsneakDelayMs();
    }

    public boolean ownsSneak() {
        return ownsSneak;
    }

    /** True while releasing early would discard an active safety cycle. */
    public boolean requiresSafetyContinuation() {
        return ownsSneak || releasingAfterPlacement || fastPassArmed;
    }

    public boolean fastPassArmed() {
        return fastPassArmed;
    }

    /** A placement attempt during the no-sneak pass can complete that pass. */
    public void onFastPassPlacementAttempt() {
        if (fastPassArmed) fastPassSawEdge = true;
    }

    public void reset() {
        ownsSneak = false;
        releasingAfterPlacement = false;
        fastPassArmed = false;
        fastPassSawEdge = false;
        releaseAtMs = 0L;
    }
}
