package dev.veyra.client.module.modules.render;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.utils.render.WorldOverlayRenderer;
import dev.veyra.client.utils.world.BedTracker;
import net.weavemc.api.event.RenderWorldEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Vape-style defense plates using actual Minecraft block item models. */
public class BedPlates extends Module {
    public static SliderSetting range, protectionRadius, maxMaterials, scale;
    private final Map<BedTracker.BedEntry, List<BedTracker.ProtectionEntry>> overlays = new LinkedHashMap<>();
    private long nextRefresh;

    public BedPlates() {
        super("BedPlates", ModuleCategory.Render, 0);
        registerSetting(new DescriptionSetting("Shows real defense-block icons above nearby beds."));
        registerSetting(range = new SliderSetting("Range", 96, 16, 128, 8));
        registerSetting(protectionRadius = new SliderSetting("Detection radius", 4, 1, 6, 1));
        registerSetting(maxMaterials = new SliderSetting("Max materials", 5, 1, 8, 1));
        registerSetting(scale = new SliderSetting("Scale", 1.0D, 0.6D, 1.6D, 0.05D));
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        BedTracker.update((int) range.getInput());
        if (mc.thePlayer == null || mc.theWorld == null) return;
        long now = System.currentTimeMillis();
        if (now < nextRefresh) return;
        nextRefresh = now + 400L;

        overlays.clear();
        double maxDistanceSq = range.getInput() * range.getInput();
        int limit = (int) maxMaterials.getInput();
        for (BedTracker.BedEntry bed : BedTracker.getBeds()) {
            if (mc.thePlayer.getDistanceSqToCenter(bed.centerBlock()) > maxDistanceSq) continue;
            List<BedTracker.ProtectionEntry> protection = BedTracker.getProtection(
                    bed, (int) protectionRadius.getInput());
            if (protection.size() > limit) {
                protection = new ArrayList<>(protection.subList(0, limit));
            }
            overlays.put(bed, protection);
        }
        setSuffix(Integer.toString(overlays.size()));
    }

    @SubscribeEvent
    public void onRender(RenderWorldEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        for (Map.Entry<BedTracker.BedEntry, List<BedTracker.ProtectionEntry>> entry : overlays.entrySet()) {
            WorldOverlayRenderer.drawBedPlate(
                    entry.getKey(), entry.getValue(),
                    0.022F * (float) scale.getInput());
        }
    }

    @Override
    public void onDisable() {
        overlays.clear();
        nextRefresh = 0L;
        setSuffix(null);
    }
}
