package dev.veyra.client.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BwhGearAlertCompatTest {
    @Test
    void formatsBedwarsProtectionLevels() {
        assertEquals("I", BwhGearAlertState.romanLevel(1));
        assertEquals("II", BwhGearAlertState.romanLevel(2));
        assertEquals("III", BwhGearAlertState.romanLevel(3));
        assertEquals("IV", BwhGearAlertState.romanLevel(4));
    }
}
