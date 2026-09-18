package dev.veyra.client.player.bridge;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Blocks movement onto client-side placements until the server confirms them. */
public final class BridgeSupportTracker {
    private final Set<BlockKey> unconfirmed = ConcurrentHashMap.newKeySet();

    public void recordAttempt(int x, int y, int z) {
        unconfirmed.add(new BlockKey(x, y, z));
    }

    public void confirm(int x, int y, int z, boolean accepted) {
        BlockKey position = new BlockKey(x, y, z);
        if (accepted) {
            unconfirmed.remove(position);
        } else {
            // Keep rejected positions unsafe. A later successful retry for the
            // same position removes it when its non-air update arrives.
            unconfirmed.add(position);
        }
    }

    public boolean isUnconfirmedCollision(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        for (BlockKey position : unconfirmed) {
            if (position.x + 1.0D > minX && position.x < maxX
                    && position.y + 1.0D > minY && position.y < maxY
                    && position.z + 1.0D > minZ && position.z < maxZ) {
                return true;
            }
        }
        return false;
    }

    public boolean isUnconfirmed(int x, int y, int z) {
        return unconfirmed.contains(new BlockKey(x, y, z));
    }

    public boolean hasUnconfirmed() {
        return !unconfirmed.isEmpty();
    }

    public void clear() {
        unconfirmed.clear();
    }

    private record BlockKey(int x, int y, int z) {
    }
}
