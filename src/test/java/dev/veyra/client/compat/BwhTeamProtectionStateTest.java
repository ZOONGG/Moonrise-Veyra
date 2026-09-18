package dev.veyra.client.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BwhTeamProtectionStateTest {
    @Test
    void alertsOncePerTeamAndOnlyForHigherLevels() {
        BwhTeamProtectionState state = new BwhTeamProtectionState();

        assertTrue(state.update("team_red", 1));
        assertFalse(state.update("team_red", 1));
        assertTrue(state.update("team_red", 2));
        assertFalse(state.update("team_red", 1));
        assertTrue(state.update("team_blue", 1));
    }

    @Test
    void resetRearmsTeamsForTheNextGame() {
        BwhTeamProtectionState state = new BwhTeamProtectionState();
        assertTrue(state.update("team_white", 3));
        state.reset();
        assertTrue(state.update("team_white", 1));
    }
}
