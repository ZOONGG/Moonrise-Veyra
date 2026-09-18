package dev.veyra.client.player.place;

/** Classic Minecraft right-click delay, measured in client ticks. */
public record FastPlaceSettings(int delayTicks) {
    public FastPlaceSettings {
        delayTicks = Math.max(0, Math.min(4, delayTicks));
    }
}
