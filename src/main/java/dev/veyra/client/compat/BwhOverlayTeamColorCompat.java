package dev.veyra.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/** Keeps BWH overlay names synchronized with current Hypixel scoreboard teams. */
public final class BwhOverlayTeamColorCompat {
    private static final String OVERLAY_CLASS = "xyz.mangal.bwhelper.mods.Overlay";
    private static final long UPDATE_INTERVAL_MS = 250L;

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private long nextUpdate;
    private boolean unavailableReported;

    @SubscribeEvent
    public void onTick(TickEvent.Post event) {
        long now = System.currentTimeMillis();
        if (now < nextUpdate || minecraft.getNetHandler() == null) return;
        nextUpdate = now + UPDATE_INTERVAL_MS;

        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> overlay = Class.forName(OVERLAY_CLASS, false, loader);
            Field playersField = overlay.getField("playersInLobby");
            Object rawPlayers = playersField.get(null);
            if (!(rawPlayers instanceof Map<?, ?> players)) return;

            for (Map.Entry<?, ?> entry : players.entrySet()) {
                if (!(entry.getKey() instanceof UUID playerId) || entry.getValue() == null) continue;
                NetworkPlayerInfo info = minecraft.getNetHandler().getPlayerInfo(playerId);
                if (info == null || info.getGameProfile() == null) continue;
                ScorePlayerTeam team = info.getPlayerTeam();
                String teamPrefix = team == null ? null : team.getColorPrefix();
                String formattedTabName = info.getDisplayName() == null
                        ? null : info.getDisplayName().getFormattedText();
                String displayName = BwhVisibleOverlayName.resolve(
                        info.getGameProfile().getName(), teamPrefix, formattedTabName);
                if (displayName == null) continue;

                Object overlayPlayer = entry.getValue();
                Method setDisplayName = overlayPlayer.getClass()
                        .getMethod("setDisplayName", String.class);
                setDisplayName.invoke(overlayPlayer, displayName);
            }
            unavailableReported = false;
        } catch (ClassNotFoundException ignored) {
            // BWH is optional.
        } catch (Exception exception) {
            if (!unavailableReported) {
                unavailableReported = true;
                System.err.println("[Veyra] BWH team-color sync unavailable: "
                        + exception.getClass().getSimpleName());
            }
        }
    }
}
