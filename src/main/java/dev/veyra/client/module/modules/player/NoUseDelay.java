package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

/** Clears only the one post-consumption right-click delay transition. */
public final class NoUseDelay extends Module {
    private boolean consuming;

    public NoUseDelay() {
        super("NoUseDelay", ModuleCategory.Player, 0);
        registerSetting(new DescriptionSetting(
                "Removes the client delay after food or potion consumption."));
    }

    @SubscribeEvent
    public void onTick(TickEvent.Post event) {
        if (!PlayerUtils.isPlayerInGame()) {
            consuming = false;
            return;
        }
        boolean consumingNow = mc.thePlayer.isUsingItem()
                && isConsumable(mc.thePlayer.getItemInUse());
        if (consuming && !consumingNow) {
            mc.rightClickDelayTimer = 0;
        }
        consuming = consumingNow;
    }

    private static boolean isConsumable(ItemStack stack) {
        return stack != null && (stack.getItem() instanceof ItemFood
                || stack.getItem() instanceof ItemPotion);
    }

    @Override
    public void onEnable() {
        consuming = false;
    }

    @Override
    public void onDisable() {
        consuming = false;
    }
}
