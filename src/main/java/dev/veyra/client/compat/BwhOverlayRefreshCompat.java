package dev.veyra.client.compat;

import net.weavemc.api.event.ChatEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Optional public-API adapter that refreshes BWH from every Hypixel /who roster. */
public final class BwhOverlayRefreshCompat {
    private static final String OVERLAY_CLASS = "xyz.mangal.bwhelper.mods.Overlay";
    private final BwhRosterRefreshGate refreshGate = new BwhRosterRefreshGate();
    private boolean unavailableReported;

    @SubscribeEvent
    public void onChat(ChatEvent.Received event) {
        BwhRosterParser.Result parsed = BwhRosterParser.parse(
                event.getMessage().getUnformattedText());
        if (!parsed.matched()
                || !refreshGate.shouldRefresh(parsed.players(), System.currentTimeMillis())) {
            return;
        }

        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.getNetHandler() == null) return;

            List<BwhRosterUuidResolver.PlayerIdentity> tabPlayers = new ArrayList<>();
            for (NetworkPlayerInfo info : minecraft.getNetHandler().getPlayerInfoMap()) {
                if (info == null || info.getGameProfile() == null) continue;
                tabPlayers.add(new BwhRosterUuidResolver.PlayerIdentity(
                        info.getGameProfile().getName(), info.getGameProfile().getId()));
            }

            Set<String> playerUuids = BwhRosterUuidResolver.resolve(parsed.players(), tabPlayers);
            if (!parsed.players().isEmpty() && playerUuids.isEmpty()) {
                // Hypixel can emit /who before the TAB list finishes updating. A repeated
                // ONLINE line will retry after the refresh gate's short cooldown.
                return;
            }

            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> overlay = Class.forName(OVERLAY_CLASS, false, loader);
            Method setPlayerList = overlay.getMethod("setPlayerList", Set.class);
            setPlayerList.invoke(null, playerUuids);
            unavailableReported = false;
            System.out.println("[Veyra] Sent " + playerUuids.size()
                    + " current Hypixel player UUID(s) to BWH from /who.");
        } catch (ClassNotFoundException ignored) {
            // BWH is optional; Veyra must work normally without it.
        } catch (Exception exception) {
            if (!unavailableReported) {
                unavailableReported = true;
                System.err.println("[Veyra] BWH roster refresh unavailable: "
                        + exception.getClass().getSimpleName());
            }
        }
    }
}
