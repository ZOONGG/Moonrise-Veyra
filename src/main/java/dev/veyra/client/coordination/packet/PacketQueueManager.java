package dev.veyra.client.coordination.packet;

import dev.veyra.client.utils.packet.PacketUtils;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.network.Packet;

import java.util.List;

/** Runtime packet capture backed by exclusive, direction-aware owner queues. */
public final class PacketQueueManager {
    private static final int MAX_PACKETS_PER_OWNER = 4096;
    private static final PacketQueueCoordinator<QueuedPacket> OUTGOING =
            new PacketQueueCoordinator<>(MAX_PACKETS_PER_OWNER);
    private static final PacketQueueCoordinator<QueuedPacket> INCOMING =
            new PacketQueueCoordinator<>(MAX_PACKETS_PER_OWNER);
    private static boolean replaying;

    public static synchronized void acquire(String owner, int priority,
                                            boolean outgoing, boolean incoming) {
        if (outgoing) OUTGOING.acquire(owner, priority);
        if (incoming) INCOMING.acquire(owner, priority);
    }

    public static synchronized boolean isActive(String owner, boolean outgoing) {
        return (outgoing ? OUTGOING : INCOMING).isActive(owner);
    }

    public static synchronized boolean hasOwner(String owner, boolean outgoing) {
        return (outgoing ? OUTGOING : INCOMING).hasOwner(owner);
    }

    public static synchronized boolean canAcquire(String owner, int priority,
                                                  boolean outgoing) {
        return (outgoing ? OUTGOING : INCOMING).canAcquire(owner, priority);
    }

    public static synchronized boolean capture(String owner, Packet<?> packet, boolean outgoing) {
        if (replaying) return false;
        return (outgoing ? OUTGOING : INCOMING).enqueue(owner,
                new QueuedPacket(packet, System.currentTimeMillis()));
    }

    public static synchronized int size(String owner) {
        return OUTGOING.size(owner) + INCOMING.size(owner);
    }

    public static synchronized void flush(String owner) {
        replay(OUTGOING.drain(owner), true);
        replay(INCOMING.drain(owner), false);
    }

    public static synchronized void releaseDue(String owner, boolean outgoing,
                                               long delayMs, long nowMs) {
        PacketQueueCoordinator<QueuedPacket> coordinator = outgoing ? OUTGOING : INCOMING;
        replay(coordinator.drainWhile(owner,
                queued -> nowMs - queued.queuedAtMs() >= Math.max(0L, delayMs)), outgoing);
    }

    public static synchronized void release(String owner, boolean flush) {
        List<QueuedPacket> outgoing = OUTGOING.release(owner);
        List<QueuedPacket> incoming = INCOMING.release(owner);
        if (flush) {
            replay(outgoing, true);
            replay(incoming, false);
        }
    }

    public static synchronized void releaseDirection(String owner, boolean outgoing,
                                                     boolean flush) {
        List<QueuedPacket> packets = (outgoing ? OUTGOING : INCOMING).release(owner);
        if (flush) replay(packets, outgoing);
    }

    public static synchronized void discard(String owner) {
        OUTGOING.discard(owner);
        INCOMING.discard(owner);
    }

    public static synchronized boolean isReplaying() {
        return replaying;
    }

    private static void replay(List<QueuedPacket> packets, boolean outgoing) {
        if (packets.isEmpty() || !PlayerUtils.isPlayerInGame()) return;
        replaying = true;
        try {
            for (QueuedPacket packet : packets) PacketUtils.handle(packet.packet(), outgoing);
        } finally {
            replaying = false;
        }
    }

    private PacketQueueManager() {
    }

    private record QueuedPacket(Packet<?> packet, long queuedAtMs) {
    }
}
