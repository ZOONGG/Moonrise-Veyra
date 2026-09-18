package dev.veyra.client.compat;

import java.util.HashMap;
import java.util.Map;

/** Stores the highest observed Protection upgrade for each BedWars team. */
final class BwhTeamProtectionState {
    private final Map<String, Integer> levels = new HashMap<>();

    boolean update(String teamKey, int level) {
        if (teamKey == null || teamKey.isBlank() || level <= 0) return false;
        int previous = levels.getOrDefault(teamKey, 0);
        if (level <= previous) return false;
        levels.put(teamKey, level);
        return true;
    }

    void reset() {
        levels.clear();
    }
}
