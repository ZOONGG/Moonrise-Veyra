package dev.veyra.client.notification;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class NotificationQueue {
    private final int maximumVisible;
    private final long lifetimeMs;
    private final List<Entry> visible = new ArrayList<>();
    private final Deque<PendingEntry> pending = new ArrayDeque<>();

    public NotificationQueue(int maximumVisible, long lifetimeMs) {
        if (maximumVisible < 1) throw new IllegalArgumentException("maximumVisible must be positive");
        if (lifetimeMs < 1L) throw new IllegalArgumentException("lifetimeMs must be positive");
        this.maximumVisible = maximumVisible;
        this.lifetimeMs = lifetimeMs;
    }

    public synchronized void post(String moduleName, boolean enabled, long nowMs) {
        if (moduleName == null || moduleName.isBlank()) return;
        pruneExpired(nowMs);
        boolean replacedVisible = visible.removeIf(
                entry -> entry.moduleName.equalsIgnoreCase(moduleName));
        pending.removeIf(entry -> entry.moduleName.equalsIgnoreCase(moduleName));

        if (replacedVisible) {
            visible.add(new Entry(moduleName, enabled, nowMs));
            promotePending(nowMs);
            return;
        }

        promotePending(nowMs);
        if (visible.size() < maximumVisible) visible.add(new Entry(moduleName, enabled, nowMs));
        else pending.addLast(new PendingEntry(moduleName, enabled));
    }

    public synchronized List<Entry> visibleAt(long nowMs) {
        pruneAndPromote(nowMs);
        return List.copyOf(visible);
    }

    public synchronized void clear() {
        visible.clear();
        pending.clear();
    }

    private void pruneAndPromote(long nowMs) {
        pruneExpired(nowMs);
        promotePending(nowMs);
    }

    private void pruneExpired(long nowMs) {
        visible.removeIf(entry -> nowMs - entry.startedAtMs > lifetimeMs);
    }

    private void promotePending(long nowMs) {
        while (visible.size() < maximumVisible && !pending.isEmpty()) {
            PendingEntry entry = pending.removeFirst();
            visible.add(new Entry(entry.moduleName, entry.enabled, nowMs));
        }
    }

    public record Entry(String moduleName, boolean enabled, long startedAtMs) {
    }

    private record PendingEntry(String moduleName, boolean enabled) {
    }
}
