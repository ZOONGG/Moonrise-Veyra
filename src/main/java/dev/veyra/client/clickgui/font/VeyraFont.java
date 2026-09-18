package dev.veyra.client.clickgui.font;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public final class VeyraFont {
    private static final int ATLAS_SIZE = 1024;
    private static final int PADDING = 2;
    private static final String GLYPHS = buildGlyphSet();

    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private final DynamicTexture texture;
    private final float nativeHeight;

    public VeyraFont(Font font) {
        BufferedImage metricsImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D metricsGraphics = metricsImage.createGraphics();
        configure(metricsGraphics);
        metricsGraphics.setFont(font);
        FontMetrics metrics = metricsGraphics.getFontMetrics();
        nativeHeight = metrics.getHeight() + PADDING * 2.0F;
        metricsGraphics.dispose();

        BufferedImage atlas = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        configure(graphics);
        graphics.setFont(font);
        graphics.setColor(Color.WHITE);

        int x = PADDING;
        int y = PADDING;
        int rowHeight = 0;
        for (char character : GLYPHS.toCharArray()) {
            int advance = Math.max(1, metrics.charWidth(character));
            int glyphWidth = advance + PADDING * 2;
            int glyphHeight = metrics.getHeight() + PADDING * 2;
            if (x + glyphWidth >= ATLAS_SIZE) {
                x = PADDING;
                y += rowHeight;
                rowHeight = 0;
            }
            if (y + glyphHeight >= ATLAS_SIZE) {
                break;
            }

            graphics.drawString(String.valueOf(character), x + PADDING, y + PADDING + metrics.getAscent());
            glyphs.put(character, new Glyph(x, y, glyphWidth, glyphHeight, advance));
            x += glyphWidth;
            rowHeight = Math.max(rowHeight, glyphHeight);
        }
        graphics.dispose();
        texture = new DynamicTexture(atlas);
        // Nearest sampling keeps 1px stems readable in Minecraft's scaled GUI.
        texture.setBlurMipmap(false, false);
    }

    public void draw(String text, float x, float y, int color, float height) {
        draw(text, x + 0.5F, y + 0.5F, withAlpha(0xFF000000, color, 0.42F), height, false);
        draw(text, x, y, color, height, false);
    }

    public void drawPlain(String text, float x, float y, int color, float height) {
        draw(text, x, y, color, height, false);
    }

    public float width(String text, float height) {
        return width(text, height, 0.0F);
    }

    public float width(String text, float height, float tracking) {
        float scale = height / nativeHeight;
        float width = 0.0F;
        char[] characters = text.toCharArray();
        for (int index = 0; index < characters.length; index++) {
            char character = characters[index];
            Glyph glyph = glyphs.getOrDefault(character, glyphs.get('?'));
            if (glyph != null) {
                width += glyph.advance * scale;
                if (index + 1 < characters.length) width += tracking;
            }
        }
        return width;
    }

    public void drawPlainTracked(String text, float x, float y, int color,
                                 float height, float tracking) {
        draw(text, x, y, color, height, tracking);
    }

    private void draw(String text, float x, float y, int color, float height, boolean ignored) {
        draw(text, x, y, color, height, 0.0F);
    }

    private void draw(String text, float x, float y, int color, float height, float tracking) {
        if (text == null || text.isEmpty()) {
            return;
        }

        float scale = height / nativeHeight;
        float alpha = (float)(color >>> 24 & 255) / 255.0F;
        float red = (float)(color >>> 16 & 255) / 255.0F;
        float green = (float)(color >>> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(texture.getGlTextureId());
        GlStateManager.color(red, green, blue, alpha);

        float cursor = snapHalf(x);
        float drawY = snapHalf(y);
        GL11.glBegin(GL11.GL_QUADS);
        for (char character : text.toCharArray()) {
            Glyph glyph = glyphs.getOrDefault(character, glyphs.get('?'));
            if (glyph == null) {
                continue;
            }

            float drawWidth = glyph.width * scale;
            float drawHeight = glyph.height * scale;
            // Half-texel inset prevents neighbouring glyphs leaking into
            // thin strokes when the GUI scale is fractional.
            float u0 = (glyph.x + 0.5F) / ATLAS_SIZE;
            float v0 = (glyph.y + 0.5F) / ATLAS_SIZE;
            float u1 = (glyph.x + glyph.width - 0.5F) / ATLAS_SIZE;
            float v1 = (glyph.y + glyph.height - 0.5F) / ATLAS_SIZE;

            GL11.glTexCoord2f(u0, v0);
            float drawLeft = snapHalf(cursor);
            float drawRight = snapHalf(cursor + drawWidth);
            GL11.glVertex2f(drawLeft, drawY);
            GL11.glTexCoord2f(u0, v1);
            GL11.glVertex2f(drawLeft, drawY + drawHeight);
            GL11.glTexCoord2f(u1, v1);
            GL11.glVertex2f(drawRight, drawY + drawHeight);
            GL11.glTexCoord2f(u1, v0);
            GL11.glVertex2f(drawRight, drawY);
            cursor += glyph.advance * scale + tracking;
        }
        GL11.glEnd();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static float snapHalf(float value) {
        return Math.round(value * 2.0F) / 2.0F;
    }

    private static int withAlpha(int base, int source, float multiplier) {
        int alpha = Math.max(0, Math.min(255, Math.round((source >>> 24 & 255) * multiplier)));
        return alpha << 24 | base & 0x00FFFFFF;
    }

    private static String buildGlyphSet() {
        StringBuilder builder = new StringBuilder();
        for (char character = 32; character <= 126; character++) {
            builder.append(character);
        }
        for (char character = '\u0400'; character <= '\u04FF'; character++) {
            builder.append(character);
        }
        builder.append("—–…•×✓★☆");
        return builder.toString();
    }

    private static final class Glyph {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int advance;

        private Glyph(int x, int y, int width, int height, int advance) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.advance = advance;
        }
    }
}
