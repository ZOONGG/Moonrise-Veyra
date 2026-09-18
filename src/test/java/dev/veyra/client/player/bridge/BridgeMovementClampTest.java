package dev.veyra.client.player.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BridgeMovementClampTest {
    @Test
    void unsupportedMovementIsReducedAllTheWayToZero() {
        BridgeMovementClamp.HorizontalMovement movement = BridgeMovementClamp.clamp(
                0.42D, -0.31D, (x, z) -> x == 0.0D && z == 0.0D);

        assertEquals(0.0D, movement.x(), 0.0001D);
        assertEquals(0.0D, movement.z(), 0.0001D);
    }

    @Test
    void highSpeedMovementStopsAtLastSupportedOffset() {
        BridgeMovementClamp.HorizontalMovement movement = BridgeMovementClamp.clamp(
                0.62D, 0.0D, (x, z) -> x <= 0.22D);

        assertEquals(0.22D, movement.x(), 0.0001D);
        assertEquals(0.0D, movement.z(), 0.0001D);
    }

    @Test
    void diagonalMovementCannotSlipPastACorner() {
        BridgeMovementClamp.HorizontalMovement movement = BridgeMovementClamp.clamp(
                0.30D, 0.30D,
                (x, z) -> x == 0.0D || z == 0.0D || x + z <= 0.31D);

        assertEquals(0.15D, movement.x(), 0.0001D);
        assertEquals(0.15D, movement.z(), 0.0001D);
    }
}
