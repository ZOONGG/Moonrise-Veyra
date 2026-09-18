package dev.veyra.client.module.modules.movement;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.math.MathUtils;
import dev.veyra.client.utils.player.MoveUtils;
import dev.veyra.client.utils.player.PlayerUtils;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;


@SuppressWarnings("unused")
public class Flight extends Module {
    public ComboSetting<modeee> flightMode;
    private boolean started;
    public final SliderSetting airspeed;
    public final TickSetting gs, ascend, descend;

    public Flight() {
        super("Flight", ModuleCategory.Movement, 0);
        this.registerSetting(new DescriptionSetting("Configurable aerial movement."));
        this.registerSetting(airspeed = new SliderSetting("Airspeed", 1, 0, 10, 0.5));
        this.registerSetting(flightMode = new ComboSetting<>("Mode", modeee.AirWalk));
        this.registerSetting(gs = new TickSetting("GroundSpoof", false));
        this.registerSetting(ascend = new TickSetting("Ascend", false));
        this.registerSetting(descend = new TickSetting("Descend", false));
    }

    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.speedInAir = 0.02F;
        }
        mc.timer.timerSpeed = 1.0F;
    }

    @SubscribeEvent
    public void airTrafficControl(TickEvent e) {
        if (!PlayerUtils.isPlayerInGame()) return;
        if (gs.isToggled()) mc.thePlayer.onGround = true;
        switch (flightMode.getMode()) {
            case AirWalk -> {
                mc.thePlayer.motionY = 0.0F;
                mc.thePlayer.speedInAir = (float) airspeed.getInput() * 2 / 100;
                if (ascend.isToggled() && mc.gameSettings.keyBindJump.isKeyDown()) {
                    mc.thePlayer.jumpTicks = 0;
                    mc.thePlayer.motionY = 0.42F;
                }
                if (descend.isToggled() && mc.gameSettings.keyBindSneak.isKeyDown()) {
                    mc.thePlayer.motionY = -0.5F;
                }
            }
            case VerusGlide -> {
                mc.timer.timerSpeed = 1.0F;
                if (mc.thePlayer.fallDistance > 0.5F) {
                    mc.thePlayer.motionY = -0.09800000190735147;
                    MoveUtils.strafe();
                    if (mc.thePlayer.fallDistance < 3.0F) {
                        MoveUtils.motionMult(1.03F);
                    }
                    if (mc.thePlayer.fallDistance > 3.0F) {
                        MoveUtils.motionMult(1.0105F);
                    };
                    mc.thePlayer.speedInAir = (float) (MathUtils.randomInt(1, 1.4) * 2) / 100;
                    if (ascend.isToggled() && mc.gameSettings.keyBindJump.isKeyDown()) {
                        mc.thePlayer.motionY += 1;
                    }
                    if (descend.isToggled() && mc.gameSettings.keyBindSneak.isKeyDown()) {
                        mc.thePlayer.motionY -= 0.5F;
                    }
                }
            }
        }
    }

    public enum modeee {
        AirWalk, VerusGlide
    }
}
