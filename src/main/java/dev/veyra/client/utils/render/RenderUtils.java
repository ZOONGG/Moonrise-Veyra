package dev.veyra.client.utils.render;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class RenderUtils {
    private static final int CORNER_STEP_DEGREES = 12;
    private static final int SAVED_GL_STATE = GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
            | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT;
    public static void setColor(final int color) {
        final float a = ((color >> 24) & 0xFF) / 255.0f;
        final float r = ((color >> 16) & 0xFF) / 255.0f;
        final float g = ((color >> 8) & 0xFF) / 255.0f;
        final float b = (color & 0xFF) / 255.0f;
        GL11.glColor4f(r, g, b, a);
    }

    public static void drawRoundedRect(float x, float y, float x1, float y1, final float radius, final int color) {
        drawRoundedRect(x, y, x1, y1, radius, color,  new boolean[] {true,true,true,true} );
    }

    public static void drawRoundedRect(float x, float y, float x1, float y1, final float radius, final int color, boolean[] round) {
        GL11.glPushAttrib(SAVED_GL_STATE);
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        x *= 2.0;
        y *= 2.0;
        x1 *= 2.0;
        y1 *= 2.0;
        GL11.glEnable(3042);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(3553);
        GL11.glEnable(2848);
        setColor(color);
        GL11.glEnable(2848);
        GL11.glBegin(GL11.GL_POLYGON);
        round(x, y, x1, y1, radius, round);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void roundHelper(float x, float y, float radius, int pn, int pn2, int originalRotation, int finalRotation) {
        for (int i = originalRotation; i < finalRotation; i += CORNER_STEP_DEGREES) {
            GL11.glVertex2d(x + (radius * -pn) + (Math.sin((i * 3.141592653589793) / 180.0) * radius * pn), y + (radius * pn2) + (Math.cos((i * 3.141592653589793) / 180.0) * radius * pn));
        }
        int i = finalRotation;
        GL11.glVertex2d(x + (radius * -pn) + (Math.sin((i * Math.PI) / 180.0) * radius * pn),
                y + (radius * pn2) + (Math.cos((i * Math.PI) / 180.0) * radius * pn));
    }

    public static void drawRoundedOutline(float x, float y, float x1, float y1, final float radius, final float borderSize, final int color) {
        drawRoundedOutline(x, y, x1, y1, radius, borderSize , color, new boolean[] {true,true,true,true});
    }

    public static void round(float x, float y, float x1, float y1, float radius, final boolean[] round) {
        if(round[0])
            roundHelper(x, y, radius, -1, 1,0, 90);
        else
            GL11.glVertex2d(x, y);

        if(round[1])
            roundHelper(x, y1, radius, -1, -1, 90, 180);
        else
            GL11.glVertex2d(x, y1);

        if(round[2])
            roundHelper(x1, y1, radius, 1, -1, 0, 90);
        else
            GL11.glVertex2d(x1, y1);

        if(round[3])
            roundHelper(x1, y, radius, 1, 1, 90, 180);
        else
            GL11.glVertex2d(x1, y);
    }


    public static void drawRoundedOutline(float x, float y, float x1, float y1, final float radius, final float borderSize, final int color, boolean[] drawCorner) {
        GL11.glPushAttrib(SAVED_GL_STATE);
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        x *= 2.0;
        y *= 2.0;
        x1 *= 2.0;
        y1 *= 2.0;
        GL11.glEnable(3042);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(3553);
        setColor(color);
        GL11.glEnable(2848);
        GL11.glLineWidth(borderSize);
        GL11.glBegin(2);
        round(x, y, x1, y1, radius, drawCorner);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        GL11.glLineWidth(1.0f);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawBorderedRoundedRect1(float x, float y, float x1, float y1, float radius, float borderSize, int borderC, int insideC) {
        drawRoundedRect(x, y, x1, y1, radius, insideC);
        drawRoundedOutline(x, y, x1, y1, radius, borderSize, borderC);
    }

    public static int rainbowDraw(long speed, long... delay) {
        long time = System.currentTimeMillis() + (delay.length > 0 ? delay[0] : 0L);
        return Color.getHSBColor((float) (time % (15000L / speed)) / (15000.0F / (float) speed), 1.0F, 1.0F).getRGB();
    }
}
