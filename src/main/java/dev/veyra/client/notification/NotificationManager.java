package dev.veyra.client.notification;

import dev.veyra.client.clickgui.UiTokens;
import dev.veyra.client.clickgui.FrameMotion;
import dev.veyra.client.clickgui.font.VeyraFont;
import dev.veyra.client.clickgui.font.VeyraFonts;
import dev.veyra.client.module.modules.client.ClickGuiModule;
import dev.veyra.client.utils.player.PlayerUtils;
import dev.veyra.client.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.weavemc.api.event.RenderGameOverlayEvent;
import net.weavemc.api.event.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class NotificationManager {
    private static final long ENTER_MS = 340L;
    private static final long HOLD_MS = 1_500L;
    private static final long EXIT_MS = 300L;
    private static final long LIFETIME_MS = ENTER_MS + HOLD_MS + EXIT_MS;
    private static final int CARD_HEIGHT = 30;
    private static final int CARD_GAP = 6;
    private static final float LEFT_MARGIN = 10.0F;
    private static final float TOP_MARGIN = 10.0F;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final NotificationQueue queue = new NotificationQueue(3, LIFETIME_MS);
    private final Map<String, Float> animatedRows = new HashMap<>();
    private long lastFrameNanos;

    public void postModuleState(String moduleName, boolean enabled) {
        if (!ClickGuiModule.notificationsEnabled()) return;
        queue.post(moduleName, enabled, System.nanoTime() / 1_000_000L);
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!ClickGuiModule.notificationsEnabled()) {
            queue.clear();
            animatedRows.clear();
            lastFrameNanos = System.nanoTime();
            return;
        }
        long nowMs = System.nanoTime() / 1_000_000L;
        List<NotificationQueue.Entry> entries = queue.visibleAt(nowMs);
        if (entries.isEmpty()
                || !PlayerUtils.isPlayerInGame()
                || mc.currentScreen != null
                || mc.gameSettings.showDebugInfo) {
            if (entries.isEmpty()) animatedRows.clear();
            lastFrameNanos = System.nanoTime();
            return;
        }

        long nowNanos = System.nanoTime();
        float deltaSeconds = lastFrameNanos == 0L
                ? 1.0F / 60.0F
                : Math.min(0.05F, Math.max(0.0F,
                (nowNanos - lastFrameNanos) / 1_000_000_000.0F));
        lastFrameNanos = nowNanos;

        Set<String> visibleKeys = new HashSet<>();
        for (int index = 0; index < entries.size(); index++) {
            NotificationQueue.Entry entry = entries.get(index);
            String key = entry.moduleName().toLowerCase(Locale.ROOT);
            visibleKeys.add(key);
            float targetY = TOP_MARGIN + index * (CARD_HEIGHT + CARD_GAP);
            float currentY = animatedRows.getOrDefault(key, targetY);
            currentY = FrameMotion.expApproach(currentY, targetY, 22.0F, deltaSeconds);
            animatedRows.put(key, currentY);
            render(entry, currentY, nowMs);
        }
        animatedRows.keySet().removeIf(key -> !visibleKeys.contains(key));
    }

    private void render(NotificationQueue.Entry entry, float y, long nowMs) {
        long ageMs = Math.max(0L, nowMs - entry.startedAtMs());
        float visibility = NotificationMotion.visibility(
                ageMs, ENTER_MS, HOLD_MS, EXIT_MS);
        int alpha = Math.round(255.0F * visibility);

        VeyraFont regular = VeyraFonts.regular();
        VeyraFont semibold = VeyraFonts.semibold();
        String state = entry.enabled() ? "ON" : "OFF";
        float nameWidth = semibold.width(entry.moduleName(), 9.2F);
        float stateWidth = semibold.width(state, 8.2F);
        int width = Math.max(148, Math.round(nameWidth + stateWidth + 42.0F));
        float x = LEFT_MARGIN - (1.0F - visibility) * (width + LEFT_MARGIN + 2.0F);

        int accent = entry.enabled() ? UiTokens.Color.SUCCESS : UiTokens.Color.DANGER;
        int background = withAlpha(UiTokens.Color.PANEL, Math.round(238.0F * visibility));
        int border = withAlpha(UiTokens.Color.BORDER, Math.round(205.0F * visibility));
        int text = withAlpha(UiTokens.Color.TEXT_PRIMARY, alpha);
        int secondary = withAlpha(UiTokens.Color.TEXT_SECONDARY, alpha);
        int accentVisible = withAlpha(accent, alpha);

        RenderUtils.drawRoundedRect(x, y, x + width, y + CARD_HEIGHT,
                UiTokens.Radius.MD, background);
        RenderUtils.drawRoundedOutline(x, y, x + width, y + CARD_HEIGHT,
                UiTokens.Radius.MD, 0.8F, border);
        RenderUtils.drawRoundedRect(x + 8.0F, y + 8.0F, x + 12.0F, y + 22.0F,
                2.0F, accentVisible);

        semibold.drawPlain(entry.moduleName(), x + 19.0F, y + 9.5F, text, 9.2F);
        regular.drawPlain(state, x + width - stateWidth - 10.0F, y + 10.0F,
                entry.enabled() ? accentVisible : secondary, 8.2F);
    }

    private static int withAlpha(int color, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | color & 0x00FFFFFF;
    }
}
