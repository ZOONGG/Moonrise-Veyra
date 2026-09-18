package dev.veyra.client.compat;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BwhRosterParserTest {
    @Test
    void parsesAFormattedHypixelWhoResponse() {
        BwhRosterParser.Result result = BwhRosterParser.parse(
                "\u00a7aONLINE: Alice, Bob_2, Alice");

        assertTrue(result.matched());
        assertEquals(Set.of("Alice", "Bob_2"), result.players());
    }

    @Test
    void ignoresUnrelatedChatAndInvalidNames() {
        assertFalse(BwhRosterParser.parse("hello").matched());
        assertEquals(Set.of("ValidName"),
                BwhRosterParser.parse("ONLINE: ValidName, invalid-name").players());
    }
}
