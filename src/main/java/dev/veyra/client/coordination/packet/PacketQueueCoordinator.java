package dev.veyra.client.coordination.packet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Exclusive packet-capture lease with strictly owner-scoped queues. */
public final class PacketQueueCoordinator<T> {
    private final int maximumQueueSize;
    private final Map<String, OwnerState<T>> owners = new HashMap<>();
    private long sequence;

    public PacketQueueCoordinator(int maximumQueueSize) {
        if (maximumQueueSize < 1) {
            throw new IllegalArgumentException("maximumQueueSize must be positive");
        }
        this.maximumQueueSize = maximumQueueSize;
    }

    public synchronized void acquire(String owner, int priority) {
        requireOwner(owner);
        OwnerState<T> previous = owners.get(owner);
        ArrayDeque<T> queue = previous == null ? new ArrayDeque<>() : previous.queue();
        owners.put(owner, new OwnerState<>(priority, ++sequence, queue));
    }

    public synchronized boolean isActive(String owner) {
        OwnerState<T> requested = owners.get(owner);
        return requested != null && requested == activeState();
    }

    public synchronized boolean hasOwner(String owner) {
        return owners.containsKey(owner);
    }

    public synchronized boolean canAcquire(String owner, int priority) {
        requireOwner(owner);
        OwnerState<T> active = activeState();
        OwnerState<T> requested = owners.get(owner);
        return active == null || active == requested || priority >= active.priority();
    }

    public synchronized boolean enqueue(String owner, T packet) {
        if (packet == null || !isActive(owner)) return false;
        OwnerState<T> state = owners.get(owner);
        if (state.queue().size() >= maximumQueueSize) return false;
        state.queue().addLast(packet);
        return true;
    }

    public synchronized List<T> drain(String owner) {
        OwnerState<T> state = owners.get(owner);
        if (state == null || state.queue().isEmpty()) return List.of();
        List<T> packets = new ArrayList<>(state.queue());
        state.queue().clear();
        return List.copyOf(packets);
    }

    public synchronized List<T> drainWhile(String owner, Predicate<T> predicate) {
        if (predicate == null) throw new IllegalArgumentException("predicate must not be null");
        OwnerState<T> state = owners.get(owner);
        if (state == null || state.queue().isEmpty()) return List.of();
        List<T> packets = new ArrayList<>();
        T packet;
        while ((packet = state.queue().peekFirst()) != null && predicate.test(packet)) {
            packets.add(state.queue().removeFirst());
        }
        return List.copyOf(packets);
    }

    public synchronized List<T> release(String owner) {
        OwnerState<T> state = owners.remove(owner);
        if (state == null || state.queue().isEmpty()) return List.of();
        return List.copyOf(state.queue());
    }

    public synchronized void discard(String owner) {
        OwnerState<T> state = owners.get(owner);
        if (state != null) state.queue().clear();
    }

    public synchronized int size(String owner) {
        OwnerState<T> state = owners.get(owner);
        return state == null ? 0 : state.queue().size();
    }

    public synchronized String activeOwner() {
        OwnerState<T> active = activeState();
        if (active == null) return null;
        for (Map.Entry<String, OwnerState<T>> entry : owners.entrySet()) {
            if (entry.getValue() == active) return entry.getKey();
        }
        return null;
    }

    private OwnerState<T> activeState() {
        OwnerState<T> selected = null;
        for (OwnerState<T> candidate : owners.values()) {
            if (selected == null
                    || candidate.priority() > selected.priority()
                    || candidate.priority() == selected.priority()
                    && candidate.sequence() > selected.sequence()) {
                selected = candidate;
            }
        }
        return selected;
    }

    private static void requireOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("owner must not be blank");
        }
    }

    private record OwnerState<T>(int priority, long sequence, ArrayDeque<T> queue) {
    }
}
