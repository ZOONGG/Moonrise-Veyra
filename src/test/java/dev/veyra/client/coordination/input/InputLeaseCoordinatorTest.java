package dev.veyra.client.coordination.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputLeaseCoordinatorTest {
    private final InputLeaseCoordinator coordinator = new InputLeaseCoordinator();

    @Test
    void finalReleaseRestoresTheCurrentPhysicalState() {
        coordinator.set("bridge", 42, true, 10);
        assertTrue(coordinator.resolve(42, false));

        coordinator.release("bridge", 42);

        assertFalse(coordinator.resolve(42, false));
        assertTrue(coordinator.resolve(42, true));
    }

    @Test
    void higherPriorityLeaseCanTemporarilyForceAKeyReleased() {
        coordinator.set("autoblock", -99, true, 10);
        coordinator.set("attack-gap", -99, false, 100);

        assertFalse(coordinator.resolve(-99, true));
        coordinator.release("attack-gap", -99);
        assertTrue(coordinator.resolve(-99, false));
    }

    @Test
    void releasingOneOwnerNeverReleasesAnotherOwnersLease() {
        coordinator.set("bridge", 42, true, 10);
        coordinator.set("clutch", 42, true, 20);

        coordinator.releaseOwner("bridge");

        assertTrue(coordinator.resolve(42, false));
        coordinator.releaseOwner("clutch");
        assertFalse(coordinator.resolve(42, false));
    }

    @Test
    void mostRecentEqualPriorityLeaseWinsDeterministically() {
        coordinator.set("first", 1, true, 10);
        coordinator.set("second", 1, false, 10);

        assertFalse(coordinator.resolve(1, true));
        coordinator.set("first", 1, true, 10);
        assertTrue(coordinator.resolve(1, false));
    }
}
