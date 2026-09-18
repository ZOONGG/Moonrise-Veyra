package dev.veyra.client.clickgui;

import dev.veyra.client.clickgui.font.VeyraFont;
import dev.veyra.client.clickgui.font.VeyraFonts;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import dev.veyra.client.module.modules.client.ArrayListModule;
import dev.veyra.client.utils.Utils;
import dev.veyra.client.utils.render.RenderUtils;

import java.util.Comparator;

public class ArrayListPosition extends GuiScreen {
    GuiButton resetPosButton;
    boolean mouseDown = false;
    int textBoxStartX = 0;
    int textBoxStartY = 0;
    ScaledResolution sr;
    int textBoxEndX = 0;
    int textBoxEndY = 0;
    int marginX = 5;
    int marginY = 70;
    int lastMousePosX = 0;
    int lastMousePosY = 0;
    int sessionMousePosX = 0;
    int sessionMousePosY = 0;

    public void initGui() {
        super.initGui();
        this.buttonList.add(this.resetPosButton = new GuiButton(1, this.width - 90, 5, 85, 20, "Reset position"));
        this.marginX = ArrayListModule.hudX;
        this.marginY = ArrayListModule.hudY;
        sr = new ScaledResolution(mc);
        ArrayListModule.positionMode = Utils.HUD.getPostitionMode(marginX, marginY, sr.getScaledWidth(), sr.getScaledHeight());
    }

    public void drawScreen(int mX, int mY, float pt) {
        drawRect(0, 0, this.width, this.height, Theme.getScrimColor().getRGB());
        drawRect(0, this.height /2, this.width, this.height /2 + 1, Theme.getTrackColor().getRGB());
        drawRect(this.width /2, 0, this.width /2 + 1, this.height, Theme.getTrackColor().getRGB());
        int textBoxStartX = this.marginX;
        int textBoxStartY = this.marginY;
        int[] previewSize = this.drawArrayList();
        int textBoxEndX = textBoxStartX + previewSize[0];
        int textBoxEndY = textBoxStartY + previewSize[1];
        this.textBoxStartX = textBoxStartX;
        this.textBoxStartY = textBoxStartY;
        this.textBoxEndX = textBoxEndX;
        this.textBoxEndY = textBoxEndY;
        ArrayListModule.hudX = textBoxStartX;
        ArrayListModule.hudY = textBoxStartY;
        ScaledResolution res = new ScaledResolution(this.mc);
        int descriptionOffsetX = res.getScaledWidth() / 2 - 84;
        int descriptionOffsetY = res.getScaledHeight() / 2 - 20;
        VeyraFonts.semibold().drawPlain("Drag the preview to position it",
                descriptionOffsetX, descriptionOffsetY, Theme.TEXT_PRIMARY_INT, 9.0F);
        VeyraFonts.regular().drawPlain("Preview uses your live ArrayList style",
                descriptionOffsetX, descriptionOffsetY + 14, Theme.TEXT_MUTED_INT, 6.7F);

        this.handleInput();

        super.drawScreen(mX, mY, pt);
    }

    private int[] drawArrayList() {
        dev.veyra.client.module.Module module = dev.veyra.client.main.Veyra.moduleManager == null
                ? null : dev.veyra.client.main.Veyra.moduleManager.getModuleByClazz(ArrayListModule.class);
        if (module instanceof ArrayListModule arrayList) {
            return arrayList.renderPositionPreview(this.marginX, this.marginY);
        }
        return new int[]{92, 39};
    }

    public void mouseClickMove(int mousePosX, int mousePosY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mousePosX, mousePosY, clickedMouseButton, timeSinceLastClick);
        if (clickedMouseButton == 0) {
            if (this.mouseDown) {
                this.marginX = this.lastMousePosX + (mousePosX - this.sessionMousePosX);
                this.marginY = this.lastMousePosY + (mousePosY - this.sessionMousePosY);
                sr = new ScaledResolution(mc);
                ArrayListModule.positionMode = Utils.HUD.getPostitionMode(marginX, marginY,sr.getScaledWidth(), sr.getScaledHeight());

                //in the else if statement, we check if the mouse is clicked AND inside the "text box"
            } else if (mousePosX > this.textBoxStartX && mousePosX < this.textBoxEndX && mousePosY > this.textBoxStartY && mousePosY < this.textBoxEndY) {
                this.mouseDown = true;
                this.sessionMousePosX = mousePosX;
                this.sessionMousePosY = mousePosY;
                this.lastMousePosX = this.marginX;
                this.lastMousePosY = this.marginY;
            }

        }
    }

    public void mouseReleased(int mX, int mY, int state) {
        super.mouseReleased(mX, mY, state);
        if (state == 0) {
            this.mouseDown = false;
        }

    }

    public void actionPerformed(GuiButton b) {
        if (b == this.resetPosButton) {
            this.marginX = ArrayListModule.hudX = 5;
            this.marginY = ArrayListModule.hudY = 70;
        }

    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        if (dev.veyra.client.main.Veyra.clientConfig != null) {
            dev.veyra.client.main.Veyra.clientConfig.saveConfig();
        }
    }
}
