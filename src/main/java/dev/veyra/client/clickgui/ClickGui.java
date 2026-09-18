package dev.veyra.client.clickgui;

import dev.veyra.client.clickgui.components.CategoryComponent;
import dev.veyra.client.clickgui.font.VeyraFont;
import dev.veyra.client.clickgui.font.VeyraFonts;
import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.bind.ModuleTransitionSource;
import dev.veyra.client.module.modules.client.ClickGuiModule;
import dev.veyra.client.module.setting.Setting;
import dev.veyra.client.module.setting.impl.ColorSetting;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.DoubleSliderSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Compact two-column control panel inspired by Vape's information density. */
public final class ClickGui extends GuiScreen {
    private static boolean favoritesCategoryEnabled = true;
    private static final int IDEAL_WIDTH = 520;
    private static final int IDEAL_HEIGHT = 360;
    private static final int HEADER_HEIGHT = 42;
    private static final int TABS_HEIGHT = 32;
    private static final int MODULE_HEADER_HEIGHT = 42;
    private static final int CONTENT_PADDING = 8;
    private static final int COLUMN_GAP = 6;
    private static final int FAVORITES_GUIDE_HEIGHT = 21;
    private static final Object NOTIFICATIONS_TOGGLE = new Object();

    /* Kept for backwards-compatible client.scfg coordinates. */
    private final ArrayList<CategoryComponent> categoryList = new ArrayList<>();
    private final Map<Module, Float> hover = new IdentityHashMap<>();
    private final Map<Object, Float> toggleAnimation = new IdentityHashMap<>();
    private final Map<Module, Float> favoriteVisualY = new IdentityHashMap<>();
    private final List<CategoryHitbox> categoryHitboxes = new ArrayList<>();
    private final List<ModuleHitbox> moduleHitboxes = new ArrayList<>();
    private final List<ActionHitbox> actionHitboxes = new ArrayList<>();
    private final List<SettingHitbox> settingHitboxes = new ArrayList<>();
    private final UiClipStack clipStack = new UiClipStack();

    private Module.ModuleCategory selectedCategory = Module.ModuleCategory.Combat;
    private boolean favoritesSelected;
    private Module bindingModule;
    private Setting activeSlider;
    private Rect activeSliderRow = Rect.EMPTY;
    private boolean activeRangeMaximum;
    private ColorSetting activeColor;
    private Rect activeColorRow = Rect.EMPTY;
    private ColorDragMode activeColorMode;

    private String searchQuery = "";
    private int searchCursor;
    private int searchAnchor;
    private boolean searchFocused;
    private int scroll;
    private int maxScroll;

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private boolean layoutInitialized;
    private boolean draggingWindow;
    private int dragOffsetX;
    private int dragOffsetY;

    private Rect windowRect = Rect.EMPTY;
    private Rect headerRect = Rect.EMPTY;
    private Rect searchRect = Rect.EMPTY;
    private Rect settingsButtonRect = Rect.EMPTY;
    private Rect settingsPanelRect = Rect.EMPTY;
    private Rect notificationsToggleRect = Rect.EMPTY;
    private Rect themeSelectorRect = Rect.EMPTY;
    private Rect shortcutButtonRect = Rect.EMPTY;
    private Rect contentClip = Rect.EMPTY;
    private Rect settingsViewport = Rect.EMPTY;
    private Rect settingsBackRect = Rect.EMPTY;
    private Rect settingsModuleToggleRect = Rect.EMPTY;
    private float openAnimation;
    private float categoryAnimation = 1.0F;
    private float frameDelta = 0.016F;
    private long lastFrameNanos;
    private Module favoriteDragModule;
    private int favoriteDragStartX;
    private int favoriteDragStartY;
    private int favoriteDragOffsetY;
    private int favoriteDropIndex = -1;
    private boolean favoriteDragging;
    private boolean settingsOpen;
    private Module settingsModule;

    public ClickGui() {
        for (Module.ModuleCategory category : Module.ModuleCategory.values()) {
            CategoryComponent component = new CategoryComponent(category);
            component.setOpened(true);
            categoryList.add(component);
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        clipStack.clear();
        closeModuleSettings();
        updateWindowMetrics();
        if (!layoutInitialized) {
            windowX = (width - windowWidth) / 2;
            windowY = (height - windowHeight) / 2;
            layoutInitialized = true;
        }
        clampWindow();
        openAnimation = 0.0F;
        lastFrameNanos = System.nanoTime();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateWindowMetrics();
        updateDraggedWindow(mouseX, mouseY);
        updateDraggedSetting(mouseX, mouseY);
        updateAnimations(mouseX, mouseY);

        windowRect = new Rect(windowX, windowY, windowWidth, windowHeight);
        headerRect = new Rect(windowX, windowY, windowWidth, HEADER_HEIGHT);

        drawGradientRect(0, 0, width, height,
                UiTokens.Color.BACKDROP_TOP, UiTokens.Color.BACKDROP_BOTTOM);
        drawBackdropAccents();
        int scrim = Math.round(118.0F * easeOutCubic(openAnimation));
        drawRect(0, 0, width, height,
                withAlpha(UiTokens.Color.BACKDROP_SCRIM, scrim));

        categoryHitboxes.clear();
        moduleHitboxes.clear();
        actionHitboxes.clear();
        settingHitboxes.clear();

        drawWindow();
        drawHeader(mouseX, mouseY);
        drawTabs(mouseX, mouseY);
        drawContent(mouseX, mouseY);
        drawUiSettings(mouseX, mouseY);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void updateWindowMetrics() {
        windowWidth = Math.min(IDEAL_WIDTH, Math.max(350, width - 16));
        windowHeight = Math.min(IDEAL_HEIGHT, Math.max(270, height - 16));
    }

    private void updateAnimations(int mouseX, int mouseY) {
        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L ? 0.016F
                : Math.min(0.05F, (now - lastFrameNanos) / 1_000_000_000.0F);
        frameDelta = delta;
        lastFrameNanos = now;
        openAnimation = FrameMotion.expApproach(openAnimation, 1.0F,
                UiTokens.Motion.OPEN_SPEED, delta);
        categoryAnimation = FrameMotion.expApproach(categoryAnimation, 1.0F,
                UiTokens.Motion.CATEGORY_SPEED, delta);

        for (Module module : Veyra.moduleManager.getModules()) {
            boolean hovered = moduleHitboxes.stream()
                    .anyMatch(hitbox -> hitbox.module == module && hitbox.isVisibleAt(mouseX, mouseY));
            hover.put(module, FrameMotion.expApproach(
                    hover.getOrDefault(module, 0.0F), hovered ? 1.0F : 0.0F,
                    UiTokens.Motion.HOVER_SPEED, delta));
        }

        if (favoriteDragModule != null && Mouse.isButtonDown(0)) {
            if (!favoriteDragging && Math.abs(mouseX - favoriteDragStartX)
                    + Math.abs(mouseY - favoriteDragStartY) >= 4) {
                favoriteDragging = true;
            }
            if (favoriteDragging && contentClip.height > 0 && contentClip.contains(mouseX, mouseY)) {
                if (mouseY < contentClip.y + 16) scroll = Math.max(0, scroll - 2);
                else if (mouseY > contentClip.bottom() - 16) scroll = Math.min(maxScroll, scroll + 2);
            }
        }
    }

    private void drawBackdropAccents() {
        RenderUtils.drawRoundedRect(windowX - 70, windowY - 35,
                windowX + 145, windowY + 105, 70, withAlpha(accent(), 12));
        RenderUtils.drawRoundedRect(windowRect.right() - 88, windowRect.bottom() - 78,
                windowRect.right() + 58, windowRect.bottom() + 36, 54,
                withAlpha(Theme.ACCENT_BLUE, 9));
    }

    private void drawWindow() {
        UiDraw.surface(windowRect.x, windowRect.y, windowRect.right(), windowRect.bottom(),
                UiTokens.Radius.WINDOW, UiTokens.Color.WINDOW, UiTokens.Color.BORDER, 2);
    }

    private void drawHeader(int mouseX, int mouseY) {
        RenderUtils.drawRoundedRect(headerRect.x, headerRect.y,
                headerRect.right(), headerRect.bottom(), UiTokens.Radius.WINDOW,
                UiTokens.Color.TOPBAR);
        drawRect(headerRect.x, headerRect.bottom() - 1,
                headerRect.right(), headerRect.bottom(), UiTokens.Color.DIVIDER);

        VeyraFonts.semibold().drawPlainTracked("VEYRA", windowX + 16, windowY + 9,
                UiTokens.Color.TEXT_PRIMARY, UiTokens.Type.DISPLAY, 0.35F);
        VeyraFonts.regular().drawPlainTracked("CONTROL CENTER", windowX + 16, windowY + 25,
                UiTokens.Color.TEXT_MUTED, UiTokens.Type.CAPTION, 0.18F);

        int searchWidth = Math.min(158, Math.max(108, windowWidth / 3));
        searchRect = new Rect(windowRect.right() - searchWidth - 10,
                windowY + 8, searchWidth, 26);
        settingsButtonRect = new Rect(searchRect.x - 33, windowY + 8, 26, 26);
        boolean settingsHovered = settingsButtonRect.contains(mouseX, mouseY);
        UiDraw.button(settingsButtonRect.x, settingsButtonRect.y,
                settingsButtonRect.right(), settingsButtonRect.bottom(),
                settingsHovered, settingsOpen, accent());
        VeyraFonts.semibold().drawPlain("UI", settingsButtonRect.x + 7,
                settingsButtonRect.y + 9,
                settingsOpen ? accent() : UiTokens.Color.TEXT_SECONDARY,
                UiTokens.Type.LABEL);
        boolean hovered = searchRect.contains(mouseX, mouseY);
        UiDraw.control(searchRect.x, searchRect.y, searchRect.right(), searchRect.bottom(),
                hovered, searchFocused, accent());
        String value = searchQuery.isEmpty() ? "Search modules" : searchQuery;
        int[] visible = visibleSearchRange(searchRect.width - 20);
        String shown = searchQuery.isEmpty() ? value
                : searchQuery.substring(visible[0], visible[1]);
        float textX = searchRect.x + 10;
        if (searchFocused && searchAnchor != searchCursor && !searchQuery.isEmpty()) {
            int selectionStart = Math.max(Math.min(searchAnchor, searchCursor), visible[0]);
            int selectionEnd = Math.min(Math.max(searchAnchor, searchCursor), visible[1]);
            if (selectionEnd > selectionStart) {
                float left = textX + VeyraFonts.semibold().width(
                        searchQuery.substring(visible[0], selectionStart), UiTokens.Type.BODY);
                float right = left + VeyraFonts.semibold().width(
                        searchQuery.substring(selectionStart, selectionEnd), UiTokens.Type.BODY);
                RenderUtils.drawRoundedRect(left - 1, searchRect.y + 4,
                        right + 1, searchRect.bottom() - 4, 2, withAlpha(accent(), 80));
            }
        }
        VeyraFonts.semibold().drawPlain(shown, textX, searchRect.y + 9,
                searchQuery.isEmpty() ? UiTokens.Color.TEXT_MUTED
                        : UiTokens.Color.TEXT_PRIMARY, UiTokens.Type.BODY);
        if (searchFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            float caretX = textX + VeyraFonts.semibold().width(
                    searchQuery.substring(visible[0], Math.min(searchCursor, visible[1])),
                    UiTokens.Type.BODY);
            drawRect((int) caretX + 1, searchRect.y + 5,
                    (int) caretX + 2, searchRect.bottom() - 5, accent());
        }
    }

    private void drawUiSettings(int mouseX, int mouseY) {
        settingsPanelRect = Rect.EMPTY;
        notificationsToggleRect = Rect.EMPTY;
        themeSelectorRect = Rect.EMPTY;
        shortcutButtonRect = Rect.EMPTY;
        if (!settingsOpen) return;
        Rect panel = new Rect(windowRect.right() - 236,
                windowY + HEADER_HEIGHT + 5, 226, 143);
        settingsPanelRect = panel;
        UiDraw.surface(panel.x, panel.y, panel.right(), panel.bottom(),
                UiTokens.Radius.MD, UiTokens.Color.PANEL, UiTokens.Color.BORDER, 2);
        VeyraFonts.semibold().drawPlain("Interface settings", panel.x + 12, panel.y + 9,
                UiTokens.Color.TEXT_PRIMARY, UiTokens.Type.TITLE);
        VeyraFonts.regular().drawPlain("Appearance, notifications and menu access",
                panel.x + 12, panel.y + 22, UiTokens.Color.TEXT_MUTED,
                UiTokens.Type.CAPTION);
        drawRect(panel.x + 10, panel.y + 35, panel.right() - 10,
                panel.y + 36, UiTokens.Color.DIVIDER);

        themeSelectorRect = new Rect(panel.x + 8, panel.y + 41, panel.width - 16, 28);
        drawPopupRow(themeSelectorRect, mouseX, mouseY, "Theme",
                ClickGuiModule.clientTheme == null ? "Veyra"
                        : String.valueOf(ClickGuiModule.clientTheme.getMode()), true);

        notificationsToggleRect = new Rect(panel.x + 8, panel.y + 72, panel.width - 16, 28);
        boolean notifications = ClickGuiModule.notificationsEnabled();
        drawPopupRow(notificationsToggleRect, mouseX, mouseY,
                "Bind notifications", notifications ? "Shown" : "Hidden", false);
        Rect notificationsToggle = new Rect(notificationsToggleRect.right() - 26,
                notificationsToggleRect.y + 8, 22, 11);
        drawToggle(notificationsToggle, notifications, accent(),
                NOTIFICATIONS_TOGGLE, notificationsToggleRect.contains(mouseX, mouseY));

        shortcutButtonRect = new Rect(panel.x + 8, panel.y + 103, panel.width - 16, 28);
        Module clickGuiModule = clickGuiModule();
        String shortcut = clickGuiModule == null ? "Enter"
                : bindingModule == clickGuiModule ? "Press a key"
                : keyName(clickGuiModule.getKeycode());
        drawPopupRow(shortcutButtonRect, mouseX, mouseY,
                "Menu shortcut", shortcut, true);
    }

    private void drawPopupRow(Rect row, int mouseX, int mouseY,
                              String label, String value, boolean chevron) {
        boolean hovered = row.contains(mouseX, mouseY);
        UiDraw.surface(row.x, row.y, row.right(), row.bottom(), UiTokens.Radius.SM,
                hovered ? UiTokens.Color.SURFACE_HOVER : UiTokens.Color.SURFACE,
                UiTokens.Color.BORDER_SOFT, 0);
        VeyraFonts.semibold().drawPlain(label, row.x + 10, row.y + 10,
                UiTokens.Color.TEXT_SECONDARY, UiTokens.Type.BODY);
        float valueWidth = VeyraFonts.semibold().width(value, UiTokens.Type.LABEL);
        float rightInset = chevron ? 15 : 31;
        VeyraFonts.semibold().drawPlain(value, row.right() - rightInset - valueWidth,
                row.y + 10, hovered ? accent() : UiTokens.Color.TEXT_MUTED,
                UiTokens.Type.LABEL);
        if (chevron) {
            VeyraFonts.semibold().drawPlain(">", row.right() - 9, row.y + 10,
                    hovered ? accent() : UiTokens.Color.TEXT_MUTED, UiTokens.Type.LABEL);
        }
    }

    private void drawTabs(int mouseX, int mouseY) {
        int y = windowY + HEADER_HEIGHT;
        drawRect(windowX, y, windowRect.right(), y + TABS_HEIGHT, UiTokens.Color.NAV);
        drawRect(windowX, y + TABS_HEIGHT - 1,
                windowRect.right(), y + TABS_HEIGHT, UiTokens.Color.DIVIDER);

        boolean favoritesEnabled = favoritesEnabled();
        if (!favoritesEnabled && favoritesSelected) {
            favoritesSelected = false;
            selectedCategory = Module.ModuleCategory.Combat;
        }
        int count = Module.ModuleCategory.values().length + (favoritesEnabled ? 1 : 0);
        int available = windowWidth - 14;
        int tabWidth = available / count;
        int x = windowX + 7;
        if (favoritesEnabled) {
            Rect rect = new Rect(x, y + 4, tabWidth, TABS_HEIGHT - 8);
            drawTab(new CategoryHitbox(null, true, rect), mouseX, mouseY,
                    favoritesSelected && searchQuery.isEmpty(), "Favorites");
            x += tabWidth;
        }
        for (Module.ModuleCategory category : Module.ModuleCategory.values()) {
            Rect rect = new Rect(x, y + 4, tabWidth, TABS_HEIGHT - 8);
            drawTab(new CategoryHitbox(category, false, rect), mouseX, mouseY,
                    !favoritesSelected && searchQuery.isEmpty() && selectedCategory == category,
                    categoryLabel(category));
            x += tabWidth;
        }
    }

    private void drawTab(CategoryHitbox hitbox, int mouseX, int mouseY,
                         boolean selected, String label) {
        categoryHitboxes.add(hitbox);
        boolean hovered = hitbox.rect.contains(mouseX, mouseY);
        if (selected || hovered) {
            RenderUtils.drawRoundedRect(hitbox.rect.x + 2, hitbox.rect.y,
                    hitbox.rect.right() - 2, hitbox.rect.bottom(), UiTokens.Radius.SM,
                    selected ? UiTokens.Color.SURFACE_RAISED : UiTokens.Color.SURFACE);
        }
        int color = selected ? UiTokens.Color.TEXT_PRIMARY
                : hovered ? UiTokens.Color.TEXT_SECONDARY : UiTokens.Color.TEXT_MUTED;
        float textWidth = VeyraFonts.semibold().width(label, UiTokens.Type.BODY);
        float groupX = hitbox.rect.x + (hitbox.rect.width - textWidth) / 2.0F;
        VeyraFonts.semibold().drawPlain(label,
                groupX, hitbox.rect.y + 8, color, UiTokens.Type.BODY);
    }

    private void drawContent(int mouseX, int mouseY) {
        int top = windowY + HEADER_HEIGHT + TABS_HEIGHT;
        contentClip = new Rect(windowX + 1, top,
                windowWidth - 2, windowRect.bottom() - top - 1);
        if (settingsModule != null) {
            drawModuleSettingsPage(mouseX, mouseY);
            return;
        }
        settingsViewport = Rect.EMPTY;
        settingsBackRect = Rect.EMPTY;
        settingsModuleToggleRect = Rect.EMPTY;
        List<Module> modules = visibleModules();
        int contentHeight = contentHeight(modules);
        maxScroll = Math.max(0, contentHeight - contentClip.height);
        scroll = clamp(scroll, 0, maxScroll);

        beginScissor(contentClip);
        if (modules.isEmpty()) {
            drawEmptyState();
        } else if (favoritesSelected && searchQuery.isEmpty()) {
            drawFavoritesContent(modules, mouseX, mouseY);
        } else {
            int columnWidth = (contentClip.width - CONTENT_PADDING * 2 - COLUMN_GAP) / 2;
            int[] columnY = {
                    contentClip.y + CONTENT_PADDING - scroll,
                    contentClip.y + CONTENT_PADDING - scroll
            };
            float slide = (1.0F - easeOutCubic(categoryAnimation)) * 9.0F;
            for (int index = 0; index < modules.size(); index++) {
                Module module = modules.get(index);
                // Stable column ownership prevents cards jumping sideways while
                // a neighbour is expanding or collapsing.
                int column = index & 1;
                int x = contentClip.x + CONTENT_PADDING
                        + column * (columnWidth + COLUMN_GAP) + Math.round(slide);
                int height = animatedModuleHeight(module);
                drawModule(module, new Rect(x, columnY[column], columnWidth, height), mouseX, mouseY);
                columnY[column] += height + 6;
            }
        }
        endScissor();
        if (maxScroll > 0) drawScrollbar();
    }

    private void drawModuleSettingsPage(int mouseX, int mouseY) {
        int panelInset = 8;
        int pageHeaderHeight = 48;
        Rect panel = new Rect(contentClip.x + panelInset, contentClip.y + panelInset,
                contentClip.width - panelInset * 2, contentClip.height - panelInset * 2);
        UiDraw.surface(panel.x, panel.y, panel.right(), panel.bottom(), UiTokens.Radius.MD,
                UiTokens.Color.PANEL, UiTokens.Color.BORDER_SOFT, 0);

        settingsBackRect = new Rect(panel.x + 8, panel.y + 8, 48, 24);
        settingsModuleToggleRect = new Rect(panel.right() - 40, panel.y + 14, 24, 12);
        settingsViewport = new Rect(panel.x + 8, panel.y + pageHeaderHeight,
                panel.width - 16, Math.max(1, panel.height - pageHeaderHeight - 8));

        int pageHeight = Math.max(settingsViewport.height, expandedHeight(settingsModule));
        maxScroll = Math.max(0, pageHeight - settingsViewport.height);
        scroll = clamp(scroll, 0, maxScroll);

        beginScissor(settingsViewport);
        Rect body = new Rect(settingsViewport.x, settingsViewport.y - scroll,
                settingsViewport.width, pageHeight);
        drawExpandedModule(settingsModule, body, mouseX, mouseY,
                accent(), true, settingsViewport);
        endScissor();

        drawRect(panel.x + 8, panel.y + pageHeaderHeight - 1,
                panel.right() - 8, panel.y + pageHeaderHeight, UiTokens.Color.DIVIDER);
        drawButton(settingsBackRect, "Back", settingsBackRect.contains(mouseX, mouseY),
                false, accent());
        VeyraFonts.semibold().drawPlain(
                ellipsize(settingsModule.getName(), panel.width - 140,
                        VeyraFonts.semibold(), UiTokens.Type.TITLE),
                settingsBackRect.right() + 10, panel.y + 9,
                UiTokens.Color.TEXT_PRIMARY, UiTokens.Type.TITLE);
        VeyraFonts.regular().drawPlain("Module settings",
                settingsBackRect.right() + 10, panel.y + 25,
                UiTokens.Color.TEXT_MUTED, UiTokens.Type.CAPTION);
        drawToggle(settingsModuleToggleRect, settingsModule.isEnabled(), accent(),
                settingsModule, settingsModuleToggleRect.contains(mouseX, mouseY));

        if (maxScroll > 0) drawScrollbar();
    }

    private void drawFavoritesContent(List<Module> modules, int mouseX, int mouseY) {
        int x = contentClip.x + CONTENT_PADDING;
        int width = contentClip.width - CONTENT_PADDING * 2;
        int guideY = contentClip.y + CONTENT_PADDING - scroll;
        drawFavoritesGuide(new Rect(x, guideY, width, FAVORITES_GUIDE_HEIGHT));

        List<Module> display = new ArrayList<>(modules);
        if (favoriteDragging && favoriteDragModule != null && display.remove(favoriteDragModule)) {
            favoriteDropIndex = favoriteInsertionIndex(display, mouseY);
            display.add(clamp(favoriteDropIndex, 0, display.size()), favoriteDragModule);
        } else {
            favoriteDropIndex = -1;
        }

        int targetY = guideY + FAVORITES_GUIDE_HEIGHT + 4;
        for (Module module : display) {
            int height = animatedModuleHeight(module);
            Rect target = new Rect(x, targetY, width, height);
            if (favoriteDragging && module == favoriteDragModule) {
                favoriteVisualY.put(module, (float) targetY);
                drawFavoriteInsertionSlot(target);
            } else {
                int visualY = Math.round(animateFavoriteY(module, targetY));
                drawModule(module, new Rect(x, visualY, width, height), mouseX, mouseY);
            }
            targetY += height + 5;
        }

        if (favoriteDragging && favoriteDragModule != null) {
            int ghostY = clamp(mouseY - favoriteDragOffsetY,
                    contentClip.y + 2, contentClip.bottom() - MODULE_HEADER_HEIGHT - 2);
            drawModule(favoriteDragModule,
                    new Rect(x + 2, ghostY, width - 4, MODULE_HEADER_HEIGHT),
                    mouseX, mouseY, false, true);
        }
    }

    private void drawFavoritesGuide(Rect rect) {
        UiDraw.surface(rect.x, rect.y, rect.right(), rect.bottom(), UiTokens.Radius.SM,
                UiTokens.Color.INPUT, UiTokens.Color.BORDER_SOFT, 0);
        VeyraFonts.semibold().drawPlain("Drag a grip to reorder favorites",
                rect.x + 10, rect.y + 7, UiTokens.Color.TEXT_SECONDARY, UiTokens.Type.LABEL);
        String state = favoriteDragging ? "Release to place" : "Order is saved automatically";
        VeyraFonts.regular().drawPlain(state,
                rect.right() - 7 - VeyraFonts.regular().width(state, UiTokens.Type.CAPTION),
                rect.y + 7, favoriteDragging ? accent() : UiTokens.Color.TEXT_MUTED,
                UiTokens.Type.CAPTION);
    }

    private int favoriteInsertionIndex(List<Module> modules, int mouseY) {
        int y = contentClip.y + CONTENT_PADDING - scroll + FAVORITES_GUIDE_HEIGHT + 4;
        for (int index = 0; index < modules.size(); index++) {
            int height = animatedModuleHeight(modules.get(index));
            if (mouseY < y + height / 2) return index;
            y += height + 5;
        }
        return modules.size();
    }

    private float animateFavoriteY(Module module, int targetY) {
        float current = favoriteVisualY.getOrDefault(module, (float) targetY);
        current = FrameMotion.expApproach(current, targetY,
                UiTokens.Motion.REORDER_SPEED, frameDelta);
        favoriteVisualY.put(module, current);
        return current;
    }

    private void drawFavoriteInsertionSlot(Rect rect) {
        int border = withAlpha(accent(), 185);
        UiDraw.surface(rect.x, rect.y, rect.right(), rect.bottom(), UiTokens.Radius.SM,
                withAlpha(accent(), 24), border, 0);
        RenderUtils.drawRoundedRect(rect.x + 6, rect.y, rect.right() - 6,
                rect.y + 2, 1, accent());
        VeyraFonts.semibold().drawPlain("Release to place",
                rect.x + 10, rect.y + 10, withAlpha(accent(), 230), UiTokens.Type.LABEL);
    }

    private void drawEmptyState() {
        String title = favoritesSelected ? "No favorite modules" : "Nothing found";
        String hint = favoritesSelected ? "Use the star on a module" : "Try a different search";
        int centerX = contentClip.x + contentClip.width / 2;
        int centerY = contentClip.y + contentClip.height / 2;
        RenderUtils.drawRoundedRect(centerX - 112, centerY - 38,
                centerX + 112, centerY + 39, UiTokens.Radius.MD,
                UiTokens.Color.PANEL);
        RenderUtils.drawRoundedOutline(centerX - 112, centerY - 38,
                centerX + 112, centerY + 39, UiTokens.Radius.MD, 0.8F,
                UiTokens.Color.BORDER_SOFT);
        VeyraFonts.semibold().drawPlain(title,
                centerX - VeyraFonts.semibold().width(title, 9.0F) / 2.0F,
                centerY - 11, UiTokens.Color.TEXT_PRIMARY, 9.0F);
        VeyraFonts.regular().drawPlain(hint,
                centerX - VeyraFonts.regular().width(hint, UiTokens.Type.LABEL) / 2.0F,
                centerY + 7, UiTokens.Color.TEXT_SECONDARY, UiTokens.Type.LABEL);
    }

    private void drawModule(Module module, Rect card, int mouseX, int mouseY) {
        drawModule(module, card, mouseX, mouseY, true, false);
    }

    private void drawModule(Module module, Rect card, int mouseX, int mouseY,
                            boolean interactive, boolean dragGhost) {
        if (!verticallyVisible(card, contentClip)) return;
        Rect header = new Rect(card.x, card.y, card.width, MODULE_HEADER_HEIGHT);
        Rect toggle = new Rect(header.right() - 44, header.y + 15, 24, 12);
        Rect expandButton = new Rect(header.right() - 17, header.y, 17, header.height);
        Rect star = new Rect(header.right() - 67, header.y + 8, 20, 26);
        Rect drag = favoritesSelected
                ? new Rect(header.x + 3, header.y + 4, 14, 24) : Rect.EMPTY;
        if (interactive) {
            moduleHitboxes.add(new ModuleHitbox(module, header, expandButton, star, drag, contentClip));
        }

        float hovered = hover.getOrDefault(module, 0.0F);
        int moduleColor = accent();
        int cardFill = mix(UiTokens.Color.SURFACE, UiTokens.Color.SURFACE_HOVER, hovered);
        if (module.isEnabled()) cardFill = mix(cardFill, moduleColor, 0.12F);
        int border = dragGhost ? withAlpha(moduleColor, 210)
                : module.isEnabled() ? withAlpha(moduleColor, 108) : UiTokens.Color.BORDER_SOFT;
        UiDraw.surface(card.x, card.y, card.right(), card.bottom(), UiTokens.Radius.SM,
                cardFill, border, dragGhost ? 1 : 0);

        int nameX = header.x + (favoritesSelected ? 20 : 10);
        String name = ellipsize(module.getName(), header.right() - 70 - nameX,
                VeyraFonts.semibold(), UiTokens.Type.TITLE);
        VeyraFonts.semibold().drawPlain(
                name, nameX, header.y + 8,
                module.canBeEnabled() ? UiTokens.Color.TEXT_PRIMARY
                        : UiTokens.Color.TEXT_DISABLED, UiTokens.Type.TITLE);
        String description = ModuleDescriptions.forModule(module.getName());
        if (description != null && !description.isBlank()) {
            VeyraFonts.regular().drawPlain(
                    ellipsize(description, header.right() - 70 - nameX,
                            VeyraFonts.regular(), UiTokens.Type.CAPTION),
                    nameX, header.y + 25, UiTokens.Color.TEXT_MUTED,
                    UiTokens.Type.CAPTION);
        }

        if (favoritesSelected) {
            drawDragGrip(drag, module == favoriteDragModule && favoriteDragging
                    ? UiTokens.Color.WARNING : UiTokens.Color.TEXT_DISABLED);
        }
        int starColor = module.isFavorite() ? UiTokens.Color.WARNING
                : star.contains(mouseX, mouseY) ? UiTokens.Color.TEXT_PRIMARY
                : UiTokens.Color.TEXT_MUTED;
        VeyraFonts.semibold().drawPlain(module.isFavorite() ? "★" : "☆",
                star.x + 4, star.y + 7, starColor, 11.0F);
        drawToggle(toggle, module.isEnabled(), moduleColor, module,
                interactive && toggle.contains(mouseX, mouseY));
        VeyraFonts.semibold().drawPlain(">",
                expandButton.x + 4, expandButton.y + 15,
                expandButton.contains(mouseX, mouseY) ? moduleColor : UiTokens.Color.TEXT_MUTED,
                UiTokens.Type.BODY);
    }

    private void drawExpandedModule(Module module, Rect body, int mouseX, int mouseY,
                                    int moduleColor, boolean interactive, Rect viewport) {
        int x = body.x + 8;
        int innerWidth = body.width - 16;
        int y = body.y + 6;
        Rect reset = new Rect(x, y, 42, 17);
        Rect bindMode = new Rect(x + 47, y, 52, 17);
        Rect bind = new Rect(x + 104, y, Math.max(58, innerWidth - 104), 17);
        Rect actionRow = new Rect(x, y, innerWidth, 17);
        boolean actionsVisible = verticallyVisible(actionRow, viewport);
        if (interactive && actionsVisible) {
            actionHitboxes.add(new ActionHitbox(module, ActionType.RESET, reset, viewport));
            actionHitboxes.add(new ActionHitbox(module, ActionType.BIND_MODE, bindMode, viewport));
            actionHitboxes.add(new ActionHitbox(module, ActionType.BIND, bind, viewport));
        }
        if (actionsVisible) {
            drawButton(reset, "Reset", reset.contains(mouseX, mouseY), false, moduleColor);
            drawButton(bindMode, bindModeLabel(module), bindMode.contains(mouseX, mouseY),
                    module.getBindMode() != dev.veyra.client.module.bind.BindMode.TOGGLE, moduleColor);
            boolean binding = bindingModule == module;
            drawButton(bind, binding ? "Press a key" : keyName(module.getKeycode()),
                    bind.contains(mouseX, mouseY), binding, moduleColor);
        }
        y += 23;

        for (Setting setting : module.getSettings()) {
            if (setting instanceof DescriptionSetting) continue;
            int settingHeight = settingHeight(setting);
            Rect row = new Rect(x, y, innerWidth, settingHeight);
            if (verticallyVisible(row, viewport)) {
                if (interactive) {
                    settingHitboxes.add(new SettingHitbox(module, setting, row, viewport));
                }
                drawSetting(setting, row, mouseX, mouseY, moduleColor, viewport);
            }
            y += settingHeight;
        }
    }

    private void drawButton(Rect rect, String text, boolean hovered, boolean active, int color) {
        UiDraw.button(rect.x, rect.y, rect.right(), rect.bottom(), hovered, active, color);
        String shown = ellipsize(text, rect.width - 8,
                VeyraFonts.semibold(), UiTokens.Type.LABEL);
        VeyraFonts.semibold().drawPlain(shown,
                rect.x + (rect.width - VeyraFonts.semibold().width(
                        shown, UiTokens.Type.LABEL)) / 2.0F,
                rect.y + 6, active ? color : UiTokens.Color.TEXT_SECONDARY,
                UiTokens.Type.LABEL);
    }

    private void drawSetting(Setting setting, Rect row, int mouseX, int mouseY,
                             int color, Rect viewport) {
        boolean hovered = row.contains(mouseX, mouseY) && viewport.contains(mouseX, mouseY);
        if (hovered) {
            RenderUtils.drawRoundedRect(row.x - 3, row.y + 1,
                    row.right() + 3, row.bottom() - 1,
                    UiTokens.Radius.SM, UiTokens.Color.SURFACE_HOVER);
        }
        String label = cleanLabel(setting.getName());
        VeyraFonts.semibold().drawPlain(
                ellipsize(label, row.width - 75,
                        VeyraFonts.semibold(), UiTokens.Type.BODY),
                row.x, row.y + 6, UiTokens.Color.TEXT_SECONDARY, UiTokens.Type.BODY);

        if (setting instanceof TickSetting tick) {
            Rect toggle = new Rect(row.right() - 22, row.y + 5, 22, 11);
            drawToggle(toggle, tick.isToggled(), color, tick, toggle.contains(mouseX, mouseY));
        } else if (setting instanceof ComboSetting<?> combo) {
            String value = String.valueOf(combo.getMode());
            int valueWidth = Math.min(88, Math.max(38,
                    (int) VeyraFonts.semibold().width(value, UiTokens.Type.LABEL) + 19));
            Rect pill = new Rect(row.right() - valueWidth, row.y + 2, valueWidth, 17);
            UiDraw.control(pill.x, pill.y, pill.right(), pill.bottom(), hovered, false, color);
            VeyraFonts.semibold().drawPlain(
                    ellipsize(value, pill.width - 18,
                            VeyraFonts.semibold(), UiTokens.Type.LABEL),
                    pill.x + 6, pill.y + 6, UiTokens.Color.TEXT_PRIMARY, UiTokens.Type.LABEL);
            VeyraFonts.semibold().drawPlain(">", pill.right() - 9, pill.y + 6,
                    UiTokens.Color.TEXT_MUTED, UiTokens.Type.LABEL);
        } else if (setting instanceof SliderSetting slider) {
            String value = formatNumber(slider.getInput());
            VeyraFonts.semibold().drawPlain(value,
                    row.right() - VeyraFonts.semibold().width(value, UiTokens.Type.CAPTION),
                    row.y + 5, UiTokens.Color.TEXT_PRIMARY, UiTokens.Type.CAPTION);
            drawSliderTrack(new Rect(row.x, row.y + 21, row.width, 3),
                    normalized(slider.getInput(), slider.getMin(), slider.getMax()), -1, color);
        } else if (setting instanceof DoubleSliderSetting range) {
            String value = formatNumber(range.getInputMin()) + " – " + formatNumber(range.getInputMax());
            VeyraFonts.semibold().drawPlain(value,
                    row.right() - VeyraFonts.semibold().width(value, UiTokens.Type.CAPTION),
                    row.y + 5, UiTokens.Color.TEXT_PRIMARY, UiTokens.Type.CAPTION);
            drawSliderTrack(new Rect(row.x, row.y + 22, row.width, 3),
                    normalized(range.getInputMin(), range.getMin(), range.getMax()),
                    normalized(range.getInputMax(), range.getMin(), range.getMax()), color);
        } else if (setting instanceof ColorSetting colorSetting) {
            drawColorPicker(colorSetting, row);
        }
        drawRect(row.x, row.bottom() - 1, row.right(), row.bottom(), UiTokens.Color.DIVIDER);
    }

    private void drawToggle(Rect rect, boolean enabled, int color,
                            Object stateKey, boolean hovered) {
        float target = enabled ? 1.0F : 0.0F;
        Float previous = toggleAnimation.get(stateKey);
        float progress = previous == null ? target : previous;
        progress = FrameMotion.expApproach(progress, target,
                UiTokens.Motion.TOGGLE_SPEED, frameDelta);
        toggleAnimation.put(stateKey, progress);
        UiDraw.toggle(rect.x, rect.y, rect.right(), rect.bottom(), progress,
                hovered, hovered && Mouse.isButtonDown(0), color);
    }

    private void drawSliderTrack(Rect track, float first, float second, int color) {
        UiDraw.slider(track.x, track.y, track.right(), track.bottom(), first, second, color);
    }

    private void drawDragGrip(Rect rect, int color) {
        VeyraFonts.semibold().drawPlain("••", rect.x, rect.y + 6,
                color, UiTokens.Type.LABEL);
        VeyraFonts.semibold().drawPlain("••", rect.x, rect.y + 12,
                color, UiTokens.Type.LABEL);
    }

    private void drawColorPicker(ColorSetting color, Rect row) {
        String hex = color.getHex();
        Rect preview = new Rect(row.right() - 16, row.y + 3, 16, 16);
        VeyraFonts.semibold().drawPlain(hex,
                preview.x - VeyraFonts.semibold().width(hex, UiTokens.Type.CAPTION) - 6,
                row.y + 7, color.getColor(), UiTokens.Type.CAPTION);
        RenderUtils.drawRoundedRect(preview.x, preview.y, preview.right(), preview.bottom(), 4,
                color.getColor());
        RenderUtils.drawRoundedOutline(preview.x, preview.y, preview.right(), preview.bottom(),
                UiTokens.Radius.SM, 0.7F, withAlpha(0xFFFFFFFF, 136));

        Rect sv = colorSaturationValueRect(row);
        drawRect(sv.x, sv.y, sv.right(), sv.bottom(), UiTokens.Color.INPUT);
        drawSaturationValueGrid(sv, color.getHue());
        RenderUtils.drawRoundedOutline(sv.x - 1, sv.y - 1,
                sv.right() + 1, sv.bottom() + 1, 4, 0.65F, UiTokens.Color.BORDER);
        Rect hue = colorHueRect(row);
        for (int segment = 0; segment < 12; segment++) {
            int left = hue.x + Math.round(hue.width * (segment / 12.0F));
            int right = hue.x + Math.round(hue.width * ((segment + 1) / 12.0F));
            drawRect(left, hue.y, Math.max(left + 1, right), hue.bottom(),
                    opaqueHsb((segment + 0.5F) / 12.0F, 1, 1));
        }

        int markerX = sv.x + Math.round(sv.width * color.getSaturation());
        int markerY = sv.y + Math.round(sv.height * (1.0F - color.getBrightness()));
        RenderUtils.drawRoundedOutline(markerX - 3, markerY - 3,
                markerX + 3, markerY + 3, 3, 1.0F, UiTokens.Color.TEXT_PRIMARY);
        int hueX = hue.x + Math.round(hue.width * color.getHue());
        drawRect(hueX - 1, hue.y - 1, hueX + 1,
                hue.bottom() + 1, UiTokens.Color.TEXT_PRIMARY);
    }

    private void drawSaturationValueGrid(Rect rect, float hue) {
        // Discrete opaque cells are intentionally used instead of inherited
        // OpenGL gradients. They remain fully colored inside nested scissors
        // on Lunar's render pipeline and avoid the blank picker regression.
        // This used to issue 288 separate draw calls per picker, including
        // for pickers outside the viewport. A smaller grid remains visually
        // smooth at this size and keeps Lunar's legacy renderer responsive.
        final int columns = 12;
        final int rows = 6;
        for (int column = 0; column < columns; column++) {
            float s0 = column / (float) columns;
            float s1 = (column + 1) / (float) columns;
            int x0 = rect.x + Math.round(rect.width * s0);
            int x1 = rect.x + Math.round(rect.width * s1);
            for (int row = 0; row < rows; row++) {
                float v0 = 1.0F - row / (float) rows;
                float v1 = 1.0F - (row + 1) / (float) rows;
                int y0 = rect.y + Math.round(rect.height * (row / (float) rows));
                int y1 = rect.y + Math.round(rect.height * ((row + 1) / (float) rows));
                drawRect(x0, y0, Math.max(x0 + 1, x1), Math.max(y0 + 1, y1),
                        opaqueHsb(hue, (s0 + s1) * 0.5F, (v0 + v1) * 0.5F));
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (bindingModule != null) {
            bindingModule.setBind(mouseButton - 100);
            bindingModule = null;
            saveClientState();
            return;
        }
        if (settingsButtonRect.contains(mouseX, mouseY) && mouseButton == 0) {
            settingsOpen = !settingsOpen;
            searchFocused = false;
            return;
        }
        if (settingsOpen && themeSelectorRect.contains(mouseX, mouseY) && mouseButton == 0) {
            if (ClickGuiModule.clientTheme != null) {
                ClickGuiModule.clientTheme.nextMode();
                saveClientState();
            }
            return;
        }
        if (settingsOpen && notificationsToggleRect.contains(mouseX, mouseY) && mouseButton == 0) {
            if (ClickGuiModule.notifications != null) ClickGuiModule.notifications.toggle();
            saveClientState();
            return;
        }
        if (settingsOpen && shortcutButtonRect.contains(mouseX, mouseY) && mouseButton == 0) {
            bindingModule = clickGuiModule();
            return;
        }
        if (settingsOpen && settingsPanelRect.contains(mouseX, mouseY)) {
            return;
        }
        if (searchRect.contains(mouseX, mouseY)) {
            closeModuleSettings();
            searchFocused = true;
            searchCursor = searchIndexAt(mouseX);
            searchAnchor = searchCursor;
            return;
        }
        searchFocused = false;
        if (settingsOpen) settingsOpen = false;

        if (settingsModule != null) {
            if (settingsBackRect.contains(mouseX, mouseY) && mouseButton == 0) {
                closeModuleSettings();
                return;
            }
            if (settingsModuleToggleRect.contains(mouseX, mouseY) && mouseButton == 0) {
                if (settingsModule.canBeEnabled()) {
                    settingsModule.toggle(ModuleTransitionSource.GUI);
                    saveClientState();
                }
                return;
            }
        }

        for (CategoryHitbox hitbox : categoryHitboxes) {
            if (!hitbox.rect.contains(mouseX, mouseY) || mouseButton != 0) continue;
            favoritesSelected = hitbox.favorites;
            if (hitbox.category != null) selectedCategory = hitbox.category;
            searchQuery = "";
            searchCursor = 0;
            searchAnchor = 0;
            settingsModule = null;
            scroll = 0;
            categoryAnimation = 0.0F;
            return;
        }

        for (int i = settingHitboxes.size() - 1; i >= 0; i--) {
            SettingHitbox hitbox = settingHitboxes.get(i);
            if (!hitbox.isVisibleAt(mouseX, mouseY)) continue;
            handleSettingClick(hitbox, mouseX, mouseY, mouseButton);
            return;
        }
        for (int i = actionHitboxes.size() - 1; i >= 0; i--) {
            ActionHitbox hitbox = actionHitboxes.get(i);
            if (!hitbox.isVisibleAt(mouseX, mouseY) || mouseButton != 0) continue;
            if (hitbox.action == ActionType.BIND) bindingModule = hitbox.module;
            else if (hitbox.action == ActionType.BIND_MODE) hitbox.module.cycleBindMode();
            else hitbox.module.resetToDefaults();
            saveClientState();
            return;
        }
        for (int i = moduleHitboxes.size() - 1; i >= 0; i--) {
            ModuleHitbox hitbox = moduleHitboxes.get(i);
            if (!hitbox.isVisibleAt(mouseX, mouseY)) continue;
            if (hitbox.star.contains(mouseX, mouseY) && mouseButton == 0) {
                hitbox.module.toggleFavorite();
                saveClientState();
                return;
            }
            if (favoritesSelected && hitbox.drag.contains(mouseX, mouseY) && mouseButton == 0) {
                favoriteDragModule = hitbox.module;
                favoriteDragStartX = mouseX;
                favoriteDragStartY = mouseY;
                favoriteDragOffsetY = mouseY - hitbox.row.y;
                List<Module> favorites = visibleModules();
                favoriteDropIndex = favorites.indexOf(hitbox.module);
                favoriteDragging = false;
                return;
            }
            boolean expandClick = mouseButton == 1
                    || (mouseButton == 0 && hitbox.expand.contains(mouseX, mouseY));
            if (expandClick) {
                settingsModule = hitbox.module;
                scroll = 0;
                maxScroll = 0;
                searchFocused = false;
            } else if (mouseButton == 0 && hitbox.module.canBeEnabled()) {
                hitbox.module.toggle(ModuleTransitionSource.GUI);
            }
            saveClientState();
            return;
        }
        if (headerRect.contains(mouseX, mouseY) && !searchRect.contains(mouseX, mouseY)
                && mouseButton == 0) {
            draggingWindow = true;
            dragOffsetX = mouseX - windowX;
            dragOffsetY = mouseY - windowY;
        }
    }

    private void handleSettingClick(SettingHitbox hitbox, int mouseX, int mouseY, int mouseButton) {
        Setting setting = hitbox.setting;
        if (setting instanceof ColorSetting color && mouseButton == 0) {
            activeSlider = null;
            activeColor = color;
            activeColorRow = hitbox.row;
            if (colorHueRect(hitbox.row).contains(mouseX, mouseY)) {
                activeColorMode = ColorDragMode.HUE;
            } else if (colorSaturationValueRect(hitbox.row).contains(mouseX, mouseY)) {
                activeColorMode = ColorDragMode.SATURATION_VALUE;
            } else {
                activeColor = null;
                activeColorMode = null;
            }
            if (activeColor != null) updateColor(activeColor, activeColorRow, mouseX, mouseY, activeColorMode);
        } else if (setting instanceof TickSetting tick && mouseButton == 0) {
            tick.toggle();
            hitbox.module.guiButtonToggled(tick);
        } else if (setting instanceof ComboSetting<?> combo && (mouseButton == 0 || mouseButton == 1)) {
            combo.nextMode();
        } else if (setting instanceof SliderSetting slider && mouseButton == 0) {
            activeColor = null;
            activeSlider = slider;
            activeSliderRow = hitbox.row;
            updateSlider(slider, hitbox.row, mouseX);
        } else if (setting instanceof DoubleSliderSetting range && mouseButton == 0) {
            activeColor = null;
            float click = clamp01((mouseX - hitbox.row.x) / (float) Math.max(1, hitbox.row.width));
            float minimum = normalized(range.getInputMin(), range.getMin(), range.getMax());
            float maximum = normalized(range.getInputMax(), range.getMin(), range.getMax());
            activeRangeMaximum = Math.abs(maximum - minimum) < 0.0001F
                    ? minimum <= 0.0001F || (minimum < 0.9999F && click >= minimum)
                    : Math.abs(click - maximum) < Math.abs(click - minimum);
            activeSlider = range;
            activeSliderRow = hitbox.row;
            updateRange(range, hitbox.row, mouseX, activeRangeMaximum);
        }
        saveClientState();
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0 && favoriteDragging && favoriteDragModule != null
                && favoriteDropIndex >= 0) {
            reorderFavorite(favoriteDragModule, favoriteDropIndex);
        }
        favoriteDragModule = null;
        favoriteDropIndex = -1;
        favoriteDragging = false;
        draggingWindow = false;
        activeSlider = null;
        activeSliderRow = Rect.EMPTY;
        activeColor = null;
        activeColorRow = Rect.EMPTY;
        activeColorMode = null;
        saveClientState();
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (contentClip.contains(mouseX, mouseY)) {
            scroll = clamp(scroll + (wheel < 0 ? 24 : -24), 0, maxScroll);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (bindingModule != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                bindingModule = null;
                return;
            }
            if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK) {
                bindingModule.setBind(bindingModule instanceof ClickGuiModule
                        ? Keyboard.KEY_RETURN : Keyboard.KEY_NONE);
            } else {
                bindingModule.setBind(keyCode);
            }
            bindingModule = null;
            saveClientState();
            return;
        }
        if (searchFocused) {
            boolean control = isCtrlKeyDown();
            boolean shift = isShiftKeyDown();
            if (control && keyCode == Keyboard.KEY_A) {
                searchAnchor = 0;
                searchCursor = searchQuery.length();
            } else if (control && keyCode == Keyboard.KEY_C) {
                copySearchSelection();
            } else if (control && keyCode == Keyboard.KEY_X) {
                copySearchSelection();
                deleteSearchSelection();
            } else if (control && keyCode == Keyboard.KEY_V) {
                insertSearchText(getClipboardString());
            } else if (keyCode == Keyboard.KEY_ESCAPE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = "";
                    searchCursor = 0;
                    searchAnchor = 0;
                } else searchFocused = false;
            } else if (keyCode == Keyboard.KEY_RETURN) {
                searchFocused = false;
            } else if (keyCode == Keyboard.KEY_LEFT) {
                int next = control ? previousWord(searchCursor) : Math.max(0, searchCursor - 1);
                moveSearchCursor(next, shift);
            } else if (keyCode == Keyboard.KEY_RIGHT) {
                int next = control ? nextWord(searchCursor)
                        : Math.min(searchQuery.length(), searchCursor + 1);
                moveSearchCursor(next, shift);
            } else if (keyCode == Keyboard.KEY_HOME) {
                moveSearchCursor(0, shift);
            } else if (keyCode == Keyboard.KEY_END) {
                moveSearchCursor(searchQuery.length(), shift);
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!deleteSearchSelection() && searchCursor > 0) {
                    int start = control ? previousWord(searchCursor) : searchCursor - 1;
                    searchQuery = searchQuery.substring(0, start) + searchQuery.substring(searchCursor);
                    searchCursor = start;
                    searchAnchor = start;
                }
            } else if (keyCode == Keyboard.KEY_DELETE) {
                if (!deleteSearchSelection() && searchCursor < searchQuery.length()) {
                    int end = control ? nextWord(searchCursor) : searchCursor + 1;
                    searchQuery = searchQuery.substring(0, searchCursor) + searchQuery.substring(end);
                    searchAnchor = searchCursor;
                }
            } else if (!control && typedChar >= 32 && typedChar != 127) {
                insertSearchText(String.valueOf(typedChar));
            }
            scroll = 0;
            categoryAnimation = 0.0F;
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (settingsModule != null) closeModuleSettings();
            else mc.displayGuiScreen(null);
        }
    }

    private void insertSearchText(String value) {
        if (value == null || value.isEmpty()) return;
        StringBuilder cleaned = new StringBuilder();
        for (char character : value.toCharArray()) {
            if (character >= 32 && character != 127) cleaned.append(character);
        }
        if (cleaned.length() == 0) return;
        deleteSearchSelection();
        int available = 64 - searchQuery.length();
        if (available <= 0) return;
        String inserted = cleaned.substring(0, Math.min(available, cleaned.length()));
        searchQuery = searchQuery.substring(0, searchCursor) + inserted
                + searchQuery.substring(searchCursor);
        searchCursor += inserted.length();
        searchAnchor = searchCursor;
    }

    private boolean deleteSearchSelection() {
        if (searchAnchor == searchCursor) return false;
        int start = Math.min(searchAnchor, searchCursor);
        int end = Math.max(searchAnchor, searchCursor);
        searchQuery = searchQuery.substring(0, start) + searchQuery.substring(end);
        searchCursor = start;
        searchAnchor = start;
        return true;
    }

    private void copySearchSelection() {
        if (searchAnchor == searchCursor) return;
        int start = Math.min(searchAnchor, searchCursor);
        int end = Math.max(searchAnchor, searchCursor);
        setClipboardString(searchQuery.substring(start, end));
    }

    private void moveSearchCursor(int position, boolean keepSelection) {
        searchCursor = clamp(position, 0, searchQuery.length());
        if (!keepSelection) searchAnchor = searchCursor;
    }

    private int previousWord(int position) {
        int cursor = clamp(position, 0, searchQuery.length());
        while (cursor > 0 && Character.isWhitespace(searchQuery.charAt(cursor - 1))) cursor--;
        while (cursor > 0 && !Character.isWhitespace(searchQuery.charAt(cursor - 1))) cursor--;
        return cursor;
    }

    private int nextWord(int position) {
        int cursor = clamp(position, 0, searchQuery.length());
        while (cursor < searchQuery.length() && !Character.isWhitespace(searchQuery.charAt(cursor))) cursor++;
        while (cursor < searchQuery.length() && Character.isWhitespace(searchQuery.charAt(cursor))) cursor++;
        return cursor;
    }

    private int[] visibleSearchRange(int maximumWidth) {
        if (searchQuery.isEmpty()) return new int[]{0, 0};
        int start = 0;
        int cursor = clamp(searchCursor, 0, searchQuery.length());
        while (start < cursor && VeyraFonts.semibold().width(
                searchQuery.substring(start, cursor), UiTokens.Type.BODY) > maximumWidth) start++;
        int end = cursor;
        while (end < searchQuery.length() && VeyraFonts.semibold().width(
                searchQuery.substring(start, end + 1), UiTokens.Type.BODY) <= maximumWidth) end++;
        return new int[]{start, end};
    }

    private int searchIndexAt(int mouseX) {
        int[] visible = visibleSearchRange(searchRect.width - 20);
        float relative = mouseX - (searchRect.x + 10);
        int best = visible[0];
        float bestDistance = Math.abs(relative);
        for (int index = visible[0] + 1; index <= visible[1]; index++) {
            float x = VeyraFonts.semibold().width(
                    searchQuery.substring(visible[0], index), UiTokens.Type.BODY);
            float distance = Math.abs(relative - x);
            if (distance < bestDistance) {
                best = index;
                bestDistance = distance;
            }
        }
        return best;
    }

    private List<Module> visibleModules() {
        String query = searchQuery.trim().toLowerCase(Locale.ROOT);
        List<Module> modules = new ArrayList<>();
        for (Module module : Veyra.moduleManager.getModules()) {
            if (module instanceof ClickGuiModule) continue;
            boolean matches;
            if (!query.isEmpty()) {
                String haystack = (module.getName() + " " + categoryLabel(module.moduleCategory()) + " "
                        + ModuleDescriptions.forModule(module.getName())).toLowerCase(Locale.ROOT);
                matches = ModuleSearch.matches(haystack, query);
            } else if (favoritesSelected) {
                matches = module.isFavorite();
            } else {
                matches = module.moduleCategory() == selectedCategory;
            }
            if (matches) modules.add(module);
        }
        if (favoritesSelected && query.isEmpty()) {
            modules.sort(Comparator.comparingInt(Module::getFavoriteOrder)
                    .thenComparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        } else {
            modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        }
        return modules;
    }

    private void reorderFavorite(Module dragged, int targetIndex) {
        if (!dragged.isFavorite()) return;
        List<Module> favorites = new ArrayList<>();
        for (Module module : Veyra.moduleManager.getModules()) {
            if (module.isFavorite()) favorites.add(module);
        }
        favorites.sort(Comparator.comparingInt(Module::getFavoriteOrder)
                .thenComparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        favorites.remove(dragged);
        favorites.add(clamp(targetIndex, 0, favorites.size()), dragged);
        for (int index = 0; index < favorites.size(); index++) {
            favorites.get(index).setFavoriteOrder(index);
        }
        saveClientState();
    }

    private int contentHeight(List<Module> modules) {
        if (favoritesSelected && searchQuery.isEmpty()) {
            int height = CONTENT_PADDING + FAVORITES_GUIDE_HEIGHT + 4;
            for (Module module : modules) height += animatedModuleHeight(module) + 5;
            return Math.max(1, height + CONTENT_PADDING);
        }
        int[] heights = {CONTENT_PADDING, CONTENT_PADDING};
        for (int index = 0; index < modules.size(); index++) {
            Module module = modules.get(index);
            int column = index & 1;
            heights[column] += animatedModuleHeight(module) + 6;
        }
        return Math.max(1, Math.max(heights[0], heights[1]) + CONTENT_PADDING);
    }

    private int animatedModuleHeight(Module module) {
        return MODULE_HEADER_HEIGHT;
    }

    private int expandedHeight(Module module) {
        int height = 30;
        for (Setting setting : module.getSettings()) {
            if (!(setting instanceof DescriptionSetting)) height += settingHeight(setting);
        }
        return height;
    }

    private int settingHeight(Setting setting) {
        if (setting instanceof ColorSetting) return 100;
        if (setting instanceof DoubleSliderSetting) return 31;
        if (setting instanceof SliderSetting) return 30;
        return 24;
    }

    private void drawScrollbar() {
        Rect viewportRect = settingsModule == null ? contentClip : settingsViewport;
        int x = viewportRect.right() - 3;
        int viewport = viewportRect.height - 8;
        int content = viewport + maxScroll;
        int thumb = Math.max(18, viewport * viewport / Math.max(1, content));
        int travel = Math.max(1, viewport - thumb);
        int y = viewportRect.y + 4 + Math.round(travel * (scroll / (float) maxScroll));
        RenderUtils.drawRoundedRect(x, viewportRect.y + 4, x + 2,
                viewportRect.bottom() - 4, 1, UiTokens.Color.TRACK_INSET);
        RenderUtils.drawRoundedRect(x - 1, y, x + 2, y + thumb,
                1.5F, UiTokens.Color.TEXT_SECONDARY);
    }

    private void updateDraggedWindow(int mouseX, int mouseY) {
        if (!draggingWindow || !Mouse.isButtonDown(0)) return;
        windowX = mouseX - dragOffsetX;
        windowY = mouseY - dragOffsetY;
        clampWindow();
    }

    private void closeModuleSettings() {
        settingsModule = null;
        settingsViewport = Rect.EMPTY;
        settingsBackRect = Rect.EMPTY;
        settingsModuleToggleRect = Rect.EMPTY;
        scroll = 0;
        maxScroll = 0;
        bindingModule = null;
        activeSlider = null;
        activeSliderRow = Rect.EMPTY;
        activeColor = null;
        activeColorRow = Rect.EMPTY;
        activeColorMode = null;
    }

    private void clampWindow() {
        windowX = clamp(windowX, 4, Math.max(4, width - windowWidth - 4));
        windowY = clamp(windowY, 4, Math.max(4, height - windowHeight - 4));
    }

    private void updateDraggedSetting(int mouseX, int mouseY) {
        if (!Mouse.isButtonDown(0)) return;
        if (activeColor != null && activeColorMode != null) {
            updateColor(activeColor, activeColorRow, mouseX, mouseY, activeColorMode);
        } else if (activeSlider instanceof SliderSetting slider) {
            updateSlider(slider, activeSliderRow, mouseX);
        } else if (activeSlider instanceof DoubleSliderSetting range) {
            updateRange(range, activeSliderRow, mouseX, activeRangeMaximum);
        }
    }

    private void updateColor(ColorSetting color, Rect row, int mouseX, int mouseY, ColorDragMode mode) {
        if (mode == ColorDragMode.HUE) {
            Rect hue = colorHueRect(row);
            color.setHue(clamp01((mouseX - hue.x) / (float) Math.max(1, hue.width)));
        } else {
            Rect sv = colorSaturationValueRect(row);
            color.setSaturationAndBrightness(
                    clamp01((mouseX - sv.x) / (float) Math.max(1, sv.width)),
                    1.0F - clamp01((mouseY - sv.y) / (float) Math.max(1, sv.height)));
        }
    }

    private static Rect colorSaturationValueRect(Rect row) {
        return new Rect(row.x, row.y + 26, row.width, Math.max(30, row.height - 46));
    }

    private static Rect colorHueRect(Rect row) {
        return new Rect(row.x, row.bottom() - 12, row.width, 6);
    }

    private void updateSlider(SliderSetting slider, Rect row, int mouseX) {
        float progress = clamp01((mouseX - row.x) / (float) Math.max(1, row.width));
        slider.setValue(slider.getMin() + progress * (slider.getMax() - slider.getMin()));
    }

    private void updateRange(DoubleSliderSetting range, Rect row, int mouseX, boolean maximum) {
        float progress = clamp01((mouseX - row.x) / (float) Math.max(1, row.width));
        double value = range.getMin() + progress * (range.getMax() - range.getMin());
        if (maximum) range.setValueMax(value); else range.setValueMin(value);
    }

    private void beginScissor(Rect rect) {
        applyScissor(clipStack.push(new UiClipStack.Region(
                rect.x, rect.y, rect.width, rect.height)));
    }

    private void endScissor() {
        UiClipStack.Region parent = clipStack.pop();
        if (parent == null) GL11.glDisable(GL11.GL_SCISSOR_TEST);
        else applyScissor(parent);
    }

    private void applyScissor(UiClipStack.Region region) {
        int factor = new ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(region.x() * factor,
                mc.displayHeight - (region.y() + region.height()) * factor,
                Math.max(0, region.width() * factor),
                Math.max(0, region.height() * factor));
    }

    private void saveClientState() {
        if (Veyra.configManager != null) Veyra.configManager.save();
        if (Veyra.clientConfig != null) Veyra.clientConfig.saveConfig();
    }

    private Module clickGuiModule() {
        return Veyra.moduleManager == null ? null
                : Veyra.moduleManager.getModuleByClazz(ClickGuiModule.class);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        saveClientState();
        Module clickGui = clickGuiModule();
        if (clickGui != null && clickGui.isEnabled()) clickGui.disable();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public ArrayList<CategoryComponent> getCategoryList() {
        return categoryList;
    }

    private static boolean favoritesEnabled() {
        return true;
    }

    public static boolean isFavoritesCategoryEnabled() {
        return true;
    }

    public static void setFavoritesCategoryEnabled(boolean enabled) {
        favoritesCategoryEnabled = true;
    }

    private static String categoryLabel(Module.ModuleCategory category) {
        return switch (category) {
            case Combat -> "Combat";
            case Movement -> "Movement";
            case Player -> "Player";
            case Render -> "Visuals";
            case Client -> "Client";
        };
    }

    private static String cleanLabel(String value) {
        if (value == null) return "Setting";
        return value.replace("(New)", "").replace("(for bypassing)", "")
                .replace("recieve", "receive").replace("AutoDisable", "Auto disable")
                .replace("GroundSpoof", "Ground spoof").replaceAll("\\s+", " ")
                .replaceAll(":$", "").trim();
    }

    public static String keyName(int keyCode) {
        if (keyCode == Keyboard.KEY_NONE) return "Not bound";
        if (keyCode < 0) {
            int button = keyCode + 100;
            if (button == 0) return "Mouse Left";
            if (button == 1) return "Mouse Right";
            if (button == 2) return "Mouse Middle";
            return "Mouse " + (button + 1);
        }
        String name = Keyboard.getKeyName(keyCode);
        return name == null ? "Key " + keyCode : name
                .replace("LMENU", "Left Alt").replace("RMENU", "Right Alt")
                .replace("LCONTROL", "Left Ctrl").replace("RCONTROL", "Right Ctrl")
                .replace("LSHIFT", "Left Shift").replace("RSHIFT", "Right Shift")
                .replace("CAPITAL", "Caps Lock").replace("RETURN", "Enter");
    }

    private static String bindModeLabel(Module module) {
        return switch (module.getBindMode()) {
            case TOGGLE -> "Toggle";
            case HOLD -> "Hold";
            case PRESS -> "Press";
        };
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) return String.valueOf((long) Math.rint(value));
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.endsWith("0")) text = text.substring(0, text.length() - 1);
        return text;
    }

    private static float normalized(double value, double min, double max) {
        return max <= min ? 0.0F : clamp01((float) ((value - min) / (max - min)));
    }

    private static String ellipsize(String value, float maxWidth, VeyraFont font, float height) {
        if (value == null) return "";
        if (font.width(value, height) <= maxWidth) return value;
        String result = value;
        while (!result.isEmpty() && font.width(result + "…", height) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }

    private static int opaqueHsb(float hue, float saturation, float brightness) {
        return 0xFF000000 | (Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF);
    }

    private static int accent() {
        return Theme.getMainColor().getRGB();
    }

    private static int withAlpha(int color, int alpha) {
        return clamp(alpha, 0, 255) << 24 | color & 0x00FFFFFF;
    }

    private static int mix(int first, int second, float amount) {
        amount = clamp01(amount);
        int a = Math.round((first >>> 24 & 255) + ((second >>> 24 & 255) - (first >>> 24 & 255)) * amount);
        int r = Math.round((first >> 16 & 255) + ((second >> 16 & 255) - (first >> 16 & 255)) * amount);
        int g = Math.round((first >> 8 & 255) + ((second >> 8 & 255) - (first >> 8 & 255)) * amount);
        int b = Math.round((first & 255) + ((second & 255) - (first & 255)) * amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0F - clamp01(value);
        return 1.0F - inverse * inverse * inverse;
    }

    private static float easeOutBack(float value) {
        float x = clamp01(value) - 1.0F;
        return 1.0F + 2.70158F * x * x * x + 1.70158F * x * x;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean verticallyVisible(Rect rect, Rect clip) {
        return rect.height > 0 && rect.bottom() > clip.y && rect.y < clip.bottom();
    }

    private enum ActionType { BIND, BIND_MODE, RESET }
    private enum ColorDragMode { SATURATION_VALUE, HUE }
    private static final class CategoryHitbox {
        private final Module.ModuleCategory category;
        private final boolean favorites;
        private final Rect rect;

        private CategoryHitbox(Module.ModuleCategory category, boolean favorites, Rect rect) {
            this.category = category;
            this.favorites = favorites;
            this.rect = rect;
        }
    }

    private static final class ModuleHitbox {
        private final Module module;
        private final Rect row;
        private final Rect expand;
        private final Rect star;
        private final Rect drag;
        private final Rect clip;

        private ModuleHitbox(Module module, Rect row, Rect expand, Rect star,
                             Rect drag, Rect clip) {
            this.module = module;
            this.row = row;
            this.expand = expand;
            this.star = star;
            this.drag = drag;
            this.clip = clip;
        }

        private boolean isVisibleAt(int x, int y) {
            return clip.contains(x, y) && row.contains(x, y);
        }
    }

    private static final class ActionHitbox {
        private final Module module;
        private final ActionType action;
        private final Rect row;
        private final Rect clip;

        private ActionHitbox(Module module, ActionType action, Rect row, Rect clip) {
            this.module = module;
            this.action = action;
            this.row = row;
            this.clip = clip;
        }

        private boolean isVisibleAt(int x, int y) {
            return clip.contains(x, y) && row.contains(x, y);
        }
    }

    private static final class SettingHitbox {
        private final Module module;
        private final Setting setting;
        private final Rect row;
        private final Rect clip;

        private SettingHitbox(Module module, Setting setting, Rect row, Rect clip) {
            this.module = module;
            this.setting = setting;
            this.row = row;
            this.clip = clip;
        }

        private boolean isVisibleAt(int x, int y) {
            return clip.contains(x, y) && row.contains(x, y);
        }
    }

    private static final class Rect {
        private static final Rect EMPTY = new Rect(0, 0, 0, 0);
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = Math.max(0, width);
            this.height = Math.max(0, height);
        }

        private int right() { return x + width; }
        private int bottom() { return y + height; }
        private boolean contains(int px, int py) {
            return px >= x && px <= right() && py >= y && py <= bottom();
        }
    }

}
