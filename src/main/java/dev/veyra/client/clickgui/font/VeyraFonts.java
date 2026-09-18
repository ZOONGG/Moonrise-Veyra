package dev.veyra.client.clickgui.font;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class VeyraFonts {
    private static final VeyraFont REGULAR = new VeyraFont(load(TextAttribute.WEIGHT_REGULAR));
    private static final VeyraFont SEMIBOLD = new VeyraFont(load(TextAttribute.WEIGHT_SEMIBOLD));

    private VeyraFonts() {
    }

    public static VeyraFont regular() {
        return REGULAR;
    }

    public static VeyraFont semibold() {
        return SEMIBOLD;
    }

    private static Font load(float weight) {
        try (InputStream stream = VeyraFonts.class.getResourceAsStream("/assets/veyra/fonts/Inter.ttf")) {
            if (stream == null) {
                throw new IllegalStateException("Bundled Inter font is missing.");
            }
            Font base = Font.createFont(Font.TRUETYPE_FONT, stream);
            Map<TextAttribute, Object> attributes = new HashMap<>();
            // Rasterize close to the GUI's actual 7-11 px display range.
            // Large atlases scaled down 5-7x destroy stems on Minecraft's
            // legacy texture pipeline and make Inter look hand-drawn.
            attributes.put(TextAttribute.SIZE, 18.0F);
            attributes.put(TextAttribute.WEIGHT, weight);
            return base.deriveFont(attributes);
        } catch (Exception exception) {
            int style = weight >= TextAttribute.WEIGHT_SEMIBOLD ? Font.BOLD : Font.PLAIN;
            return new Font("SansSerif", style, 18);
        }
    }
}
