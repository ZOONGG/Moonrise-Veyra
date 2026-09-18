package dev.veyra.client.clickgui.components;

import dev.veyra.client.module.setting.impl.DoubleSliderSetting;
import dev.veyra.client.utils.Utils;
import dev.veyra.client.utils.math.MathUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import dev.veyra.client.clickgui.Component;
import dev.veyra.client.clickgui.Theme;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RangeSliderComponent implements Component {
    private final DoubleSliderSetting doubleSlider;
    private final ModuleComponent module;
    private double barWidth;
    private double blankWidth;
    private int sliderStartX;
    private int sliderStartY;
    private int moduleStartY;
    private boolean mouseDown;
    private Helping mode = Helping.NONE;

    private final int boxMargin = 4;

    public RangeSliderComponent(DoubleSliderSetting doubleSlider, ModuleComponent module, int moduleStartY){
        this.doubleSlider = doubleSlider;
        this.module = module;
        this.sliderStartX = this.module.category.getX() + boxMargin;
        this.sliderStartY = moduleStartY + module.category.getY();
        this.moduleStartY = moduleStartY;
    }

    public void draw(){
        int boxHeight = 4;
        int textSize = 11;
        net.minecraft.client.gui.Gui.drawRect(this.module.category.getX() + boxMargin, this.module.category.getY() + this.moduleStartY + textSize, this.module.category.getX() - boxMargin + this.module.category.getWidth(), this.module.category.getY() + this.moduleStartY + textSize + boxHeight, Theme.getTrackColor().getRGB());
        int startToDrawFrom = this.module.category.getX() + boxMargin + (int) this.blankWidth;
        int finishDrawingAt = startToDrawFrom + (int)this.barWidth;
        int minHandle = startToDrawFrom;
        int maxHandle = finishDrawingAt;

        net.minecraft.client.gui.Gui.drawRect(startToDrawFrom, this.module.category.getY() + this.moduleStartY + textSize, finishDrawingAt, this.module.category.getY() + this.moduleStartY + textSize + boxHeight, Theme.getMainColor().getRGB());
        net.minecraft.client.gui.Gui.drawRect(minHandle - 1, this.module.category.getY() + this.moduleStartY + textSize - 2, minHandle + 1, this.module.category.getY() + this.moduleStartY + textSize + boxHeight + 2, Theme.getTextPrimaryColor().getRGB());
        net.minecraft.client.gui.Gui.drawRect(maxHandle - 1, this.module.category.getY() + this.moduleStartY + textSize - 2, maxHandle + 1, this.module.category.getY() + this.moduleStartY + textSize + boxHeight + 2, Theme.getTextPrimaryColor().getRGB());

        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(this.doubleSlider.getName() + ": " + format(this.doubleSlider.getInputMin()) + "–" + format(this.doubleSlider.getInputMax()), (float)((int)((float)(this.module.category.getX() + 4) * 2.0F)), (float)((int)((float)(this.module.category.getY() + this.moduleStartY + 3) * 2.0F)), Theme.getTextPrimaryColor().getRGB());
        GL11.glPopMatrix();
    }

    public void setComponentStartAt(int posY) {
        this.moduleStartY = posY;
    }

    @Override
    public int getHeight() {
        return 16;
    }

    public void update(int mousePosX, int mousePosY){
        this.sliderStartY = this.module.category.getY() + this.moduleStartY;
        this.sliderStartX = this.module.category.getX() + boxMargin;

        double mousePressedAt = Math.min(this.module.category.getWidth() - boxMargin*2, Math.max(0, mousePosX - this.sliderStartX));
        this.blankWidth = (double)(this.module.category.getWidth() - boxMargin*2)
                * (this.doubleSlider.getInputMin() - this.doubleSlider.getMin())
                / (this.doubleSlider.getMax() - this.doubleSlider.getMin());
        this.barWidth = (double)(this.module.category.getWidth() - boxMargin*2)
                * (this.doubleSlider.getInputMax() - this.doubleSlider.getInputMin())
                / (this.doubleSlider.getMax() - this.doubleSlider.getMin());

        if(this.mouseDown) {
            if (mode == Helping.NONE && Math.abs(barWidth) < 0.0001D) {
                double trackWidth = this.module.category.getWidth() - boxMargin * 2.0D;
                mode = blankWidth <= 0.0001D
                        ? Helping.MAX
                        : blankWidth >= trackWidth - 0.0001D
                        ? Helping.MIN
                        : mousePressedAt >= blankWidth ? Helping.MAX : Helping.MIN;
            }
            if (mousePressedAt > blankWidth + barWidth / 2 || mode == Helping.MAX) {
                if (this.mode == Helping.NONE) this.mode = Helping.MAX;
                if (this.mode == Helping.MAX) {
                    if (mousePressedAt <= blankWidth) {
                        this.doubleSlider.setValueMax(this.doubleSlider.getInputMin());
                    } else {
                        double n = r(mousePressedAt
                                / (double) (this.module.category.getWidth() - boxMargin * 2)
                                * (this.doubleSlider.getMax() - this.doubleSlider.getMin())
                                + this.doubleSlider.getMin(), 2);
                        this.doubleSlider.setValueMax(n);
                    }
                }
            }

            if (mousePressedAt < blankWidth + barWidth / 2 || mode == Helping.MIN) {
                if (this.mode == Helping.NONE) this.mode = Helping.MIN;
                if(this.mode == Helping.MIN) {
                    if (mousePressedAt == 0.0D) {
                        this.doubleSlider.setValueMin(this.doubleSlider.getMin());
                    } else if(mousePressedAt >= barWidth + blankWidth){
                        this.doubleSlider.setValueMin(this.doubleSlider.getMax());
                    }else {
                        double n = r(mousePressedAt
                                / (double) (this.module.category.getWidth() - boxMargin*2)
                                * (this.doubleSlider.getMax() - this.doubleSlider.getMin())
                                + this.doubleSlider.getMin(), 2);
                        this.doubleSlider.setValueMin(n);
                    }
                }
            }
        } else {
            if(mode != Helping.NONE) mode = Helping.NONE;
        }

    }

    private static double r(double v, int p) {
        if (p < 0) {
            return 0.0D;
        } else {
            BigDecimal bd = new BigDecimal(v);
            bd = bd.setScale(p, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
    }

    private static String format(double value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Double.toString(value);
    }

    public void mouseDown(int x, int y, int b) {
        if (this.u(x, y) && b == 0 && this.module.po) {
            this.mouseDown = true;
        }

        if (this.i(x, y) && b == 0 && this.module.po) {
            this.mouseDown = true;
        }

    }

    public void mouseReleased(int x, int y, int m) {
        this.mouseDown = false;
    }

    @Override
    public void keyTyped(char t, int k) {

    }

    public boolean u(int x, int y) {
        return x > this.sliderStartX && x < this.sliderStartX + this.module.category.getWidth() / 2 + 1 && y > this.sliderStartY && y < this.sliderStartY + 16;
    }

    public boolean i(int x, int y) {
        return x > this.sliderStartX + this.module.category.getWidth() / 2 && x < this.sliderStartX + this.module.category.getWidth() && y > this.sliderStartY && y < this.sliderStartY + 16;
    }

    public enum Helping {
        MIN, MAX, NONE
    }
}
