package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.player.place.FastPlaceContext;
import dev.veyra.client.player.place.FastPlaceEngine;
import dev.veyra.client.player.place.FastPlaceSettings;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

public final class FastPlace extends Module {
    private final SliderSetting delayTicks;
    private final TickSetting blocksOnly;
    private final FastPlaceEngine engine = new FastPlaceEngine();

    public FastPlace() {
        super("FastPlace", ModuleCategory.Player, 0);
        registerSetting(delayTicks = new SliderSetting(
                "Delay", 0.0D, 0.0D, 4.0D, 1.0D));
        registerSetting(blocksOnly = new TickSetting("Blocks only", true));
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.Post event) {
        boolean inGame = PlayerUtils.isPlayerInGame() && mc.inGameHasFocus
                && mc.currentScreen == null;
        ItemStack held = inGame ? mc.thePlayer.getHeldItem() : null;
        boolean eligibleItem = !blocksOnly.isToggled()
                || (held != null && held.getItem() instanceof ItemBlock);
        int cap = engine.timerCapTicks(new FastPlaceContext(inGame && eligibleItem),
                new FastPlaceSettings((int) delayTicks.getInput()));

        if (cap != FastPlaceEngine.INACTIVE && mc.rightClickDelayTimer > cap) {
            mc.rightClickDelayTimer = cap;
        }
    }

}
