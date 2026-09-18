package dev.veyra.client.player.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeSupportTrackerTest {
    private final BridgeSupportTracker tracker = new BridgeSupportTracker();

    @Test
    void attemptedPlacementRemainsUnsafeUntilAcceptedByServer() {
        tracker.recordAttempt(4, 63, -2);
        assertTrue(tracker.hasUnconfirmed());
        assertTrue(tracker.isUnconfirmedCollision(4, 63, -2, 5, 64, -1));

        tracker.confirm(4, 63, -2, true);
        assertFalse(tracker.hasUnconfirmed());
        assertFalse(tracker.isUnconfirmedCollision(4, 63, -2, 5, 64, -1));
    }

    @Test
    void rejectedPlacementStaysUnsafeForRetry() {
        tracker.recordAttempt(1, 10, 1);
        tracker.confirm(1, 10, 1, false);

        assertTrue(tracker.isUnconfirmed(1, 10, 1));
    }

    @Test
    void unrelatedExistingBlockCollisionIsNeverMarkedAsGhostSupport() {
        tracker.recordAttempt(10, 10, 10);

        assertFalse(tracker.isUnconfirmedCollision(0, 0, 0, 1, 1, 1));
    }
}
