package dev.veyra.client.clickgui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleSearchTest {
    @Test
    void matchesTextTypedWithRussianKeyboardLayout() {
        assertTrue(ModuleSearch.matches("HitSelect Combat Filters attacks", "ршеыудусе"));
        assertTrue(ModuleSearch.matches("AutoBlock Combat Blocks hits", "фгещидщсл"));
    }

    @Test
    void remainsCaseAndWhitespaceInsensitive() {
        assertTrue(ModuleSearch.matches("Auto Rod Utility", "  AUTO   ROD "));
        assertFalse(ModuleSearch.matches("Velocity Combat", "timer"));
    }
}
