package dev.veyra.client.compat;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BwhRosterRefreshGateTest {
    @Test
    void suppressesImmediateDuplicatesButAcceptsANewMatch() {
        BwhRosterRefreshGate gate = new BwhRosterRefreshGate();
        Set<String> first = Set.of("Alice", "Bob");

        assertTrue(gate.shouldRefresh(first, 1_000L));
        assertFalse(gate.shouldRefresh(first, 1_500L));
        assertTrue(gate.shouldRefresh(Set.of("Carol", "Dave"), 1_600L));
        assertTrue(gate.shouldRefresh(Set.of("Carol", "Dave"), 7_000L));
    }
}
