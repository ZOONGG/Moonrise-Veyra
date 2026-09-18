package dev.veyra.client.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BwhBedwarsTeamLabelTest {
    @Test
    void formatsEveryBedwarsTeamIncludingTrueWhite() {
        assertEquals("§cRed team", BwhBedwarsTeamLabel.fromPrefix("§c[R] "));
        assertEquals("§9Blue team", BwhBedwarsTeamLabel.fromPrefix("§9[B] "));
        assertEquals("§fWhite team", BwhBedwarsTeamLabel.fromPrefix("§7[§fW§7] §f"));
        assertEquals("§7Gray team", BwhBedwarsTeamLabel.fromPrefix("§7[G] "));
    }


    @Test
    void usesFinalTeamColorAsStableDeduplicationKey() {
        assertEquals("color:§c", BwhBedwarsTeamLabel.teamKey("red-player-one", "§c[R] "));
        assertEquals("color:§c", BwhBedwarsTeamLabel.teamKey("red-player-two", "§7[§cR§7] §c"));
        assertEquals("color:§f", BwhBedwarsTeamLabel.teamKey("white-player", "§7[§fW§7] §f"));
        assertEquals("team:fallback", BwhBedwarsTeamLabel.teamKey("Fallback", "[none]"));
    }
}
