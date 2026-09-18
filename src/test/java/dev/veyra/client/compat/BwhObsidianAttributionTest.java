package dev.veyra.client.compat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BwhObsidianAttributionTest {
    @Test
    void attributesOnlyOneNearbyHolder() {
        UUID author = UUID.randomUUID();
        UUID farAway = UUID.randomUUID();

        UUID result = BwhObsidianAttribution.uniqueNearbyHolder(List.of(
                new BwhObsidianAttribution.Candidate(author, 4.0D, true),
                new BwhObsidianAttribution.Candidate(farAway, 100.0D, true)), 36.0D);

        assertEquals(author, result);
    }

    @Test
    void refusesToGuessWhenTwoNearbyPlayersHoldObsidian() {
        UUID result = BwhObsidianAttribution.uniqueNearbyHolder(List.of(
                new BwhObsidianAttribution.Candidate(UUID.randomUUID(), 4.0D, true),
                new BwhObsidianAttribution.Candidate(UUID.randomUUID(), 9.0D, true)), 36.0D);

        assertNull(result);
    }

    @Test
    void ignoresPlayersWhoDoNotActuallyHoldObsidian() {
        UUID result = BwhObsidianAttribution.uniqueNearbyHolder(List.of(
                new BwhObsidianAttribution.Candidate(UUID.randomUUID(), 1.0D, false)), 36.0D);

        assertNull(result);
    }
}
