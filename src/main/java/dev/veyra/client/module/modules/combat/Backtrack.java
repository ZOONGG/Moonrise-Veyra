package dev.veyra.client.module.modules.combat;

import dev.veyra.client.module.Module;
import dev.veyra.client.coordination.packet.PacketQueueManager;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

import java.util.Comparator;

import dev.veyra.weave.mixins.IS14PacketEntity;

@SuppressWarnings("unused")
public class Backtrack extends Module {
    private static final int MAX_PACKETS = 512;
    private static final long ATTACK_TARGET_HOLD_MS = 1_500L;
    private static final String PACKET_OWNER = "backtrack";
    private static final int PACKET_PRIORITY = 30;

    public static SliderSetting spoofms, range;
    public static TickSetting useRange;
    private volatile int targetEntityId = -1;
    private volatile long activeDelayMs;
    private volatile boolean captureReady;
    private volatile int lastAttackEntityId = -1;
    private volatile long lastAttackAtMs;

    public Backtrack() {
        super("Backtrack", ModuleCategory.Combat, 0);
        registerSetting(new DescriptionSetting(
                "Range limits how close a target must be before its updates are delayed."));
        registerSetting(spoofms = new SliderSetting("Delay in ms", 50.0, 0.0, 500.0, 5.0));
        registerSetting(range = new SliderSetting("Range", 5.0, 0.0, 7.0, 0.1));
        registerSetting(useRange = new TickSetting("Use Range", true));
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        if (!PlayerUtils.isPlayerInGame()) {
            captureReady = false;
            PacketQueueManager.release(PACKET_OWNER, false);
            resetTargets();
            setSuffix(null);
            return;
        }
        PacketQueueManager.acquire(PACKET_OWNER, PACKET_PRIORITY, false, true);

        long now = System.currentTimeMillis();
        EntityPlayer nextTarget = selectTarget(now);
        captureReady = false;
        switchTarget(nextTarget);
        activeDelayMs = Math.max(0L, (long) spoofms.getInput());
        captureReady = nextTarget != null && activeDelayMs > 0L;

        if (!captureReady) {
            PacketQueueManager.flush(PACKET_OWNER);
            setSuffix(null);
            return;
        }

        setSuffix(activeDelayMs + "ms");
        PacketQueueManager.releaseDue(PACKET_OWNER, false, activeDelayMs, now);
    }

    @SubscribeEvent
    public void onAttack(PacketEvent.Send event) {
        if (!PlayerUtils.isPlayerInGame()
                || !(event.getPacket() instanceof C02PacketUseEntity attack)
                || attack.getAction() != C02PacketUseEntity.Action.ATTACK) {
            return;
        }
        Entity attacked = attack.getEntityFromWorld(mc.theWorld);
        if (attacked instanceof EntityPlayer player && isEligible(player)) {
            lastAttackEntityId = player.getEntityId();
            lastAttackAtMs = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onIncoming(PacketEvent.Receive event) {
        if (PacketQueueManager.isReplaying() || !captureReady
                || targetEntityId < 0
                || activeDelayMs <= 0L
                || !PacketQueueManager.isActive(PACKET_OWNER, false)) return;
        if (!isMovementForTarget(event.getPacket(), targetEntityId)) return;

        if (PacketQueueManager.size(PACKET_OWNER) >= MAX_PACKETS
                || !PacketQueueManager.capture(PACKET_OWNER, event.getPacket(), false)) {
            PacketQueueManager.flush(PACKET_OWNER);
            if (!PacketQueueManager.capture(PACKET_OWNER, event.getPacket(), false)) return;
        }
        event.setCancelled(true);
    }

    @Override
    public void onEnable() {
        PacketQueueManager.release(PACKET_OWNER, false);
        PacketQueueManager.acquire(PACKET_OWNER, PACKET_PRIORITY, false, true);
        resetTargets();
    }

    @Override
    public void onDisable() {
        PacketQueueManager.release(PACKET_OWNER, true);
        resetTargets();
        setSuffix(null);
    }

    private EntityPlayer selectTarget(long nowMs) {
        if (lastAttackEntityId >= 0 && nowMs - lastAttackAtMs <= ATTACK_TARGET_HOLD_MS) {
            Entity attacked = mc.theWorld.getEntityByID(lastAttackEntityId);
            if (attacked instanceof EntityPlayer player && isEligible(player)) return player;
        } else {
            lastAttackEntityId = -1;
        }

        if (mc.objectMouseOver != null
                && mc.objectMouseOver.entityHit instanceof EntityPlayer crosshairTarget
                && isEligible(crosshairTarget)) {
            return crosshairTarget;
        }

        return mc.theWorld.playerEntities.stream()
                .filter(this::isEligible)
                .min(Comparator.comparingDouble(player -> player.getDistanceToEntity(mc.thePlayer)))
                .orElse(null);
    }

    private boolean isEligible(EntityPlayer player) {
        return TargetFilter.isValidPlayer(player)
                && (!useRange.isToggled()
                || player.getDistanceToEntity(mc.thePlayer) <= range.getInput());
    }

    private void switchTarget(EntityPlayer nextTarget) {
        int nextEntityId = nextTarget == null ? -1 : nextTarget.getEntityId();
        if (nextEntityId == targetEntityId) return;
        PacketQueueManager.flush(PACKET_OWNER);
        targetEntityId = nextEntityId;
    }

    private void resetTargets() {
        targetEntityId = -1;
        activeDelayMs = 0L;
        captureReady = false;
        lastAttackEntityId = -1;
        lastAttackAtMs = 0L;
    }

    private boolean isMovementForTarget(Packet<?> packet, int currentTargetEntityId) {
        if (packet instanceof S14PacketEntity relativeMove) {
            return ((IS14PacketEntity) relativeMove).veyra$getEntityId()
                    == currentTargetEntityId;
        }
        return packet instanceof S18PacketEntityTeleport teleport
                && teleport.getEntityId() == currentTargetEntityId;
    }

}
