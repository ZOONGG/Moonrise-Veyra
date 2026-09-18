package dev.veyra.client.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationMotionTest {
    @Test
    void entryAndExitHaveGentleEndpoints() {
        assertEquals(0.0F, NotificationMotion.visibility(0L, 340L, 1_500L, 300L));
        assertTrue(NotificationMotion.visibility(34L, 340L, 1_500L, 300L) < 0.02F);
        assertEquals(0.5F, NotificationMotion.visibility(170L, 340L, 1_500L, 300L), 0.0001F);
        assertEquals(1.0F, NotificationMotion.visibility(1_840L, 340L, 1_500L, 300L));
        assertTrue(NotificationMotion.visibility(2_110L, 340L, 1_500L, 300L) < 0.02F);
        assertEquals(0.0F, NotificationMotion.visibility(2_140L, 340L, 1_500L, 300L));
    }
}
