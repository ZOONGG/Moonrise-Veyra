package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.coordination.packet.PacketQueueManager;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.network.Packet;
import net.weavemc.api.event.*;

import java.util.Locale;

public class Blink extends Module {
    private static final String PACKET_OWNER = "blink";
    private static final int PACKET_PRIORITY = 100;

    public static TickSetting inbound, outbound, spawnFake, pulse;
    public static SliderSetting pulseDelay, maximumDuration;

    private EntityOtherPlayerMP fakePlayer;
    private long enabledAt;
    private long segmentStartedAt;

    public Blink() {
        super("Blink", ModuleCategory.Player, 0);
        registerSetting(new DescriptionSetting("Queues packets and releases them together."));
        registerSetting(inbound = new TickSetting("Block Inbound", false));
        registerSetting(outbound = new TickSetting("Block Outbound", true));
        registerSetting(spawnFake = new TickSetting("Spawn fake player", true));
        registerSetting(pulse = new TickSetting("Pulse", false));
        registerSetting(pulseDelay = new SliderSetting("Pulse MS", 500, 50, 5000, 50));
        registerSetting(maximumDuration = new SliderSetting("Maximum seconds", 10, 0, 60, 1));
    }

    @SubscribeEvent
    public void onOutgoing(PacketEvent.Send event) {
        if (PacketQueueManager.isReplaying() || !outbound.isToggled()
                || !PacketQueueManager.isActive(PACKET_OWNER, true)) return;
        Packet<?> packet = event.getPacket();
        if (!packet.getClass().getCanonicalName().startsWith("net.minecraft.network.play.client")) return;
        boolean captured = PacketQueueManager.capture(PACKET_OWNER, packet, true);
        if (!captured) {
            PacketQueueManager.flush(PACKET_OWNER);
            captured = PacketQueueManager.capture(PACKET_OWNER, packet, true);
        }
        if (captured) event.setCancelled(true);
    }

    @SubscribeEvent
    public void onIncoming(PacketEvent.Receive event) {
        if (PacketQueueManager.isReplaying() || !inbound.isToggled()
                || !PacketQueueManager.isActive(PACKET_OWNER, false)) return;
        boolean captured = PacketQueueManager.capture(PACKET_OWNER, event.getPacket(), false);
        if (!captured) {
            PacketQueueManager.flush(PACKET_OWNER);
            captured = PacketQueueManager.capture(PACKET_OWNER, event.getPacket(), false);
        }
        if (captured) event.setCancelled(true);
    }

    @Override
    public void onEnable() {
        PacketQueueManager.release(PACKET_OWNER, false);
        syncPacketDirections();
        enabledAt = segmentStartedAt = System.currentTimeMillis();
        createFakePlayer();
    }

    @Override
    public void onDisable() {
        PacketQueueManager.release(PACKET_OWNER, true);
        removeFakePlayer();
        setSuffix(null);
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            disable();
            return;
        }

        long now = System.currentTimeMillis();
        syncPacketDirections();
        double elapsedSeconds = (now - enabledAt) / 1000.0D;
        setSuffix(String.format(Locale.ROOT, "%.1fs · %d", elapsedSeconds,
                PacketQueueManager.size(PACKET_OWNER)));

        if (pulse.isToggled() && now - segmentStartedAt >= (long) pulseDelay.getInput()) {
            PacketQueueManager.flush(PACKET_OWNER);
            segmentStartedAt = now;
        }

        if (maximumDuration.getInput() > 0.0D && elapsedSeconds >= maximumDuration.getInput()) {
            disable();
        }
    }

    @SubscribeEvent
    public void onShutdown(ShutdownEvent event) {
        disable();
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent event) {
        PacketQueueManager.release(PACKET_OWNER, false);
        disable();
    }

    @SubscribeEvent
    public void onStart(StartGameEvent event) {
        PacketQueueManager.release(PACKET_OWNER, false);
        disable();
    }

    private void createFakePlayer() {
        if (!spawnFake.isToggled() || mc.thePlayer == null || mc.theWorld == null) return;
        fakePlayer = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        fakePlayer.copyLocationAndAnglesFrom(mc.thePlayer);
        fakePlayer.rotationYawHead = mc.thePlayer.rotationYawHead;
        mc.theWorld.addEntityToWorld(-13370, fakePlayer);
    }

    private void syncPacketDirections() {
        syncPacketDirection(outbound.isToggled(), true);
        syncPacketDirection(inbound.isToggled(), false);
    }

    private void syncPacketDirection(boolean enabled, boolean outgoingDirection) {
        if (enabled && !PacketQueueManager.hasOwner(PACKET_OWNER, outgoingDirection)) {
            PacketQueueManager.acquire(PACKET_OWNER, PACKET_PRIORITY,
                    outgoingDirection, !outgoingDirection);
        } else if (!enabled && PacketQueueManager.hasOwner(PACKET_OWNER, outgoingDirection)) {
            PacketQueueManager.releaseDirection(PACKET_OWNER, outgoingDirection, true);
        }
    }

    private void removeFakePlayer() {
        if (fakePlayer != null && mc.theWorld != null) {
            mc.theWorld.removeEntityFromWorld(-13370);
        }
        fakePlayer = null;
    }
}
