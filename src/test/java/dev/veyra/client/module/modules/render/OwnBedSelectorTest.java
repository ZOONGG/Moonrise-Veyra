package dev.veyra.client.module.modules.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OwnBedSelectorTest {
    @Test
    void choosesOnlyBedNearTeamSpawn() {
        assertEquals(1, OwnBedSelector.findIndex(List.of(
                new OwnBedSelector.BedPoint(80, 80),
                new OwnBedSelector.BedPoint(8, 5)), 0, 0));
    }

    @Test
    void hidesNothingWhenServerSpawnIsNotAtABase() {
        assertEquals(-1, OwnBedSelector.findIndex(
                List.of(new OwnBedSelector.BedPoint(80, 80)), 0, 0));
    }
}
