package dev.veyra.client.module.bind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindStateMachineTest {
    @Test
    void toggleFiresOnlyOnEachPressEdge() {
        BindStateMachine state = new BindStateMachine();

        assertEquals(BindAction.TOGGLE, state.update(BindMode.TOGGLE, true));
        assertEquals(BindAction.NONE, state.update(BindMode.TOGGLE, true));
        assertEquals(BindAction.NONE, state.update(BindMode.TOGGLE, false));
        assertEquals(BindAction.TOGGLE, state.update(BindMode.TOGGLE, true));
    }

    @Test
    void holdEnablesOnPressAndDisablesOnRelease() {
        BindStateMachine state = new BindStateMachine();

        assertEquals(BindAction.ENABLE, state.update(BindMode.HOLD, true));
        assertEquals(BindAction.NONE, state.update(BindMode.HOLD, true));
        assertEquals(BindAction.DISABLE, state.update(BindMode.HOLD, false));
        assertEquals(BindAction.NONE, state.update(BindMode.HOLD, false));
    }

    @Test
    void pressInvokesOneActionPerPressEdge() {
        BindStateMachine state = new BindStateMachine();

        assertEquals(BindAction.PRESS, state.update(BindMode.PRESS, true));
        assertEquals(BindAction.NONE, state.update(BindMode.PRESS, true));
        assertEquals(BindAction.NONE, state.update(BindMode.PRESS, false));
        assertEquals(BindAction.PRESS, state.update(BindMode.PRESS, true));
    }

    @Test
    void resetReleasesAnActiveHoldBindOnce() {
        BindStateMachine state = new BindStateMachine();
        state.update(BindMode.HOLD, true);

        assertEquals(BindAction.DISABLE, state.reset(BindMode.HOLD));
        assertEquals(BindAction.NONE, state.reset(BindMode.HOLD));
    }

    @Test
    void parsesPersistedModeWithoutBreakingOlderOrInvalidConfigs() {
        assertEquals(BindMode.HOLD, BindMode.fromConfig("hold", BindMode.TOGGLE));
        assertEquals(BindMode.TOGGLE, BindMode.fromConfig(null, BindMode.TOGGLE));
        assertEquals(BindMode.TOGGLE, BindMode.fromConfig("unknown", BindMode.TOGGLE));
    }

    @Test
    void onlyKeybindTransitionsProduceStateNotifications() {
        assertTrue(ModuleTransitionSource.KEYBIND.shouldNotify());
        assertFalse(ModuleTransitionSource.GUI.shouldNotify());
        assertFalse(ModuleTransitionSource.CONFIG.shouldNotify());
        assertFalse(ModuleTransitionSource.AUTO.shouldNotify());
    }
}
