package dev.veyra.client.coordination.packet;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketQueueCoordinatorTest {
    private final PacketQueueCoordinator<String> coordinator = new PacketQueueCoordinator<>(3);

    @Test
    void highestPriorityOwnerHasExclusiveCapture() {
        coordinator.acquire("fake-lag", 20);
        assertTrue(coordinator.isActive("fake-lag"));

        coordinator.acquire("blink", 100);

        assertTrue(coordinator.isActive("blink"));
        assertFalse(coordinator.isActive("fake-lag"));
        assertFalse(coordinator.enqueue("fake-lag", "movement"));
        assertTrue(coordinator.enqueue("blink", "movement"));
    }

    @Test
    void releasingHigherPriorityOwnerHandsCaptureBack() {
        coordinator.acquire("fake-lag", 20);
        coordinator.acquire("blink", 100);

        assertEquals(List.of(), coordinator.release("blink"));

        assertTrue(coordinator.isActive("fake-lag"));
    }

    @Test
    void ownerCanDrainOnlyItsOwnPackets() {
        coordinator.acquire("blink", 100);
        coordinator.enqueue("blink", "one");
        coordinator.enqueue("blink", "two");

        assertEquals(List.of(), coordinator.drain("fake-lag"));
        assertEquals(List.of("one", "two"), coordinator.drain("blink"));
    }

    @Test
    void queueLimitRejectsAdditionalPacketsWithoutDroppingExistingOrder() {
        coordinator.acquire("autoblock", 50);
        assertTrue(coordinator.enqueue("autoblock", "one"));
        assertTrue(coordinator.enqueue("autoblock", "two"));
        assertTrue(coordinator.enqueue("autoblock", "three"));
        assertFalse(coordinator.enqueue("autoblock", "four"));

        assertEquals(List.of("one", "two", "three"), coordinator.drain("autoblock"));
    }

    @Test
    void drainingDuePrefixPreservesLaterPacketsInOrder() {
        coordinator.acquire("fake-lag", 20);
        coordinator.enqueue("fake-lag", "due-one");
        coordinator.enqueue("fake-lag", "due-two");
        coordinator.enqueue("fake-lag", "later");

        assertEquals(List.of("due-one", "due-two"),
                coordinator.drainWhile("fake-lag", packet -> packet.startsWith("due")));
        assertEquals(List.of("later"), coordinator.drain("fake-lag"));
    }

    @Test
    void availabilityRespectsActivePriorityWithoutTakingOwnership() {
        coordinator.acquire("fake-lag", 50);

        assertFalse(coordinator.canAcquire("autoblock", 25));
        assertTrue(coordinator.canAcquire("blink", 100));
        assertEquals("fake-lag", coordinator.activeOwner());
    }

    @Test
    void ownerRegistrationCanBeInspectedIndependentlyOfActivity() {
        coordinator.acquire("fake-lag", 50);
        coordinator.acquire("blink", 100);

        assertTrue(coordinator.hasOwner("fake-lag"));
        assertFalse(coordinator.isActive("fake-lag"));
    }
}
