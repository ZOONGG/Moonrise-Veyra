package dev.veyra.client.module.modules.movement;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

@SuppressWarnings("unused")
public class Bhop extends Module {

    private final SliderSetting Speed, gs, as;
    public final TickSetting hitRec;
    public ComboSetting<mode> SpeedMode;
    public boolean incomingVelo = false;
    private int velocityTicks;

    public Bhop() {
        super("Bhop", ModuleCategory.Movement, 0);
        this.registerSetting(new DescriptionSetting("Bunny Hop"));
        this.registerSetting(Speed = new SliderSetting("Speed", 1.0D, 0.5D, 4.0D, 0.01D));
        this.registerSetting(gs = new SliderSetting("Ground Speed (New)", 1.0D, 1.D, 2.0D, 0.01D));
        this.registerSetting(as = new SliderSetting("Air Speed (New)", 1.0D, 1.0D, 5.0D, 0.01D));
        this.registerSetting(hitRec = new TickSetting("Slow on hit recieve (New)", true));
        this.registerSetting(SpeedMode = new ComboSetting<>("Mode", mode.New));
    }

    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.speedInAir = 0.02F;
        }
    }

    @SubscribeEvent
    public void heck(PacketEvent.Receive e) {
        if (PlayerUtils.isPlayerInGame() && e.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) e.getPacket();
            if (packet.getEntityID() == mc.thePlayer.getEntityId()) velocityTicks = 3;
        }
    }

    @SubscribeEvent
    public void onMovementTick(TickEvent e) {
        incomingVelo = velocityTicks > 0;
        if (velocityTicks > 0) velocityTicks--;
        switch (SpeedMode.getMode()) {
            case Old:
                if (PlayerUtils.isPlayerInGame()) {
                    onDisable();
                    if (mc.thePlayer.onGround && !mc.thePlayer.isSneaking() && mc.gameSettings.keyBindForward.isKeyDown()) {
                        if (mc.thePlayer.motionX != 0 || mc.thePlayer.motionZ != 0) {
                            mc.thePlayer.jump();
                            mc.thePlayer.motionX *= (float) Speed.getInput() / 2;
                            mc.thePlayer.motionZ *= (float) Speed.getInput() / 2;
                        }
                    }
                    break;
                }
            case New:
                boolean devZoomin = false;
                devZoomin = mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown() ||mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown();
                if (PlayerUtils.isPlayerInGame()) {
                    if (mc.thePlayer.onGround && !mc.thePlayer.isSneaking() && devZoomin && !mc.thePlayer.inWater) {
                        if (mc.thePlayer.motionX != 0 || mc.thePlayer.motionZ != 0) {
                            if (incomingVelo && hitRec.isToggled()) {
                                mc.thePlayer.motionX *= .5;
                                mc.thePlayer.motionZ *= .5;
                            } else {
                                mc.thePlayer.motionX *= (float) Speed.getInput() / 2;
                                mc.thePlayer.motionZ *= (float) Speed.getInput() / 2;
                                mc.thePlayer.movementInput.moveForward *= (float) gs.getInput();
                                mc.thePlayer.movementInput.moveStrafe *= (float) gs.getInput();
                                mc.thePlayer.jumpTicks = 0;
                                mc.thePlayer.speedInAir = (float) as.getInput() * 2 / 100;
                                mc.thePlayer.jump();
                            }
                        }
                    }
                    break;
                }
        }
    }

    public enum mode {
        Old, New
    }
}
