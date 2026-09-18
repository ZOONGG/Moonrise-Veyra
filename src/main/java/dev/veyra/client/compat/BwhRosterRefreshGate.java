package dev.veyra.client.compat;

import java.util.LinkedHashSet;
import java.util.Set;

/** Collapses Hypixel's repeated identical /who responses into one refresh. */
public final class BwhRosterRefreshGate {
    private static final long DUPLICATE_WINDOW_MS = 5_000L;
    private Set<String> lastRoster = Set.of();
    private long lastRefreshAtMs = Long.MIN_VALUE;

    public synchronized boolean shouldRefresh(Set<String> roster, long nowMs) {
        boolean duplicate = lastRoster.equals(roster)
                && lastRefreshAtMs != Long.MIN_VALUE
                && nowMs - lastRefreshAtMs < DUPLICATE_WINDOW_MS;
        if (duplicate) return false;
        lastRoster = new LinkedHashSet<>(roster);
        lastRefreshAtMs = nowMs;
        return true;
    }
}
