package dev.veyra.client.utils.packet;

import net.minecraft.network.Packet;
import net.minecraft.network.ThreadQuickExitException;

import static dev.veyra.client.utils.Utils.mc;

@SuppressWarnings("unused")
public class PacketUtils {
    public static void Send(Packet packet) {
        mc.getNetHandler().addToSendQueue(packet);
    }
    public static void handle(Packet packet, boolean outgoing) {
        if (mc.getNetHandler() == null) return;
        if (outgoing) {
            mc.getNetHandler().netManager.sendPacket(packet);
        } else {
            if (mc.getNetHandler().netManager.channel.isOpen()) {
                try {
                    packet.processPacket(mc.getNetHandler().netManager.packetListener);
                }
                catch (final ThreadQuickExitException ignored) {}
            }
        }
    }
}
