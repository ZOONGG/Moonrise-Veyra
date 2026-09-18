package dev.veyra.client.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BwhBowAlertLabelTest {
    @Test
    void distinguishesEveryBedwarsBowTier() {
        assertEquals("Bow", BwhBowAlertLabel.fromLevels(0, 0));
        assertEquals("Power Bow", BwhBowAlertLabel.fromLevels(1, 0));
        assertEquals("Punch Bow", BwhBowAlertLabel.fromLevels(1, 1));
    }

    @Test
    void preservesUnexpectedHigherEnchantLevels() {
        assertEquals("Punch Bow", BwhBowAlertLabel.fromLevels(2, 2));
    }

    @Test
    void fallsBackToRawPacketEvidence() {
        assertEquals("Power Bow", BwhBowAlertLabel.fromEvidence(0, 0, 1, null));
        assertEquals("Punch Bow", BwhBowAlertLabel.fromEvidence(0, 0, 2, null));
        assertEquals("Punch Bow", BwhBowAlertLabel.fromEvidence(0, 0, 0, "Punch I"));
    }
}
