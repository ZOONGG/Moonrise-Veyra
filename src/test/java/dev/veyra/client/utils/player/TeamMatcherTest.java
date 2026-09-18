package dev.veyra.client.utils.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamMatcherTest {
    @Test
    void matchesScoreboardTeamNamesIgnoringCase() {
        assertTrue(TeamMatcher.sameRegisteredTeam("red", "RED"));
        assertFalse(TeamMatcher.sameRegisteredTeam("red", "blue"));
        assertFalse(TeamMatcher.sameRegisteredTeam(null, null));
    }

    @Test
    void usesTheColorImmediatelyAppliedToThePlayerName() {
        assertTrue(TeamMatcher.sameEffectiveColor(
                "\u00a76[MVP+] \u00a7c", "\u00a7c[R] \u00a7cAlice", "Alice",
                "\u00a77[VIP] \u00a7c", "\u00a7c[R] \u00a7cBob", "Bob"));
        assertFalse(TeamMatcher.sameEffectiveColor(
                "\u00a7c", "\u00a7cAlice", "Alice",
                "\u00a79", "\u00a79Bob", "Bob"));
    }
}
