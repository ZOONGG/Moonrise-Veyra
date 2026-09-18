package dev.veyra.client.module.bind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindNotificationTrackerTest {
    @Test
    void onlyDirectBindTransitionsNotify() {
        BindNotificationTracker tracker = new BindNotificationTracker();

        assertTrue(tracker.transition(true, ModuleTransitionSource.KEYBIND));
        assertFalse(tracker.transition(false, ModuleTransitionSource.AUTO));
        assertTrue(tracker.transition(false, ModuleTransitionSource.KEYBIND));
    }

    @Test
    void guiAndConfigLifetimesNeverNotify() {
        BindNotificationTracker tracker = new BindNotificationTracker();

        assertFalse(tracker.transition(true, ModuleTransitionSource.GUI));
        assertFalse(tracker.transition(false, ModuleTransitionSource.AUTO));
        assertFalse(tracker.transition(true, ModuleTransitionSource.CONFIG));
        assertFalse(tracker.transition(false, ModuleTransitionSource.CONFIG));
    }

    @Test
    void guiDisableOfBindActivatedModuleDoesNotNotify() {
        BindNotificationTracker tracker = new BindNotificationTracker();
        tracker.transition(true, ModuleTransitionSource.KEYBIND);

        assertFalse(tracker.transition(false, ModuleTransitionSource.GUI));
    }
}
