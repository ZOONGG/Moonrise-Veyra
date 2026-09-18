package dev.veyra.client.utils.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiBotNameClassifierTest {
    @Test
    void recognizesHypixelBedwarsMerchants() {
        assertTrue(AntiBotNameClassifier.isKnownNpc("§e§lITEM SHOP", "ITEM SHOP"));
        assertTrue(AntiBotNameClassifier.isKnownNpc("§b§lTEAM UPGRADES", "TEAM UPGRADES"));
        assertTrue(AntiBotNameClassifier.isKnownNpc("§e[NPC] Shopkeeper", "[NPC] Shopkeeper"));
    }

    @Test
    void keepsOrdinaryPlayers() {
        assertFalse(AntiBotNameClassifier.isKnownNpc("§cDyrov", "Dyrov"));
        assertFalse(AntiBotNameClassifier.isKnownNpc("§aFriendly_Duck", "Friendly_Duck"));
    }
}
