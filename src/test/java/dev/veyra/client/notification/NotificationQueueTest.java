package dev.veyra.client.notification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationQueueTest {
    @Test
    void limitsVisibleNotificationsAndPromotesQueuedItems() {
        NotificationQueue queue = new NotificationQueue(3, 1_700L);

        queue.post("Blink", true, 0L);
        queue.post("Timer", true, 0L);
        queue.post("FastPlace", true, 0L);
        queue.post("Freecam", true, 0L);

        assertEquals(List.of("Blink", "Timer", "FastPlace"), names(queue.visibleAt(0L)));
        assertEquals(List.of("Freecam"), names(queue.visibleAt(1_701L)));
    }

    @Test
    void replacesStaleStateForTheSameModule() {
        NotificationQueue queue = new NotificationQueue(3, 1_700L);

        queue.post("Blink", true, 0L);
        queue.post("Blink", false, 100L);

        List<NotificationQueue.Entry> visible = queue.visibleAt(100L);
        assertEquals(1, visible.size());
        assertEquals("Blink", visible.get(0).moduleName());
        assertFalse(visible.get(0).enabled());
        assertEquals(100L, visible.get(0).startedAtMs());
    }

    @Test
    void replacementStateTakesTheFreedVisibleSlotBeforeOlderQueuedItems() {
        NotificationQueue queue = new NotificationQueue(3, 1_700L);
        queue.post("Blink", true, 0L);
        queue.post("Timer", true, 0L);
        queue.post("FastPlace", true, 0L);
        queue.post("Freecam", true, 0L);

        queue.post("Blink", false, 100L);

        assertEquals(List.of("Timer", "FastPlace", "Blink"), names(queue.visibleAt(100L)));
        assertFalse(queue.visibleAt(100L).get(2).enabled());
    }

    @Test
    void keepsEntryVisibleUntilItsFullLifetimeEnds() {
        NotificationQueue queue = new NotificationQueue(3, 1_700L);
        queue.post("Timer", true, 50L);

        assertTrue(queue.visibleAt(1_749L).stream().anyMatch(entry -> entry.moduleName().equals("Timer")));
        assertTrue(queue.visibleAt(1_750L).stream().anyMatch(entry -> entry.moduleName().equals("Timer")));
        assertFalse(queue.visibleAt(1_751L).stream().anyMatch(entry -> entry.moduleName().equals("Timer")));
    }

    @Test
    void clearDropsVisibleAndPendingNotifications() {
        NotificationQueue queue = new NotificationQueue(1, 1_700L);
        queue.post("Blink", true, 0L);
        queue.post("Timer", true, 0L);

        queue.clear();

        assertTrue(queue.visibleAt(0L).isEmpty());
        assertTrue(queue.visibleAt(2_000L).isEmpty());
    }

    private static List<String> names(List<NotificationQueue.Entry> entries) {
        return entries.stream().map(NotificationQueue.Entry::moduleName).toList();
    }
}
