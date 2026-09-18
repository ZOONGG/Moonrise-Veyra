package dev.veyra.client.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BwhVisibleOverlayNameTest {
    @Test
    void alwaysUsesVisibleGameAliasInsteadOfApiTechnicalName() {
        assertEquals("§avlazhggkr", BwhVisibleOverlayName.resolve(
                "vlazhggkr", "§a[G] ", "§a[G] vlazhggkr"));
        assertEquals("§fCurrentNick", BwhVisibleOverlayName.resolve(
                "CurrentNick", null, null));
    }

    @Test
    void preservesTrueWhiteTeamColor() {
        assertEquals("§fWhiteAlias", BwhVisibleOverlayName.resolve(
                "WhiteAlias", "§7[§fW§7] §f", "§fWhiteAlias"));
        assertNull(BwhVisibleOverlayName.resolve(" ", "§c", null));
    }
}
