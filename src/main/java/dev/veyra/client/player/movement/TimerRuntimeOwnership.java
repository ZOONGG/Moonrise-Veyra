package dev.veyra.client.player.movement;

/** Owns only Veyra writes to the shared Minecraft timer; idle clients are passive. */
public final class TimerRuntimeOwnership {
    private boolean owned;
    private float previous = 1.0F;
    private float lastWritten;

    public float update(float current, boolean requested, float speed) {
        if (requested) {
            if (!owned || Float.compare(current, lastWritten) != 0) previous = current;
            owned = true;
            return lastWritten = speed;
        }
        if (!owned) return current;
        owned = false;
        return Float.compare(current, lastWritten) == 0 ? previous : current;
    }
}
