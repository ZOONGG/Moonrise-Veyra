package dev.veyra.client.player.movement;

/** Correction recovery uses real time and never changes the user's toggle. */
public final class FreezeRecoveryController {
    private enum Phase { RUNNING, WAITING, STOPPED }
    private Phase phase = Phase.STOPPED;
    private long resumeAt;

    public synchronized void arm() { phase = Phase.RUNNING; }
    public synchronized void disarm() { phase = Phase.STOPPED; }
    public synchronized void corrected(boolean repeat, long now, long delayNanos) {
        if (phase == Phase.STOPPED) return;
        phase = repeat ? Phase.WAITING : Phase.STOPPED;
        resumeAt = now + Math.max(0L, delayNanos);
    }
    public synchronized void advance(long now) {
        if (phase == Phase.WAITING && now - resumeAt >= 0) phase = Phase.RUNNING;
    }
    public synchronized boolean blocksFreeze() { return phase != Phase.RUNNING; }
    public synchronized boolean shouldDisable() { return phase == Phase.STOPPED; }
}
