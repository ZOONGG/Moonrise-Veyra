package dev.veyra.client.module.modules.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NametagRenderPolicyTest {
    @Test
    void preservesLunarOwnNametagInThirdPerson() {
        assertFalse(NametagRenderPolicy.shouldCancelBaseLabel(true, true));
    }

    @Test
    void stillReplacesOtherPlayerLabels() {
        assertTrue(NametagRenderPolicy.shouldCancelBaseLabel(true, false));
        assertFalse(NametagRenderPolicy.shouldCancelBaseLabel(false, false));
    }
}
