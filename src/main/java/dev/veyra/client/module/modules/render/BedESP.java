package dev.veyra.client.module.modules.render;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.ColorSetting;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.main.Veyra;
import dev.veyra.client.utils.render.WorldOverlayRenderer;
import dev.veyra.client.utils.world.BedTracker;
import net.minecraft.util.BlockPos;
import net.weavemc.api.event.RenderWorldEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

public class BedESP extends Module {
    public static SliderSetting range, lineWidth, outlineOpacity, fillOpacity;
    public static ColorSetting color;
    public static ComboSetting<Style> style;

    public BedESP() {
        super("BedESP", ModuleCategory.Render, 0);
        registerSetting(new DescriptionSetting("Full bed-sized ESP rendered through walls."));
        registerSetting(range = new SliderSetting("Range", 96, 16, 128, 8));
        registerSetting(style = new ComboSetting<>("Style", Style.Hitbox));
        registerSetting(color = new ColorSetting("Color", 0xFFA855F7));
        registerSetting(lineWidth = new SliderSetting("Line width", 1.0D, 0.5D, 5.0D, 0.5D));
        registerSetting(outlineOpacity = new SliderSetting("Outline opacity", 100, 10, 100, 5));
        registerSetting(fillOpacity = new SliderSetting("Fill opacity", 24, 0, 80, 2));
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            BedTracker.update((int) range.getInput());
            return;
        }
        BedTracker.update((int) range.getInput());
    }

    @SubscribeEvent
    public void onRender(RenderWorldEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        double maxDistanceSq = range.getInput() * range.getInput();
        int color = BedESP.color.getColor();
        int rendered = 0;
        java.util.List<BedTracker.BedEntry> beds = BedTracker.getBeds();
        BlockPos ownBedHead = findOwnBedHead(beds);
        for (int bedIndex = 0; bedIndex < beds.size(); bedIndex++) {
            BedTracker.BedEntry bed = beds.get(bedIndex);
            if (ownBedHead != null && ownBedHead.equals(bed.head())) continue;
            if (mc.thePlayer.getDistanceSqToCenter(bed.centerBlock()) > maxDistanceSq) continue;
            if (style.getMode() == Style.Hitbox) {
                // Default-looking block hitboxes: exact geometry, outline only.
                drawBox(BedTracker.getPartBounds(bed.head()), color, true, false);
                drawBox(BedTracker.getPartBounds(bed.foot()), color, true, false);
            } else {
                drawBox(BedTracker.getBounds(bed), color,
                        style.getMode() != Style.Filled,
                        style.getMode() != Style.Outline);
            }
            rendered++;
        }
        setSuffix(style.getMode() + " · " + rendered);
    }

    @Override
    public void onDisable() {
        setSuffix(null);
    }

    private BlockPos findOwnBedHead(java.util.List<BedTracker.BedEntry> beds) {
        if (Veyra.bedwarsBaseAnchorTracker == null) return null;
        BlockPos ownBaseAnchor = Veyra.bedwarsBaseAnchorTracker.anchorFor(mc.theWorld);
        if (ownBaseAnchor == null) return null;
        java.util.List<OwnBedSelector.BedPoint> bedPoints = beds.stream()
                .map(bed -> new OwnBedSelector.BedPoint(
                        bed.centerBlock().getX(), bed.centerBlock().getZ()))
                .toList();
        int ownBedIndex = OwnBedSelector.findIndex(
                bedPoints, ownBaseAnchor.getX(), ownBaseAnchor.getZ());
        return ownBedIndex < 0 ? null : beds.get(ownBedIndex).head();
    }

    private void drawBox(net.minecraft.util.AxisAlignedBB bounds, int color, boolean outline, boolean fill) {
        WorldOverlayRenderer.drawBedBox(
                bounds,
                color,
                outline,
                fill,
                (float) lineWidth.getInput(),
                (float) (outlineOpacity.getInput() / 100.0D),
                (float) (fillOpacity.getInput() / 100.0D));
    }

    public enum Style {
        Hitbox, Outline, Filled, Both
    }
}
