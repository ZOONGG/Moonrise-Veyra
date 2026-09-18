package dev.veyra.client.compat;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BwhGearAlertStateTest {
    @Test
    void alertsWhenDiamondStackCrossesSixteenAndRearmsAfterSwitchingAway() {
        UUID player = UUID.randomUUID();

        assertFalse(new BwhGearAlertState().updateDiamonds(player, 15));

        BwhGearAlertState state = new BwhGearAlertState();
        assertFalse(state.updateDiamonds(player, 15));
        assertTrue(state.updateDiamonds(player, 16));
        assertFalse(state.updateDiamonds(player, 20));
        assertFalse(state.updateDiamonds(player, 0));
        assertTrue(state.updateDiamonds(player, 16));
    }

    @Test
    void protectionAlertsOnlyForRealUpgrades() {
        UUID player = UUID.randomUUID();
        BwhGearAlertState state = new BwhGearAlertState();

        assertFalse(state.updateProtection(player, 0));
        assertTrue(state.updateProtection(player, 1));
        assertFalse(state.updateProtection(player, 1));
        assertTrue(state.updateProtection(player, 2));
        assertFalse(state.updateProtection(player, 1));
        assertTrue(state.updateProtection(player, 3));
        assertTrue(state.updateProtection(player, 4));
        assertFalse(state.updateProtection(player, 4));
    }

    @Test
    void resetAllowsTheNextGameToAlertAgain() {
        UUID player = UUID.randomUUID();
        BwhGearAlertState state = new BwhGearAlertState();

        assertTrue(state.updateDiamonds(player, 16));
        assertTrue(state.updateProtection(player, 2));
        state.reset();
        assertTrue(state.updateDiamonds(player, 16));
        assertTrue(state.updateProtection(player, 2));
    }

    @Test
    void removingAnInvisiblePlayerRearmsTheirAlerts() {
        UUID player = UUID.randomUUID();
        BwhGearAlertState state = new BwhGearAlertState();

        assertTrue(state.updateDiamonds(player, 16));
        assertTrue(state.updateProtection(player, 1));
        state.retainPlayers(Set.of());
        assertTrue(state.updateDiamonds(player, 16));
        assertTrue(state.updateProtection(player, 1));
    }
}
