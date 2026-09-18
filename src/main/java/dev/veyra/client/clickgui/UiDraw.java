package dev.veyra.client.clickgui;

import dev.veyra.client.utils.render.RenderUtils;

/** Low-cost reusable drawing primitives shared by Veyra screens. */
public final class UiDraw {
    private UiDraw() {
    }

    public static void surface(float left, float top, float right, float bottom,
                               float radius, int fill, int border, int elevation) {
        if (elevation >= 2) {
            RenderUtils.drawRoundedRect(left - 2, top + 3, right + 2, bottom + 5,
                    radius + 1, UiTokens.Color.SHADOW_AMBIENT);
        }
        if (elevation >= 1) {
            RenderUtils.drawRoundedRect(left - 1, top + 1, right + 1, bottom + 2,
                    radius + 1, UiTokens.Color.SHADOW_KEY);
        }
        RenderUtils.drawRoundedRect(left, top, right, bottom, radius, fill);
        if ((border >>> 24) != 0) {
            RenderUtils.drawRoundedOutline(left, top, right, bottom, radius, 0.7F, border);
        }
    }

    public static void control(float left, float top, float right, float bottom,
                               boolean hovered, boolean focused, int accent) {
        int fill = focused ? UiTokens.Color.INPUT_FOCUS
                : hovered ? UiTokens.Color.INPUT_HOVER : UiTokens.Color.INPUT;
        int border = focused ? withAlpha(accent, 180) : UiTokens.Color.BORDER_SOFT;
        surface(left, top, right, bottom, UiTokens.Radius.SM, fill, border, focused ? 1 : 0);
    }

    public static void button(float left, float top, float right, float bottom,
                              boolean hovered, boolean active, int accent) {
        int fill = active ? mix(UiTokens.Color.SURFACE_RAISED, accent, 0.12F)
                : hovered ? UiTokens.Color.SURFACE_HOVER : UiTokens.Color.SURFACE;
        int border = active ? withAlpha(accent, 155) : UiTokens.Color.BORDER_SOFT;
        surface(left, top, right, bottom, UiTokens.Radius.SM, fill, border, 0);
    }

    public static void toggle(float left, float top, float right, float bottom,
                               boolean enabled, int accent) {
        toggle(left, top, right, bottom, enabled ? 1.0F : 0.0F, false, false, accent);
    }

    public static void toggle(float left, float top, float right, float bottom,
                              float progress, boolean hovered, boolean pressed, int accent) {
        progress = clamp01(progress);
        float height = bottom - top;
        float radius = height / 2.0F;
        RenderUtils.drawRoundedRect(left, top, right, bottom, radius,
                mix(UiTokens.Color.TRACK_INSET, accent, progress * 0.88F));
        int outline = mix(UiTokens.Color.BORDER,
                withAlpha(accent, hovered ? 220 : 160), progress);
        RenderUtils.drawRoundedOutline(left, top, right, bottom, radius, 0.65F,
                hovered ? mix(outline, 0xFFD6DCE7, 0.18F) : outline);
        float knobSize = height - 4;
        float knobLeft = left + 2 + (right - left - knobSize - 4) * progress;
        if (pressed) knobSize = Math.max(4.0F, knobSize - 1.0F);
        RenderUtils.drawRoundedRect(knobLeft, top + 2, knobLeft + knobSize,
                top + 2 + knobSize, knobSize / 2.0F, UiTokens.Color.KNOB);
    }

    public static void slider(float left, float top, float right, float bottom,
                              float first, float second, int accent) {
        float radius = Math.max(1.0F, (bottom - top) / 2.0F);
        RenderUtils.drawRoundedRect(left, top - 1, right, bottom + 1,
                radius + 1, UiTokens.Color.TRACK_INSET);
        RenderUtils.drawRoundedRect(left, top, right, bottom, radius, UiTokens.Color.TRACK);
        float firstX = left + (right - left) * clamp01(first);
        if (second < 0.0F) {
            RenderUtils.drawRoundedRect(left, top, firstX, bottom, radius, accent);
            sliderKnob(firstX, (top + bottom) / 2.0F, accent);
        } else {
            float secondX = left + (right - left) * clamp01(second);
            RenderUtils.drawRoundedRect(firstX, top, secondX, bottom, radius, accent);
            sliderKnob(firstX, (top + bottom) / 2.0F, accent);
            sliderKnob(secondX, (top + bottom) / 2.0F, accent);
        }
    }

    private static void sliderKnob(float x, float centerY, int accent) {
        RenderUtils.drawRoundedRect(x - 5, centerY - 4, x + 5, centerY + 6,
                5, UiTokens.Color.SHADOW_KEY);
        RenderUtils.drawRoundedRect(x - 4, centerY - 4, x + 4, centerY + 4,
                4, UiTokens.Color.KNOB);
        RenderUtils.drawRoundedOutline(x - 4, centerY - 4, x + 4, centerY + 4,
                4, 0.65F, withAlpha(accent, 190));
    }

    public static int withAlpha(int color, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | color & 0x00FFFFFF;
    }

    public static int mix(int first, int second, float amount) {
        amount = clamp01(amount);
        int a = Math.round((first >>> 24 & 255) + ((second >>> 24 & 255) - (first >>> 24 & 255)) * amount);
        int r = Math.round((first >> 16 & 255) + ((second >> 16 & 255) - (first >> 16 & 255)) * amount);
        int g = Math.round((first >> 8 & 255) + ((second >> 8 & 255) - (first >> 8 & 255)) * amount);
        int b = Math.round((first & 255) + ((second & 255) - (first & 255)) * amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
