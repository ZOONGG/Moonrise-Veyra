package dev.veyra.client.module.modules.client;

import dev.veyra.client.clickgui.ArrayListPosition;
import dev.veyra.client.clickgui.ArrayListVisibilityScreen;
import dev.veyra.client.clickgui.FrameMotion;
import dev.veyra.client.clickgui.Theme;
import dev.veyra.client.clickgui.font.VeyraFont;
import dev.veyra.client.clickgui.font.VeyraFonts;
import dev.veyra.client.main.Veyra;
import dev.veyra.client.config.ArrayListVisibilityStore;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.ColorSetting;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.Utils;
import dev.veyra.client.utils.player.PlayerUtils;
import dev.veyra.client.utils.render.RenderUtils;
import lombok.Getter;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.weavemc.api.event.RenderGameOverlayEvent;
import net.weavemc.api.event.SubscribeEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Fully configurable enabled-module Text GUI. */
@SuppressWarnings("unused")
public final class ArrayListModule extends Module {
    public static final String HUDX_prefix = "HUDX~ ";
    public static final String HUDY_prefix = "HUDY~ ";
    public static final String FILTER_PREFIX = "arraylist-filter~ ";

    public static TickSetting editPosition;
    public static TickSetting editModuleList;
    public static ComboSetting<FilterMode> filterMode;

    public static ComboSetting<SortMode> sortMode;
    private final ComboSetting<SortDirection> sortDirection;
    private final ComboSetting<Alignment> alignment;
    private final ComboSetting<FontMode> fontMode;
    private final ComboSetting<SuffixMode> suffixMode;
    private final ComboSetting<TextMode> textMode;
    private final ComboSetting<ColorMode> colorMode;
    private final ComboSetting<BackgroundMode> backgroundMode;
    private final ComboSetting<AccentMode> accentMode;
    private final ComboSetting<AccentSide> accentSide;
    private final ComboSetting<AnimationMode> animationMode;

    private final SliderSetting fontSize;
    private final SliderSetting horizontalPadding;
    private final SliderSetting verticalPadding;
    private final SliderSetting rowSpacing;
    private final SliderSetting cornerRadius;
    private final SliderSetting backgroundOpacity;
    private final SliderSetting outlineOpacity;
    private final SliderSetting accentThickness;
    private final SliderSetting animationDuration;
    private final SliderSetting colorSpeed;
    private final TickSetting textShadow;

    private final ColorSetting primaryColor;
    private final ColorSetting secondaryColor;
    private final ColorSetting customTextColor;
    private final ColorSetting backgroundColor;
    private final ColorSetting outlineColor;

    private static final Set<String> FILTERED_MODULES = new LinkedHashSet<>();
    private static final ArrayListVisibilityStore FILTER_STORE = ArrayListVisibilityStore.userDefault();
    private final Map<String, EntryAnimation> animations = new HashMap<>();
    private long lastFrameNanos;

    @Getter
    public static int hudX = 5;
    @Getter
    public static int hudY = 5;
    public static Utils.HUD.PositionMode positionMode;

    public ArrayListModule() {
        super("ArrayList", ModuleCategory.Client, 0);
        registerSetting(new DescriptionSetting("Highly configurable enabled-module Text GUI."));

        registerSetting(editPosition = new TickSetting("Edit position", false));
        registerSetting(editModuleList = new TickSetting("Edit module list", false));
        registerSetting(filterMode = new ComboSetting<>("List mode", FilterMode.Blacklist));
        registerSetting(sortMode = new ComboSetting<>("Sort", SortMode.Length));
        registerSetting(sortDirection = new ComboSetting<>("Sort direction", SortDirection.LongToShort));
        registerSetting(alignment = new ComboSetting<>("Alignment", Alignment.Auto));
        registerSetting(fontMode = new ComboSetting<>("Font", FontMode.Semibold));
        registerSetting(fontSize = new SliderSetting("Font size", 9.0D, 6.0D, 16.0D, 0.5D));
        registerSetting(suffixMode = new ComboSetting<>("Suffix", SuffixMode.Dash));
        registerSetting(textMode = new ComboSetting<>("Text color", TextMode.Colored));
        registerSetting(textShadow = new TickSetting("Text shadow", true));

        registerSetting(colorMode = new ComboSetting<>("Color mode", ColorMode.Gradient));
        registerSetting(primaryColor = new ColorSetting("Primary color", 0xFF8B5CF6));
        registerSetting(secondaryColor = new ColorSetting("Secondary color", 0xFF22D3EE));
        registerSetting(customTextColor = new ColorSetting("Custom text color", 0xFFF5F7FF));
        registerSetting(colorSpeed = new SliderSetting("Color speed", 1.0D, 0.1D, 5.0D, 0.1D));

        registerSetting(backgroundMode = new ComboSetting<>("Background", BackgroundMode.PerRow));
        registerSetting(backgroundColor = new ColorSetting("Background color", 0xFF0B0E15));
        registerSetting(backgroundOpacity = new SliderSetting("Background opacity", 60.0D, 0.0D, 100.0D, 1.0D));
        registerSetting(outlineColor = new ColorSetting("Outline color", 0xFF252B3A));
        registerSetting(outlineOpacity = new SliderSetting("Outline opacity", 0.0D, 0.0D, 100.0D, 1.0D));
        registerSetting(accentMode = new ComboSetting<>("Accent", AccentMode.Bar));
        registerSetting(accentSide = new ComboSetting<>("Accent side", AccentSide.Auto));
        registerSetting(accentThickness = new SliderSetting("Accent thickness", 2.0D, 1.0D, 5.0D, 1.0D));

        registerSetting(horizontalPadding = new SliderSetting("Horizontal padding", 6.0D, 0.0D, 16.0D, 1.0D));
        registerSetting(verticalPadding = new SliderSetting("Vertical padding", 2.0D, 0.0D, 8.0D, 1.0D));
        registerSetting(rowSpacing = new SliderSetting("Row spacing", 1.0D, 0.0D, 8.0D, 1.0D));
        registerSetting(cornerRadius = new SliderSetting("Corner radius", 3.0D, 0.0D, 8.0D, 1.0D));

        registerSetting(animationMode = new ComboSetting<>("Animation", AnimationMode.SlideFade));
        registerSetting(animationDuration = new SliderSetting("Animation ms", 180.0D, 50.0D, 600.0D, 10.0D));
    }

    @Override
    public void guiButtonToggled(TickSetting tick) {
        if (tick == editPosition) {
            editPosition.disable();
            mc.displayGuiScreen(new ArrayListPosition());
        } else if (tick == editModuleList) {
            editModuleList.disable();
            mc.displayGuiScreen(new ArrayListVisibilityScreen());
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!PlayerUtils.isPlayerInGame()
                || mc.currentScreen != null
                || mc.gameSettings.showDebugInfo) {
            lastFrameNanos = System.nanoTime();
            return;
        }

        long nowNanos = System.nanoTime();
        double deltaSeconds = lastFrameNanos == 0L
                ? 1.0D / 60.0D
                : Math.min(0.1D, Math.max(0.0D, (nowNanos - lastFrameNanos) / 1_000_000_000.0D));
        lastFrameNanos = nowNanos;

        List<DisplayEntry> entries = updateEntries(deltaSeconds);
        if (entries.isEmpty()) return;
        renderEntries(entries, hudX, hudY, true);
    }

    private List<DisplayEntry> updateEntries(double deltaSeconds) {
        List<DisplayEntry> entries = new ArrayList<>();
        double durationSeconds = Math.max(0.05D, animationDuration.getInput() / 1000.0D);
        double response = 7.0D / durationSeconds;
        boolean animated = animationMode.getMode() != AnimationMode.Off;

        for (Module module : Veyra.moduleManager.getModules()) {
            if (module == this) continue;
            boolean desired = module.isEnabled() && isAllowedByFilter(module);
            EntryAnimation animation = animations.computeIfAbsent(
                    module.getName().toLowerCase(Locale.ROOT),
                    ignored -> new EntryAnimation(animated && desired ? 0.0D : desired ? 1.0D : 0.0D));
            animation.progress = animated
                    ? FrameMotion.expApproach(animation.progress, desired ? 1.0D : 0.0D,
                    response, deltaSeconds)
                    : desired ? 1.0D : 0.0D;
            if (animation.progress > 0.005D) {
                entries.add(new DisplayEntry(module, displayName(module), animation.progress));
            }
        }

        sortEntries(entries);
        return entries;
    }

    private void sortEntries(List<DisplayEntry> entries) {
        Comparator<DisplayEntry> comparator;
        switch (sortMode.getMode()) {
            case Alphabetical:
                comparator = Comparator.comparing(DisplayEntry::text, String.CASE_INSENSITIVE_ORDER);
                break;
            case Category:
                comparator = Comparator.comparingInt(entry -> entry.module().moduleCategory().ordinal());
                comparator = comparator.thenComparing(DisplayEntry::text, String.CASE_INSENSITIVE_ORDER);
                break;
            case Length:
            default:
                comparator = Comparator.comparingDouble(entry -> textWidth(entry.text()));
                break;
        }
        if (sortDirection.getMode() == SortDirection.LongToShort) comparator = comparator.reversed();
        entries.sort(comparator);
    }

    private int[] renderEntries(List<DisplayEntry> entries, int requestedX, int requestedY, boolean clampToScreen) {
        int count = entries.size();
        int fontHeight = (int) Math.ceil(fontSize.getInput());
        int padX = (int) Math.round(horizontalPadding.getInput());
        int padY = (int) Math.round(verticalPadding.getInput());
        int spacing = (int) Math.round(rowSpacing.getInput());
        int rowHeight = Math.max(1, fontHeight + padY * 2);
        int maximumWidth = 1;
        for (DisplayEntry entry : entries) {
            maximumWidth = Math.max(maximumWidth, (int) Math.ceil(textWidth(entry.text())) + padX * 2
                    + (accentMode.getMode() == AccentMode.Bar ? (int) accentThickness.getInput() : 0));
        }
        int totalHeight = count * rowHeight + Math.max(0, count - 1) * spacing;

        ScaledResolution resolution = new ScaledResolution(mc);
        int x = requestedX;
        int y = requestedY;
        if (clampToScreen) {
            x = clamp(x, 2, Math.max(2, resolution.getScaledWidth() - maximumWidth - 2));
            y = clamp(y, 2, Math.max(2, resolution.getScaledHeight() - totalHeight - 2));
            hudX = x;
            hudY = y;
        }

        boolean alignRight = resolveRightAlignment(x, maximumWidth, resolution.getScaledWidth());
        boolean accentRight = resolveAccentRight(alignRight);
        int radius = (int) Math.round(cornerRadius.getInput());
        int background = withAlpha(backgroundColor.getColor(), percentAlpha(backgroundOpacity.getInput()));
        int outline = withAlpha(outlineColor.getColor(), percentAlpha(outlineOpacity.getInput()));

        if (backgroundMode.getMode() == BackgroundMode.Full) {
            RenderUtils.drawRoundedRect(x, y, x + maximumWidth, y + totalHeight, radius, background);
            if (outlineOpacity.getInput() > 0.0D) {
                RenderUtils.drawRoundedOutline(x, y, x + maximumWidth, y + totalHeight,
                        radius, 0.8F, outline);
            }
        }

        int rowY = y;
        for (int index = 0; index < entries.size(); index++) {
            DisplayEntry entry = entries.get(index);
            float progress = easeOutCubic((float) entry.progress());
            int textWidth = (int) Math.ceil(textWidth(entry.text()));
            int rowWidth = textWidth + padX * 2
                    + (accentMode.getMode() == AccentMode.Bar ? (int) accentThickness.getInput() : 0);
            int baseLeft = alignRight ? x + maximumWidth - rowWidth : x;
            float slideDistance = animationMode.getMode() == AnimationMode.Slide
                    || animationMode.getMode() == AnimationMode.SlideFade
                    ? (1.0F - progress) * (rowWidth + 8.0F) : 0.0F;
            float rowLeft = baseLeft + (alignRight ? slideDistance : -slideDistance);
            float rowRight = rowLeft + rowWidth;
            int alpha = animationMode.getMode() == AnimationMode.Fade
                    || animationMode.getMode() == AnimationMode.SlideFade
                    ? Math.round(255.0F * progress) : 255;
            int color = withAlpha(moduleColor(entry.module(), index, count), alpha);

            if (backgroundMode.getMode() == BackgroundMode.PerRow) {
                RenderUtils.drawRoundedRect(rowLeft, rowY, rowRight, rowY + rowHeight,
                        radius, multiplyAlpha(background, alpha));
                if (outlineOpacity.getInput() > 0.0D) {
                    RenderUtils.drawRoundedOutline(rowLeft, rowY, rowRight, rowY + rowHeight,
                            radius, 0.8F, multiplyAlpha(outline, alpha));
                }
            }

            if (accentMode.getMode() == AccentMode.Outline) {
                RenderUtils.drawRoundedOutline(rowLeft, rowY, rowRight, rowY + rowHeight,
                        radius, Math.max(0.8F, (float) accentThickness.getInput()), color);
            } else if (accentMode.getMode() == AccentMode.Bar) {
                int thickness = (int) Math.round(accentThickness.getInput());
                float barLeft = accentRight ? rowRight - thickness : rowLeft;
                RenderUtils.drawRoundedRect(barLeft, rowY, barLeft + thickness, rowY + rowHeight,
                        Math.min(radius, thickness), color);
            }

            float contentLeft = rowLeft + padX + (!accentRight && accentMode.getMode() == AccentMode.Bar
                    ? (int) accentThickness.getInput() : 0);
            float contentRight = rowRight - padX - (accentRight && accentMode.getMode() == AccentMode.Bar
                    ? (int) accentThickness.getInput() : 0);
            float textX = alignRight ? contentRight - textWidth : contentLeft;
            int textColor = resolveTextColor(color, alpha);
            drawText(entry.text(), textX, rowY + padY, textColor);
            rowY += rowHeight + spacing;
        }
        return new int[]{maximumWidth, totalHeight};
    }

    /** Renders the exact configured style inside the drag-position screen. */
    public int[] renderPositionPreview(int x, int y) {
        List<DisplayEntry> preview = new ArrayList<>();
        if (Veyra.moduleManager != null) {
            for (Module module : Veyra.moduleManager.getModules()) {
                if (module == this) continue;
                if (module.isEnabled() && isAllowedByFilter(module)) {
                    preview.add(new DisplayEntry(module, displayName(module), 1.0D));
                }
                if (preview.size() >= 5) break;
            }
            if (preview.size() < 3) {
                for (Module module : Veyra.moduleManager.getModules()) {
                    if (module == this || preview.stream().anyMatch(entry -> entry.module() == module)) continue;
                    preview.add(new DisplayEntry(module, displayName(module), 1.0D));
                    if (preview.size() >= 3) break;
                }
            }
        }
        sortEntries(preview);
        return renderEntries(preview, x, y, false);
    }

    private String displayName(Module module) {
        if (suffixMode.getMode() == SuffixMode.Hidden || module.getSuffix() == null || module.getSuffix().isBlank()) {
            return module.getName();
        }
        if (suffixMode.getMode() == SuffixMode.Brackets) {
            return module.getName() + " [" + module.getSuffix() + "]";
        }
        return module.getDisplayName();
    }

    private int moduleColor(Module module, int index, int count) {
        int first = primaryColor.getColor();
        int second = secondaryColor.getColor();
        double speed = colorSpeed.getInput();
        double time = System.currentTimeMillis() / 1000.0D * speed;
        switch (colorMode.getMode()) {
            case Static:
                return first;
            case Category:
                return Theme.moduleAccent(module.getName(), module.moduleCategory().ordinal());
            case Rainbow:
                float hue = (float) ((time * 0.12D + index / (double) Math.max(1, count)) % 1.0D);
                return 0xFF000000 | (Color.HSBtoRGB(hue, 0.72F, 1.0F) & 0x00FFFFFF);
            case Fade:
                double fade = (Math.sin(time * 1.8D + index * 0.45D) + 1.0D) * 0.5D;
                return interpolate(first, second, fade);
            case Breathe:
                int breathed = interpolate(first, second, 0.15D);
                double brightness = 0.68D + (Math.sin(time * 2.2D + index * 0.35D) + 1.0D) * 0.16D;
                return scaleBrightness(breathed, brightness);
            case Gradient:
            default:
                return interpolate(first, second, count <= 1 ? 0.0D : index / (double) (count - 1));
        }
    }

    private int resolveTextColor(int moduleColor, int alpha) {
        switch (textMode.getMode()) {
            case White:
                return withAlpha(0xFFFFFFFF, alpha);
            case Custom:
                return withAlpha(customTextColor.getColor(), alpha);
            case Colored:
            default:
                return moduleColor;
        }
    }

    private void drawText(String text, float x, float y, int color) {
        float size = (float) fontSize.getInput();
        if (fontMode.getMode() == FontMode.Minecraft) {
            float scale = size / Math.max(1.0F, mc.fontRendererObj.FONT_HEIGHT);
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.scale(scale, scale, 1.0F);
            float inverse = 1.0F / scale;
            if (textShadow.isToggled()) {
                mc.fontRendererObj.drawStringWithShadow(text, x * inverse, y * inverse, color);
            } else {
                mc.fontRendererObj.drawString(text, (int) (x * inverse), (int) (y * inverse), color);
            }
            net.minecraft.client.renderer.GlStateManager.popMatrix();
            return;
        }
        VeyraFont font = selectedVeyraFont();
        if (textShadow.isToggled()) font.draw(text, x, y, color, size);
        else font.drawPlain(text, x, y, color, size);
    }

    private float textWidth(String text) {
        float size = (float) fontSize.getInput();
        if (fontMode.getMode() == FontMode.Minecraft) {
            return mc.fontRendererObj.getStringWidth(text) * size / Math.max(1.0F, mc.fontRendererObj.FONT_HEIGHT);
        }
        return selectedVeyraFont().width(text, size);
    }

    private VeyraFont selectedVeyraFont() {
        return fontMode.getMode() == FontMode.Regular ? VeyraFonts.regular() : VeyraFonts.semibold();
    }

    private boolean resolveRightAlignment(int x, int width, int screenWidth) {
        if (alignment.getMode() == Alignment.Left) return false;
        if (alignment.getMode() == Alignment.Right) return true;
        return x + width / 2 >= screenWidth / 2;
    }

    private boolean resolveAccentRight(boolean alignRight) {
        if (accentSide.getMode() == AccentSide.Left) return false;
        if (accentSide.getMode() == AccentSide.Right) return true;
        return alignRight;
    }

    public static boolean isAllowedByFilter(Module module) {
        boolean member = FILTERED_MODULES.contains(module.getName().toLowerCase(Locale.ROOT));
        return filterMode == null || filterMode.getMode() == FilterMode.Blacklist ? !member : member;
    }

    public static boolean isFilterMember(Module module) {
        return FILTERED_MODULES.contains(module.getName().toLowerCase(Locale.ROOT));
    }

    public static void toggleFilterMember(Module module) {
        String key = module.getName().toLowerCase(Locale.ROOT);
        if (!FILTERED_MODULES.remove(key)) FILTERED_MODULES.add(key);
        persistFilterMembers();
    }

    public static void clearFilterMembers() {
        FILTERED_MODULES.clear();
        persistFilterMembers();
    }

    public static void selectAllFilterMembers() {
        FILTERED_MODULES.clear();
        if (Veyra.moduleManager == null) return;
        for (Module module : Veyra.moduleManager.getModules()) {
            if (!(module instanceof ArrayListModule)) {
                FILTERED_MODULES.add(module.getName().toLowerCase(Locale.ROOT));
            }
        }
        persistFilterMembers();
    }

    public static String serializeFilterMembers() {
        return String.join(",", FILTERED_MODULES);
    }

    public static void deserializeFilterMembers(String value) {
        FILTERED_MODULES.clear();
        if (value == null || value.isBlank()) return;
        for (String name : value.split(",")) {
            String cleaned = name.trim().toLowerCase(Locale.ROOT);
            if ("playeresp".equals(cleaned)) cleaned = "esp";
            if (!cleaned.isEmpty()) FILTERED_MODULES.add(cleaned);
        }
    }

    public static boolean hasPersistedFilterMembers() {
        return FILTER_STORE.exists();
    }

    public static void loadPersistedFilterMembers() {
        try {
            FILTERED_MODULES.clear();
            FILTERED_MODULES.addAll(FILTER_STORE.load());
        } catch (Exception exception) {
            System.err.println("Veyra: failed to load ArrayList module selection");
            exception.printStackTrace();
        }
    }

    public static void persistFilterMembers() {
        try {
            FILTER_STORE.save(FILTERED_MODULES);
        } catch (Exception exception) {
            System.err.println("Veyra: failed to save ArrayList module selection");
            exception.printStackTrace();
        }
    }

    public static void setHudX(int value) {
        hudX = value;
    }

    public static void setHudY(int value) {
        hudY = value;
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    private static int percentAlpha(double percent) {
        return clamp((int) Math.round(percent / 100.0D * 255.0D), 0, 255);
    }

    private static int withAlpha(int color, int alpha) {
        return clamp(alpha, 0, 255) << 24 | color & 0x00FFFFFF;
    }

    private static int multiplyAlpha(int color, int alpha) {
        return withAlpha(color, (color >>> 24 & 255) * clamp(alpha, 0, 255) / 255);
    }

    private static int interpolate(int first, int second, double amount) {
        amount = Math.max(0.0D, Math.min(1.0D, amount));
        int red = (int) Math.round((first >> 16 & 255) + ((second >> 16 & 255) - (first >> 16 & 255)) * amount);
        int green = (int) Math.round((first >> 8 & 255) + ((second >> 8 & 255) - (first >> 8 & 255)) * amount);
        int blue = (int) Math.round((first & 255) + ((second & 255) - (first & 255)) * amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int scaleBrightness(int color, double factor) {
        int red = clamp((int) Math.round((color >> 16 & 255) * factor), 0, 255);
        int green = clamp((int) Math.round((color >> 8 & 255) * factor), 0, 255);
        int blue = clamp((int) Math.round((color & 255) * factor), 0, 255);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum FilterMode { Blacklist, Whitelist }
    public enum SortMode { Length, Alphabetical, Category }
    public enum SortDirection { LongToShort, ShortToLong }
    public enum Alignment { Auto, Left, Right }
    public enum FontMode { Regular, Semibold, Minecraft }
    public enum SuffixMode { Dash, Brackets, Hidden }
    public enum TextMode { Colored, White, Custom }
    public enum ColorMode { Static, Gradient, Rainbow, Fade, Breathe, Category }
    public enum BackgroundMode { None, PerRow, Full }
    public enum AccentMode { None, Bar, Outline }
    public enum AccentSide { Auto, Left, Right }
    public enum AnimationMode { Off, Slide, Fade, SlideFade }

    private record DisplayEntry(Module module, String text, double progress) {
    }

    private static final class EntryAnimation {
        private double progress;

        private EntryAnimation(double progress) {
            this.progress = progress;
        }
    }
}
