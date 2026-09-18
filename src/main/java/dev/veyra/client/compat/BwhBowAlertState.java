package dev.veyra.client.compat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Emits each observed bow tier once per player and game. */
final class BwhBowAlertState {
    private final Map<UUID, Set<String>> announced = new HashMap<>();

    synchronized boolean update(UUID playerId, String bowType) {
        if (playerId == null || bowType == null || bowType.isBlank()) return false;
        return announced.computeIfAbsent(playerId, ignored -> new HashSet<>()).add(bowType);
    }

    synchronized void reset() {
        announced.clear();
    }
}
