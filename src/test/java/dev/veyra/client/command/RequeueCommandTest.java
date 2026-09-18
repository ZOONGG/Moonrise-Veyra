package dev.veyra.client.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequeueCommandTest {
    @Test
    void convertsHypixelLocrawModeToPlayCommand() {
        RequeueCommandParser.Result result = RequeueCommandParser.fromLocraw(
                "{\"server\":\"mini1A\",\"gametype\":\"BEDWARS\","
                        + "\"mode\":\"BEDWARS_EIGHT_TWO\",\"map\":\"Lighthouse\"}");

        assertTrue(result.handled());
        assertEquals("/play bedwars_eight_two", result.playCommand());
    }

    @Test
    void ignoresOtherChatAndRejectsNonBedwarsLocations() {
        assertFalse(RequeueCommandParser.fromLocraw("hello").handled());

        RequeueCommandParser.Result result = RequeueCommandParser.fromLocraw(
                "{\"server\":\"mini1A\",\"gametype\":\"SKYWARS\","
                        + "\"mode\":\"solo_normal\"}");
        assertTrue(result.handled());
        assertNull(result.playCommand());
    }

    @Test
    void rejectsUnsafeModeInsteadOfBuildingAChatCommand() {
        RequeueCommandParser.Result result = RequeueCommandParser.fromLocraw(
                "{\"server\":\"mini1A\",\"gametype\":\"BEDWARS\","
                        + "\"mode\":\"BEDWARS_EIGHT_TWO;/lobby\"}");

        assertTrue(result.handled());
        assertNull(result.playCommand());
    }
}
