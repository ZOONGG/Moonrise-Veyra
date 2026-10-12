package dev.veyra.client.player.movement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimerRuntimeOwnershipTest {
    @Test void inactiveVeyraPreservesExternalTimerOnEveryFrame() {
        var owner = new TimerRuntimeOwnership();
        for (float external : new float[]{0, 0.1F, 1, 1.6F, 2.5F}) {
            for (int frame = 0; frame < 100; frame++)
                assertEquals(external, owner.update(external, false, 1));
        }
    }
    @Test void disablingRestoresExternalSpeedOnceAndThenRemainsPassive() {
        var owner = new TimerRuntimeOwnership();
        assertEquals(0.2F, owner.update(1.6F, true, 0.2F));
        assertEquals(1.6F, owner.update(0.2F, false, 1));
        assertEquals(1.8F, owner.update(1.8F, false, 1));
    }
    @Test void normalDisableRestoresVanillaAndZeroDoesNotGetClamped() {
        var owner = new TimerRuntimeOwnership();
        assertEquals(0, owner.update(1, true, 0));
        assertEquals(1, owner.update(0, false, 1));
    }
    @Test void externalWriteBeforeReleaseIsNotOverwritten() {
        var owner = new TimerRuntimeOwnership();
        owner.update(1, true, 0.3F);
        assertEquals(1.7F, owner.update(1.7F, false, 1));
    }
    @Test void externalWriteDuringOwnershipBecomesNewRestoreValue() {
        var owner = new TimerRuntimeOwnership();
        owner.update(1, true, 0.3F);
        assertEquals(0.3F, owner.update(1.8F, true, 0.3F));
        assertEquals(1.8F, owner.update(0.3F, false, 1));
    }
    @Test void nestedOverridesRestoreManualThenExternalRuntime() {
        var controller = new TimerRuntimeController();
        var owner = new TimerRuntimeOwnership();
        controller.setManual(true, 0.7);
        float clock = owner.update(1.5F, controller.isActive(), (float) controller.speed());
        var first = controller.override(0.2);
        clock = owner.update(clock, controller.isActive(), (float) controller.speed());
        var second = controller.override(0);
        clock = owner.update(clock, controller.isActive(), (float) controller.speed());
        assertEquals(0, clock);
        first.close(); second.close();
        clock = owner.update(clock, controller.isActive(), (float) controller.speed());
        assertEquals(0.7F, clock);
        controller.setManual(false, 0.7);
        assertEquals(1.5F, owner.update(clock, controller.isActive(), (float) controller.speed()));
    }
    @Test void aimOverrideRestoresExternalTimerBeforePearl() {
        var controller = new TimerRuntimeController();
        var owner = new TimerRuntimeOwnership();
        var lease = controller.override(0.1);
        float clock = owner.update(1.4F, controller.isActive(), (float) controller.speed());
        assertEquals(0.1F, clock);
        lease.close();
        assertEquals(1.4F, owner.update(clock, controller.isActive(), (float) controller.speed()));
    }
}
