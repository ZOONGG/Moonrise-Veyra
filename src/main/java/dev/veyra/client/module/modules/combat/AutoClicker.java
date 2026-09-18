package dev.veyra.client.module.modules.combat;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.DoubleSliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.game.MouseManager;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.weavemc.api.event.*;
import org.lwjgl.input.Mouse;

import java.util.concurrent.ThreadLocalRandom;


@SuppressWarnings("unused")
public class AutoClicker extends Module {
    public static TickSetting breakBlocks;
    public static DoubleSliderSetting leftCPS;
    private long lastClickTime;
    private long nextClickDelay = 100L;
    private final int lmb = mc.gameSettings.keyBindAttack.getKeyCode();

    public AutoClicker() {
        super("AutoClicker", ModuleCategory.Combat, 0);
        this.registerSetting(new DescriptionSetting("Click automatically"));
        this.registerSetting(leftCPS = new DoubleSliderSetting("CPS", 8.0D, 12.0D, 1.0D, 20.0D, 1.0D));
        this.registerSetting(breakBlocks = new TickSetting("Break blocks", false));
    }

    public boolean breakBlock() {
        if (breakBlocks.isToggled() && mc.objectMouseOver != null) {
            BlockPos p = mc.objectMouseOver.getBlockPos();

            if (p != null) {
                if (mc.theWorld.getBlockState(p).getBlock() != Blocks.air && !(mc.theWorld.getBlockState(p).getBlock() instanceof BlockLiquid)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onRender(RenderHandEvent e) {
        if (!PlayerUtils.isPlayerInGame() || mc.currentScreen != null || !Mouse.isButtonDown(0)) return;
        if (breakBlock()) return;

        long now = System.currentTimeMillis();
        if (now - lastClickTime < nextClickDelay) return;

        KeyBinding.onTick(lmb);
        MouseManager.addLeftClick();
        lastClickTime = now;

        double minCps = leftCPS.getInputMin();
        double maxCps = leftCPS.getInputMax();
        double cps = minCps == maxCps
                ? minCps
                : ThreadLocalRandom.current().nextDouble(minCps, Math.nextUp(maxCps));
        nextClickDelay = Math.max(25L, Math.round(1000.0D / Math.max(1.0D, cps)));
    }
}
