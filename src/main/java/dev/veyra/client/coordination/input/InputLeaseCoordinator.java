package dev.veyra.client.coordination.input;

import java.util.HashMap;
import java.util.Map;

/** Priority-aware logical key ownership, independent from Minecraft input APIs. */
public final class InputLeaseCoordinator {
    private final Map<Integer, Map<String, Lease>> leasesByKey = new HashMap<>();
    private long sequence;

    public synchronized void set(String owner, int keyCode, boolean pressed, int priority) {
        requireOwner(owner);
        leasesByKey.computeIfAbsent(keyCode, ignored -> new HashMap<>())
                .put(owner, new Lease(pressed, priority, ++sequence));
    }

    public synchronized void release(String owner, int keyCode) {
        requireOwner(owner);
        Map<String, Lease> leases = leasesByKey.get(keyCode);
        if (leases == null) return;
        leases.remove(owner);
        if (leases.isEmpty()) leasesByKey.remove(keyCode);
    }

    public synchronized void releaseOwner(String owner) {
        requireOwner(owner);
        leasesByKey.values().forEach(leases -> leases.remove(owner));
        leasesByKey.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public synchronized boolean resolve(int keyCode, boolean physicalState) {
        Map<String, Lease> leases = leasesByKey.get(keyCode);
        if (leases == null || leases.isEmpty()) return physicalState;

        Lease selected = null;
        for (Lease candidate : leases.values()) {
            if (selected == null
                    || candidate.priority() > selected.priority()
                    || candidate.priority() == selected.priority()
                    && candidate.sequence() > selected.sequence()) {
                selected = candidate;
            }
        }
        return selected != null ? selected.pressed() : physicalState;
    }

    public synchronized boolean hasLeases(int keyCode) {
        Map<String, Lease> leases = leasesByKey.get(keyCode);
        return leases != null && !leases.isEmpty();
    }

    private static void requireOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("owner must not be blank");
        }
    }

    private record Lease(boolean pressed, int priority, long sequence) {
    }
}
