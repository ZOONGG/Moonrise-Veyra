package dev.veyra.client.player.clutch;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeMouseDeltaTest {
    @Test void nativeScalingReachesTargetAcrossSensitivityAndInvertSettings() {
        for (float sensitivity : new float[]{0, .1f, .5f, .8f, 1}) {
            float gain = NativeMouseDelta.sensitivityGain(sensitivity);
            for (boolean invert : new boolean[]{false, true}) {
                for (float initial : new float[]{-90, -89, -20, 0, 30, 90}) {
                    float pitch = initial;
                    float yaw = 61;
                    for (float progress : new float[]{.5f, 1}) {
                        float target = initial + (-90 - initial) * progress;
                        int delta = NativeMouseDelta.upwardCounts(pitch, target, gain, invert);
                        // Vanilla float scaling/inversion, followed by Entity.setAngles double math.
                        float scaled = delta * gain * (invert ? -1 : 1);
                        pitch = Math.max(-90, Math.min(90, (float) (pitch - scaled * .15D)));
                        assertTrue(pitch <= target + .0001f);
                    }
                    assertEquals(-90, pitch, .0001f);
                    assertEquals(61, yaw);
                }
            }
        }
    }
    @Test void physicalInputIsAddedAndSyntheticInputUsesCorrectDirection() {
        float gain = NativeMouseDelta.sensitivityGain(.5f);
        int normal = NativeMouseDelta.upwardCounts(30, -90, gain, false);
        int inverted = NativeMouseDelta.upwardCounts(30, -90, gain, true);
        assertTrue(normal > 0); assertEquals(-normal, inverted);
        assertEquals(0, NativeMouseDelta.upwardCounts(-90, -90, gain, false));
        assertEquals(0, NativeMouseDelta.upwardCounts(0, -90, 0, false));
    }
}
