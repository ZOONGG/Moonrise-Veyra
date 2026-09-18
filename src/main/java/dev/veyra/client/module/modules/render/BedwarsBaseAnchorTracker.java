package dev.veyra.client.module.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.weavemc.api.event.ChatEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

/**
 * Remembers the player's BedWars base even while BedESP is disabled.
 *
 * BedESP modules are unsubscribed from the event bus while disabled, so keeping
 * the game-start/respawn anchor inside the module loses the only reliable own
 * base signal when the module is enabled later in the match.
 */
public final class BedwarsBaseAnchorTracker {
    private final Minecraft minecraft = Minecraft.getMinecraft();
    private World trackedWorld;
    private BlockPos anchor;
    private boolean captureOnNextTick;

    @SubscribeEvent
    public void onChat(ChatEvent.Received event) {
        String message = event.getMessage() == null
                ? null : event.getMessage().getUnformattedText();
        if (BedwarsBaseAnchorTrigger.matches(message)) captureOnNextTick = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        if (minecraft.theWorld == null || minecraft.thePlayer == null) {
            trackedWorld = null;
            anchor = null;
            return;
        }
        if (trackedWorld != minecraft.theWorld) {
            trackedWorld = minecraft.theWorld;
            anchor = null;
        }
        if (!captureOnNextTick) return;
        captureOnNextTick = false;
        anchor = minecraft.thePlayer.getPosition();
        System.out.println("[Veyra] Captured BedWars own-base anchor at " + anchor + '.');
    }

    public BlockPos anchorFor(World world) {
        return world != null && world == trackedWorld ? anchor : null;
    }
}
