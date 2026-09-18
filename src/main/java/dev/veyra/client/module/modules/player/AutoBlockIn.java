package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.world.BlockInteractionUtils;
import net.minecraft.util.BlockPos;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

import java.util.ArrayList;
import java.util.List;

public class AutoBlockIn extends Module {
    public static SliderSetting placeDelay;
    public static TickSetting cap;
    private final List<BlockPos> targets = new ArrayList<>();
    private BlockPos origin;
    private long lastPlace;
    private int failedPasses;

    public AutoBlockIn() {
        super("AutoBlock In", ModuleCategory.Player, 0);
        registerSetting(new DescriptionSetting("Builds a compact shell around your current position."));
        registerSetting(placeDelay = new SliderSetting("Place delay ms", 75, 25, 250, 25));
        registerSetting(cap = new TickSetting("Roof", true));
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            disable();
            return;
        }
        origin = mc.thePlayer.getPosition();
        targets.clear();
        addRing(origin);
        addRing(origin.up());
        if (cap.isToggled()) targets.add(origin.up(2));
        lastPlace = 0L;
        failedPasses = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        if (mc.thePlayer == null || mc.theWorld == null || origin == null) {
            disable();
            return;
        }
        if (mc.thePlayer.getDistanceSq(origin) > 2.25D) {
            disable();
            return;
        }

        targets.removeIf(pos -> !BlockInteractionUtils.isReplaceable(pos));
        setSuffix(Integer.toString(targets.size()));
        if (targets.isEmpty()) {
            disable();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPlace < (long) placeDelay.getInput()) return;

        boolean placed = false;
        for (int i = 0; i < targets.size(); i++) {
            BlockPos pos = targets.get(i);
            if (BlockInteractionUtils.placeBlock(pos, false)) {
                targets.remove(i);
                placed = true;
                lastPlace = now;
                failedPasses = 0;
                break;
            }
        }
        if (!placed && ++failedPasses > 20) disable();
    }

    @Override
    public void onDisable() {
        targets.clear();
        origin = null;
        setSuffix(null);
    }

    private void addRing(BlockPos center) {
        targets.add(center.north());
        targets.add(center.south());
        targets.add(center.west());
        targets.add(center.east());
    }
}
