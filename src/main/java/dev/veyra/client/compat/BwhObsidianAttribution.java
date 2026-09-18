package dev.veyra.client.compat;

import java.util.Collection;
import java.util.UUID;

/** Selects an author only when packet evidence leaves exactly one candidate. */
final class BwhObsidianAttribution {
    private BwhObsidianAttribution() {
    }

    static UUID uniqueNearbyHolder(Collection<Candidate> candidates, double maxDistanceSq) {
        UUID match = null;
        for (Candidate candidate : candidates) {
            if (!candidate.holdingObsidian() || candidate.distanceSq() > maxDistanceSq) continue;
            if (match != null && !match.equals(candidate.playerId())) return null;
            match = candidate.playerId();
        }
        return match;
    }

    record Candidate(UUID playerId, double distanceSq, boolean holdingObsidian) {
    }
}
