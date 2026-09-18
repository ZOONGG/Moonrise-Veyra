package dev.veyra.client.clickgui;

import dev.veyra.client.clickgui.font.VeyraFonts;
import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.modules.client.ArrayListModule;
import dev.veyra.client.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Dedicated blacklist/whitelist editor for the enabled-module overlay. */
public final class ArrayListVisibilityScreen extends GuiScreen {
    private static final int ROW_HEIGHT = 25;
    private final List<EntryHitbox> hitboxes = new ArrayList<>();
    private final List<Module> modules = new ArrayList<>();
    private int scroll;
    private int maxScroll;
    private Rect clearButton = Rect.EMPTY;
    private Rect selectButton = Rect.EMPTY;
    private Rect doneButton = Rect.EMPTY;
    private Rect listViewport = Rect.EMPTY;

    @Override
    public void initGui() {
        modules.clear();
        if (Veyra.moduleManager != null) {
            for (Module module : Veyra.moduleManager.getModules()) {
                if (!(module instanceof ArrayListModule)) modules.add(module);
            }
        }
        modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        buttonList.clear();
        recalculateScroll();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawGradientRect(0, 0, width, height, Theme.BACKDROP_TOP, Theme.BACKDROP_BOTTOM);
        int panelWidth = Math.min(520, Math.max(350, width - 16));
        int panelHeight = Math.min(360, Math.max(270, height - 16));
        int panelLeft = (width - panelWidth) / 2;
        int panelTop = (height - panelHeight) / 2;
        int panelRight = panelLeft + panelWidth;
        int panelBottom = panelTop + panelHeight;
        RenderUtils.drawRoundedRect(panelLeft, panelTop, panelRight, panelBottom, 9, Theme.WINDOW);
        RenderUtils.drawRoundedOutline(panelLeft, panelTop, panelRight, panelBottom,
                10, 0.8F, Theme.BORDER_INT);

        VeyraFonts.semibold().drawPlain("ArrayList modules", panelLeft + 16, panelTop + 13,
                Theme.TEXT_PRIMARY_INT, 11.5F);
        boolean blacklist = ArrayListModule.filterMode.getMode() == ArrayListModule.FilterMode.Blacklist;
        String explanation = blacklist
                ? "Blacklist: selected modules are hidden from the ArrayList"
                : "Whitelist: only selected modules are shown in the ArrayList";
        VeyraFonts.regular().drawPlain(explanation, panelLeft + 16, panelTop + 30,
                Theme.TEXT_MUTED_INT, 8.0F);

        hitboxes.clear();
        int columns = Math.max(1, Math.min(3, (panelRight - panelLeft - 28) / 170));
        int gap = 7;
        int gridLeft = panelLeft + 14;
        int gridWidth = panelRight - panelLeft - 28;
        int columnWidth = (gridWidth - gap * (columns - 1)) / columns;
        int listTop = panelTop + 52;
        int visibleBottom = panelBottom - 42;
        listViewport = new Rect(panelLeft + 8, listTop,
                panelRight - panelLeft - 16, Math.max(0, visibleBottom - listTop));
        recalculateScroll();

        enableScissor(listViewport.x, listViewport.y, listViewport.width, listViewport.height);
        for (int index = 0; index < modules.size(); index++) {
            int column = index % columns;
            int row = index / columns;
            int x = gridLeft + column * (columnWidth + gap);
            int y = listTop + row * ROW_HEIGHT - scroll;
            Rect rect = new Rect(x, y, columnWidth, 20);
            if (rect.bottom() < listTop || rect.y > visibleBottom) continue;

            Module module = modules.get(index);
            boolean selected = ArrayListModule.isFilterMember(module);
            boolean hovered = rect.contains(mouseX, mouseY) && listViewport.contains(mouseX, mouseY);
            int accent = Theme.moduleAccent(module.getName(), module.moduleCategory().ordinal());
            int fill = selected ? UiDraw.mix(Theme.SURFACE_INT, accent, hovered ? 0.20F : 0.13F)
                    : hovered ? Theme.SURFACE_HOVER_INT : Theme.SURFACE_INT;
            RenderUtils.drawRoundedRect(rect.x, rect.y, rect.right(), rect.bottom(), 5, fill);
            RenderUtils.drawRoundedOutline(rect.x, rect.y, rect.right(), rect.bottom(), 5,
                    0.7F, selected ? withAlpha(accent, 180) : Theme.BORDER_SOFT);
            String label = ellipsize(module.getName(), columnWidth - 20);
            VeyraFonts.semibold().drawPlain(label, rect.x + 10, rect.y + 6,
                    selected ? Theme.TEXT_PRIMARY_INT : Theme.TEXT_SECONDARY_INT, 8.0F);
            hitboxes.add(new EntryHitbox(module, rect));
        }
        disableScissor();
        int buttonY = panelBottom - 31;
        clearButton = new Rect(panelLeft + 14, buttonY, 82, 21);
        selectButton = new Rect(panelLeft + 102, buttonY, 82, 21);
        doneButton = new Rect(panelRight - 96, buttonY, 82, 21);
        drawFooterButton(clearButton, "Clear", mouseX, mouseY, false);
        drawFooterButton(selectButton, "Select all", mouseX, mouseY, false);
        drawFooterButton(doneButton, "Done", mouseX, mouseY, true);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        if (clearButton.contains(mouseX, mouseY)) {
            ArrayListModule.clearFilterMembers();
            save();
            return;
        }
        if (selectButton.contains(mouseX, mouseY)) {
            ArrayListModule.selectAllFilterMembers();
            save();
            return;
        }
        if (doneButton.contains(mouseX, mouseY)) {
            save();
            mc.displayGuiScreen(Veyra.clickGui);
            return;
        }
        if (!listViewport.contains(mouseX, mouseY)) return;
        for (EntryHitbox hitbox : hitboxes) {
            if (hitbox.rect.contains(mouseX, mouseY)) {
                ArrayListModule.toggleFilterMember(hitbox.module);
                save();
                return;
            }
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll = clamp(scroll + (wheel < 0 ? 31 : -31), 0, maxScroll);
        }
    }

    @Override
    public void onGuiClosed() {
        save();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void recalculateScroll() {
        int windowWidth = Math.min(520, Math.max(350, width - 16));
        int panelWidth = windowWidth - 28;
        int columns = Math.max(1, Math.min(3, panelWidth / 170));
        int rows = (modules.size() + columns - 1) / columns;
        int contentHeight = rows * ROW_HEIGHT;
        int viewportHeight = listViewport.height > 0 ? listViewport.height
                : Math.min(360, Math.max(270, height - 16)) - 94;
        maxScroll = Math.max(0, contentHeight - Math.max(1, viewportHeight));
        scroll = clamp(scroll, 0, maxScroll);
    }

    private void save() {
        if (Veyra.clientConfig != null) Veyra.clientConfig.saveConfig();
        if (Veyra.configManager != null) Veyra.configManager.save();
    }

    private String ellipsize(String text, int maximumWidth) {
        if (VeyraFonts.semibold().width(text, 8.0F) <= maximumWidth) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && VeyraFonts.semibold().width(text.substring(0, end) + suffix, 8.0F) > maximumWidth) {
            end--;
        }
        return text.substring(0, end) + suffix;
    }

    private void enableScissor(int x, int y, int scissorWidth, int scissorHeight) {
        int factor = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(x * factor, mc.displayHeight - (y + scissorHeight) * factor,
                scissorWidth * factor, scissorHeight * factor);
    }

    private void disableScissor() {
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }

    private static int withAlpha(int color, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | color & 0x00FFFFFF;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawFooterButton(Rect rect, String label, int mouseX, int mouseY,
                                  boolean primary) {
        boolean hovered = rect.contains(mouseX, mouseY);
        int accent = Theme.moduleAccent(label, 4);
        UiDraw.button(rect.x, rect.y, rect.right(), rect.bottom(), hovered, primary, accent);
        float textWidth = VeyraFonts.semibold().width(label, 8.0F);
        VeyraFonts.semibold().drawPlain(label, rect.x + (rect.width - textWidth) / 2.0F,
                rect.y + 7, primary ? Theme.TEXT_PRIMARY_INT : Theme.TEXT_SECONDARY_INT, 8.0F);
    }

    private record EntryHitbox(Module module, Rect rect) {
    }

    private record Rect(int x, int y, int width, int height) {
        private static final Rect EMPTY = new Rect(0, 0, 0, 0);
        private int right() { return x + width; }
        private int bottom() { return y + height; }
        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= right() && mouseY >= y && mouseY <= bottom();
        }
    }
}
