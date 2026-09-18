package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.SubscribeEvent;

public class NoRotate extends Module {
    public NoRotate() {
        super("NoRotate", ModuleCategory.Player, 0);
    }
    @SubscribeEvent
    public void onPacket(PacketEvent.Receive event) {
        if (mc.thePlayer == null) return;
        final Packet<?> packet = event.getPacket();
        if (packet instanceof S08PacketPlayerPosLook s08) {
            s08.yaw = mc.thePlayer.rotationYaw;
            s08.pitch = mc.thePlayer.rotationPitch;
        }
    }
}
