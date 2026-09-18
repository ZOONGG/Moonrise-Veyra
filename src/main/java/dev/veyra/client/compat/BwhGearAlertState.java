package dev.veyra.client.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Per-game deduplication for BWH diamond and armor-protection alerts. */
final class BwhGearAlertState {
    static final int DIAMOND_THRESHOLD = 16;

    private final Map<UUID, Integer> heldDiamonds = new HashMap<>();
    private final Map<UUID, Integer> protectionLevels = new HashMap<>();

    boolean updateDiamonds(UUID playerId, int amount) {
        int normalized = Math.max(0, amount);
        int previous = heldDiamonds.getOrDefault(playerId, 0);
        heldDiamonds.put(playerId, normalized);
        return previous < DIAMOND_THRESHOLD && normalized >= DIAMOND_THRESHOLD;
    }

    boolean updateProtection(UUID playerId, int level) {
        int normalized = Math.max(0, level);
        int previous = protectionLevels.getOrDefault(playerId, 0);
        if (normalized > previous) {
            protectionLevels.put(playerId, normalized);
            return normalized > 0;
        }
        return false;
    }

    void reset() {
        heldDiamonds.clear();
        protectionLevels.clear();
    }

    void retainPlayers(Set<UUID> visiblePlayers) {
        heldDiamonds.keySet().retainAll(visiblePlayers);
        protectionLevels.keySet().retainAll(visiblePlayers);
    }

    static String romanLevel(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> Integer.toString(level);
        };
    }
}
