package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.coordination.packet.PacketQueueManager;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.network.Packet;
import net.weavemc.api.event.*;

@SuppressWarnings("unused")
public class FakeLag extends Module {
    private static final String PACKET_OWNER = "fake-lag";
    private static final int PACKET_PRIORITY = 50;
    public static SliderSetting spoofms;

    public FakeLag() {
        super("FakeLag", ModuleCategory.Player, 0);
        this.registerSetting(new DescriptionSetting(
                "Adds bounded incoming and outgoing packet delay without sharing other queues."));
        this.registerSetting(spoofms = new SliderSetting("Ping in ms", 80.0, 30.0, 1000.0, 5.0));
    }


    @SubscribeEvent
    public void onTickDisabler(TickEvent.Pre e) {
      if (!PlayerUtils.isPlayerInGame()) {
            PacketQueueManager.release(PACKET_OWNER, false);
            setSuffix(null);
      } else {
            PacketQueueManager.acquire(PACKET_OWNER, PACKET_PRIORITY, true, true);
      }
    }

    @SubscribeEvent
    public void pingSpooferOutgoing(PacketEvent.Send e) {
        if (!PlayerUtils.isPlayerInGame() || PacketQueueManager.isReplaying()
                || !PacketQueueManager.isActive(PACKET_OWNER, true)) return;
        Packet<?> packet = e.getPacket();
        boolean captured = PacketQueueManager.capture(PACKET_OWNER, packet, true);
        if (!captured) {
            PacketQueueManager.flush(PACKET_OWNER);
            captured = PacketQueueManager.capture(PACKET_OWNER, packet, true);
        }
        if (captured) e.setCancelled(true);
    }

    @SubscribeEvent
    public void pingSpooferIncoming(PacketEvent.Receive e) {
        if (!PlayerUtils.isPlayerInGame() || PacketQueueManager.isReplaying()
                || !PacketQueueManager.isActive(PACKET_OWNER, false)) return;
        Packet<?> packet = e.getPacket();
        boolean captured = PacketQueueManager.capture(PACKET_OWNER, packet, false);
        if (!captured) {
            PacketQueueManager.flush(PACKET_OWNER);
            captured = PacketQueueManager.capture(PACKET_OWNER, packet, false);
        }
        if (captured) e.setCancelled(true);
    }

    @SubscribeEvent
    public void packetHandler(TickEvent.Pre e) {
        if (!PlayerUtils.isPlayerInGame()) return;
        final long time = System.currentTimeMillis();
        PacketQueueManager.releaseDue(PACKET_OWNER, true, (long) spoofms.getInput(), time);
        PacketQueueManager.releaseDue(PACKET_OWNER, false, (long) spoofms.getInput(), time);
        setSuffix((int) spoofms.getInput() + "ms · " + PacketQueueManager.size(PACKET_OWNER));
    }

    @Override
    public void onEnable() {
        PacketQueueManager.release(PACKET_OWNER, false);
        PacketQueueManager.acquire(PACKET_OWNER, PACKET_PRIORITY, true, true);
        setSuffix((int) spoofms.getInput() + "ms");
    }

    @Override
    public void onDisable() {
        PacketQueueManager.release(PACKET_OWNER, true);
        setSuffix(null);
    }
}
