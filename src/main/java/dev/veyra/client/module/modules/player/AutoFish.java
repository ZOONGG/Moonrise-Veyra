package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

public class AutoFish extends Module {
    public static SliderSetting recastDelay;
    private long nextActionAt;
    private boolean waitingToRecast;

    public AutoFish() {
        super("AutoFish", ModuleCategory.Player, 0);
        registerSetting(new DescriptionSetting("Retracts on a bite and casts again."));
        registerSetting(recastDelay = new SliderSetting("Recast delay ms", 500, 200, 1500, 50));
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || held.getItem() != Items.fishing_rod) {
            waitingToRecast = false;
            setSuffix("rod");
            return;
        }

        long now = System.currentTimeMillis();
        if (waitingToRecast) {
            long remaining = nextActionAt - now;
            setSuffix(Math.max(0L, remaining) + "ms");
            if (now >= nextActionAt && mc.thePlayer.fishEntity == null) {
                useRod(held);
                waitingToRecast = false;
                setSuffix("cast");
            }
            return;
        }

        if (mc.thePlayer.fishEntity != null && mc.thePlayer.fishEntity.ticksCatchable > 0) {
            useRod(held);
            waitingToRecast = true;
            nextActionAt = now + (long) recastDelay.getInput();
            setSuffix("caught");
        } else {
            setSuffix(mc.thePlayer.fishEntity == null ? "ready" : "waiting");
        }
    }

    @Override
    public void onDisable() {
        waitingToRecast = false;
        setSuffix(null);
    }

    private void useRod(ItemStack stack) {
        if (mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, stack)) {
            mc.getItemRenderer().resetEquippedProgress2();
        }
    }
}
