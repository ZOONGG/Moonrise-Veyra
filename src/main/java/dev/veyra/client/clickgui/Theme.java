package dev.veyra.client.clickgui;

import dev.veyra.client.module.modules.client.ClickGuiModule;

import java.awt.Color;

public final class Theme {
    public static final int BACKDROP = UiTokens.Color.BACKDROP_SCRIM;
    public static final int BACKDROP_TOP = UiTokens.Color.BACKDROP_TOP;
    public static final int BACKDROP_BOTTOM = UiTokens.Color.BACKDROP_BOTTOM;
    public static final int WINDOW = UiTokens.Color.WINDOW;
    public static final int TOPBAR = UiTokens.Color.TOPBAR;
    public static final int SIDEBAR = UiTokens.Color.NAV;
    public static final int PANEL_INT = UiTokens.Color.PANEL;
    public static final int SURFACE_INT = UiTokens.Color.SURFACE;
    public static final int SURFACE_HOVER_INT = UiTokens.Color.SURFACE_HOVER;
    public static final int SURFACE_SELECTED = UiTokens.Color.SURFACE_SELECTED;
    public static final int INPUT = UiTokens.Color.INPUT;
    public static final int INPUT_ACTIVE = UiTokens.Color.INPUT_FOCUS;
    public static final int BORDER_INT = UiTokens.Color.BORDER;
    public static final int BORDER_SOFT = UiTokens.Color.BORDER_SOFT;
    public static final int TEXT_PRIMARY_INT = UiTokens.Color.TEXT_PRIMARY;
    public static final int TEXT_SECONDARY_INT = UiTokens.Color.TEXT_SECONDARY;
    public static final int TEXT_MUTED_INT = UiTokens.Color.TEXT_MUTED;
    public static final int ACCENT_INT = 0xFFA86CFF;
    public static final int ACCENT_DARK = 0xFF705CFF;
    public static final int ACCENT_BLUE = 0xFF5D8BFF;
    public static final int SUCCESS = UiTokens.Color.SUCCESS;
    public static final int DANGER = UiTokens.Color.DANGER;
    public static final int TRACK_INT = UiTokens.Color.TRACK;

    private static final Color PANEL = new Color(PANEL_INT, true);
    private static final Color SURFACE = new Color(SURFACE_INT, true);
    private static final Color SURFACE_HOVER = new Color(SURFACE_HOVER_INT, true);
    private static final Color TRACK = new Color(TRACK_INT, true);
    private static final Color TEXT_PRIMARY = new Color(TEXT_PRIMARY_INT, true);
    private static final Color TEXT_SECONDARY = new Color(TEXT_SECONDARY_INT, true);
    private static final Color DISABLED = new Color(UiTokens.Color.TEXT_DISABLED, true);
    private static final Color SCRIM = new Color(BACKDROP, true);

    private Theme() {
    }

    public static Color getMainColor() {
        ClickGuiModule.Colors preset = ClickGuiModule.clientTheme == null
                ? ClickGuiModule.Colors.Veyra
                : ClickGuiModule.clientTheme.getMode();

        switch (preset) {
            case Aurora:
                return new Color(34, 211, 238);
            case Glacier:
                return new Color(96, 165, 250);
            case Ember:
                return new Color(251, 113, 133);
            case Lime:
                return new Color(163, 230, 53);
            case Mono:
                return new Color(226, 232, 240);
            case Veyra:
            default:
                return new Color(139, 92, 246);
        }
    }

    public static Color getBackColor() {
        return PANEL;
    }

    public static Color getSurfaceColor() {
        return SURFACE;
    }

    public static Color getSurfaceHoverColor() {
        return SURFACE_HOVER;
    }

    public static Color getTrackColor() {
        return TRACK;
    }

    public static Color getTextPrimaryColor() {
        return TEXT_PRIMARY;
    }

    public static Color getTextSecondaryColor() {
        return TEXT_SECONDARY;
    }

    public static Color getDisabledColor() {
        return DISABLED;
    }

    public static Color getScrimColor() {
        return SCRIM;
    }

    /** Stable pastel accent for a category without turning the UI into a rainbow. */
    public static int categoryAccent(int categoryIndex) {
        final int[] palette = {
                0xFFFF668F, 0xFF75A7FF, 0xFFB681FF, 0xFF5EDBC7, 0xFFA67CFF
        };
        return palette[Math.floorMod(categoryIndex, palette.length)];
    }

    /**
     * Each module receives its own nearby shade. The name keeps it stable while
     * the category keeps related modules visually coherent.
     */
    public static int moduleAccent(String moduleName, int categoryIndex) {
        Color base = new Color(categoryAccent(categoryIndex), true);
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        int hash = moduleName == null ? 0 : moduleName.toLowerCase(java.util.Locale.ROOT).hashCode();
        float hueOffset = ((Math.floorMod(hash, 101) / 100.0F) - 0.5F) * 0.075F;
        float saturation = 0.48F + Math.floorMod(hash >>> 8, 25) / 100.0F;
        float brightness = 0.90F + Math.floorMod(hash >>> 16, 10) / 100.0F;
        return 0xFF000000 | (Color.HSBtoRGB(wrapHue(hsb[0] + hueOffset),
                Math.min(0.76F, saturation), Math.min(1.0F, brightness)) & 0x00FFFFFF);
    }

    private static float wrapHue(float hue) {
        while (hue < 0.0F) hue += 1.0F;
        while (hue > 1.0F) hue -= 1.0F;
        return hue;
    }
}
