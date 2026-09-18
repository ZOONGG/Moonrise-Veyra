package dev.veyra.client.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BwhProtectionPurchaseParserTest {
    @Test
    void parsesAllHypixelReinforcedArmorLevels() {
        assertPurchase("osteli purchased Reinforced Armor I", "osteli", 1);
        assertPurchase("Player_2 purchased Reinforced Armor II", "Player_2", 2);
        assertPurchase("Player3 purchased Reinforced Armor III", "Player3", 3);
        assertPurchase("Player4 purchased Reinforced Armor IV", "Player4", 4);
    }

    @Test
    void acceptsFormattedMessagesAndRejectsOtherUpgrades() {
        assertPurchase("\u00a7aosteli purchased Reinforced Armor II", "osteli", 2);
        assertPurchase("TEAM UPGRADE > [MVP+] osteli purchased Reinforced Armor III!",
                "osteli", 3);
        assertPurchase("Team Upgrade: Player_2 PURCHASED Reinforced Armor IV",
                "Player_2", 4);
        assertFalse(BwhProtectionPurchaseParser.parse(
                "osteli purchased Sharpened Swords").matched());
    }

    private static void assertPurchase(String message, String player, int level) {
        BwhProtectionPurchaseParser.Result result = BwhProtectionPurchaseParser.parse(message);
        assertTrue(result.matched());
        assertEquals(player, result.purchaser());
        assertEquals(level, result.level());
    }
}
