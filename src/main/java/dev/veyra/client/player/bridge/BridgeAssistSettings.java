package dev.veyra.client.player.bridge;

public record BridgeAssistSettings(
        long unsneakDelayMs,
        boolean sneakOnJump,
        boolean avoidDoubleSneaking,
        boolean adaptiveTwoBlockRhythm
) {
    public static final long MIN_SAFE_UNSNEAK_DELAY_MS = 0L;

    public BridgeAssistSettings {
        // Safety no longer depends on a timer: BridgeAssistEngine also requires
        // confirmed support before releasing. Zero therefore stays fast and safe.
        unsneakDelayMs = Math.max(MIN_SAFE_UNSNEAK_DELAY_MS, unsneakDelayMs);
    }

    public BridgeAssistSettings(
            long unsneakDelayMs,
            boolean sneakOnJump,
            boolean avoidDoubleSneaking
    ) {
        this(unsneakDelayMs, sneakOnJump, avoidDoubleSneaking, true);
    }
}
