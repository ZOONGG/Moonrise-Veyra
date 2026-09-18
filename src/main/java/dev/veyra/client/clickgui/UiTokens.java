package dev.veyra.client.clickgui;

/**
 * Visual contract for Veyra's desktop UI.
 *
 * <p>Values live here instead of inside individual screens so Stage 2 can
 * compose new layouts without inventing a second palette or spacing scale.</p>
 */
public final class UiTokens {
    private UiTokens() {
    }

    public static final class Color {
        public static final int BACKDROP_TOP = 0xE50A0D16;
        public static final int BACKDROP_BOTTOM = 0xF2070910;
        public static final int BACKDROP_SCRIM = 0x8E02040A;
        public static final int WINDOW = 0xF70D111B;
        public static final int TOPBAR = 0xFA121722;
        public static final int NAV = 0xF80E131D;
        public static final int CONTENT = 0xF40A0E17;
        public static final int PANEL = 0xFB121823;
        public static final int SURFACE = 0xFF171E2A;
        public static final int SURFACE_RAISED = 0xFF1B2331;
        public static final int SURFACE_HOVER = 0xFF202A39;
        public static final int SURFACE_PRESSED = 0xFF151B26;
        public static final int SURFACE_SELECTED = 0xFF232A38;
        public static final int INPUT = 0xFF111721;
        public static final int INPUT_HOVER = 0xFF161D29;
        public static final int INPUT_FOCUS = 0xFF192231;
        public static final int BORDER = 0xFF303B4E;
        public static final int BORDER_SOFT = 0x9E263043;
        public static final int DIVIDER = 0xB9232C3A;
        public static final int TEXT_PRIMARY = 0xFFE3E7EE;
        public static final int TEXT_SECONDARY = 0xFFB9C1CD;
        public static final int TEXT_MUTED = 0xFF929CAA;
        public static final int TEXT_DISABLED = 0xFF697487;
        public static final int TRACK = 0xFF303949;
        public static final int TRACK_INSET = 0xFF202735;
        public static final int KNOB = 0xFFF8FAFC;
        public static final int SUCCESS = 0xFF38D69A;
        public static final int WARNING = 0xFFFFC857;
        public static final int DANGER = 0xFFFF7088;
        public static final int SHADOW_AMBIENT = 0x24000000;
        public static final int SHADOW_KEY = 0x52000000;

        private Color() {
        }
    }

    public static final class Space {
        public static final int XS = 4;
        public static final int SM = 6;
        public static final int MD = 8;
        public static final int LG = 12;
        public static final int XL = 16;

        private Space() {
        }
    }

    public static final class Radius {
        public static final float XS = 2.0F;
        public static final float SM = 4.0F;
        public static final float MD = 6.0F;
        public static final float LG = 8.0F;
        public static final float WINDOW = 9.0F;

        private Radius() {
        }
    }

    public static final class Type {
        public static final float DISPLAY = 11.5F;
        public static final float TITLE = 10.0F;
        public static final float BODY = 8.8F;
        public static final float LABEL = 8.0F;
        public static final float CAPTION = 8.4F;

        private Type() {
        }
    }

    public static final class Motion {
        public static final float OPEN_SPEED = 9.5F;
        public static final float CATEGORY_SPEED = 11.0F;
        public static final float EXPAND_SPEED = 10.0F;
        public static final float HOVER_SPEED = 14.0F;
        public static final float REORDER_SPEED = 16.0F;
        public static final float TOGGLE_SPEED = 18.0F;

        private Motion() {
        }
    }

}
