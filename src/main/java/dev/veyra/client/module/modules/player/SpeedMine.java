package dev.veyra.client.module.modules.player;

import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;

/** Scales only local block-breaking progress, leaving the game clock untouched. */
public final class SpeedMine extends Module {
    private final SliderSetting speed;

    public SpeedMine() {
        super("SpeedMine", ModuleCategory.Player, 0);
        registerSetting(new DescriptionSetting("Multiplies block-breaking speed only. 1.00 is vanilla; 2.50 is maximum."));
        registerSetting(speed = new SliderSetting("Speed", 1.0D, 1.0D, 2.5D, 0.01D));
    }

    public static float scaleProgress(float vanilla) {
        if (Veyra.moduleManager == null || mc.thePlayer == null || mc.theWorld == null
                || mc.thePlayer.isDead || mc.thePlayer.isSpectator()) return vanilla;
        SpeedMine module = (SpeedMine) Veyra.moduleManager.getModuleByClazz(SpeedMine.class);
        // A multiplier must never make an unbreakable block breakable.
        if (module == null || !module.isEnabled() || vanilla <= 0.0F) return vanilla;
        return vanilla * (float) module.speed.getInput();
    }
}
