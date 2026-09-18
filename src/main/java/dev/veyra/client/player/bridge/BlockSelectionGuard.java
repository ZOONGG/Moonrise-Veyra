package dev.veyra.client.player.bridge;

/** Gives explicit player hotbar changes priority over automatic block selection. */
public final class BlockSelectionGuard {
    private int observedSlot = -1;
    private boolean manualOverride;

    public boolean allowAutomaticSelection(int currentSlot, boolean holdingBlock) {
        if (observedSlot >= 0 && currentSlot != observedSlot) {
            manualOverride = !holdingBlock;
        } else if (holdingBlock) {
            manualOverride = false;
        }
        observedSlot = currentSlot;
        return !manualOverride;
    }

    public void recordAutomaticSelection(int slot) {
        observedSlot = slot;
        manualOverride = false;
    }

    public void reset() {
        observedSlot = -1;
        manualOverride = false;
    }
}
