package dev.veyra.client.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BwhTeamColorResolverTest {
    @Test
    void usesFinalTeamColorInsteadOfGrayFormattingWrapper() {
        assertEquals("\u00a7f", BwhTeamColorResolver.resolve("\u00a77[\u00a7fW\u00a77] \u00a7f"));
        assertEquals("\u00a7c", BwhTeamColorResolver.resolve("\u00a77[\u00a7cR\u00a77] \u00a7c"));
    }

    @Test
    void preservesPlainHypixelTeamColors() {
        assertEquals("\u00a7b", BwhTeamColorResolver.resolve("\u00a7b"));
        assertEquals("\u00a7d", BwhTeamColorResolver.resolve("&d[P] "));
    }

    @Test
    void refusesPrefixesWithoutAColor() {
        assertNull(BwhTeamColorResolver.resolve("[TEAM] "));
        assertNull(BwhTeamColorResolver.resolve(null));
    }
}
