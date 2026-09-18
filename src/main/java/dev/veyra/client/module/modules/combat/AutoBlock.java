package dev.veyra.client.module.modules.combat;

import dev.veyra.client.combat.block.AutoBlockAction;
import dev.veyra.client.combat.block.AutoBlockContext;
import dev.veyra.client.combat.block.AutoBlockEngine;
import dev.veyra.client.combat.block.AutoBlockMode;
import dev.veyra.client.combat.block.AutoBlockSettings;
import dev.veyra.client.combat.block.AutoBlockStep;
import dev.veyra.client.coordination.input.InputLeaseManager;
import dev.veyra.client.coordination.packet.PacketQueueManager;
import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.DoubleSliderSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.ShutdownEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;
import net.weavemc.api.event.WorldEvent;
import org.lwjgl.input.Mouse;

import java.util.concurrent.ThreadLocalRandom;

/** Three-mode sword AutoBlock with input and packet ownership boundaries. */
@SuppressWarnings("unused")
public final class AutoBlock extends Module {
    private static final int MAX_LAG_PACKETS = 256;
    private static final long RECENT_DAMAGE_MS = 1_000L;
    private static final String INPUT_OWNER = "autoblock";
    private static final String PACKET_OWNER = "autoblock";
    private static final int INPUT_PRIORITY = 60;
    private static final int PACKET_PRIORITY = 25;

    private final ComboSetting<Mode> mode;
    private final SliderSetting range;
    private final SliderSetting chance;
    private final SliderSetting maximumHurtTimeMs;
    private final DoubleSliderSetting startDelayMs;
    private final DoubleSliderSetting maximumHoldMs;
    private final DoubleSliderSetting cooldownMs;
    private final TickSetting requireLeftMouse;
    private final TickSetting requireRightMouse;
    private final TickSetting requireRecentDamage;
    private final TickSetting allowFirstHit;
    private final TickSetting allowDuringCombos;
    private final SliderSetting lagChance;
    private final DoubleSliderSetting lagDurationMs;
    private final TickSetting preventDelayingAttacks;
    private final TickSetting blockAgainImmediately;
    private final TickSetting flushBeforeAttack;
    private final TickSetting forceBlockAnimation;

    private final AutoBlockEngine engine = new AutoBlockEngine();
    private boolean injectedBlock;
    private volatile boolean ownedLag;
    private long lastDamagedAtMs = Long.MIN_VALUE;
    private int previousHurtTime;
    private Mode previousMode;

    public AutoBlock() {
        super("AutoBlock", ModuleCategory.Combat, 0);
        registerSetting(new DescriptionSetting("Blocks predicted hits; Blatant can preserve server blocking with short lag."));
        registerSetting(mode = new ComboSetting<>("Mode", Mode.BlockHit));
        registerSetting(range = new SliderSetting("Range", 3.4D, 2.0D, 5.0D, 0.1D));
        registerSetting(chance = new SliderSetting("Chance", 85.0D, 0.0D, 100.0D, 1.0D));
        registerSetting(maximumHurtTimeMs = new SliderSetting("Maximum hurt time ms", 200.0D, 0.0D, 500.0D, 10.0D));
        registerSetting(startDelayMs = new DoubleSliderSetting("Start delay ms", 0.0D, 50.0D, 0.0D, 250.0D, 10.0D));
        registerSetting(maximumHoldMs = new DoubleSliderSetting("Maximum hold ms", 75.0D, 125.0D, 25.0D, 500.0D, 5.0D));
        registerSetting(cooldownMs = new DoubleSliderSetting("Cooldown ms", 200.0D, 350.0D, 0.0D, 1000.0D, 25.0D));
        registerSetting(requireLeftMouse = new TickSetting("Left mouse pressed", false));
        registerSetting(requireRightMouse = new TickSetting("Right mouse pressed", false));
        registerSetting(requireRecentDamage = new TickSetting("Recently damaged", false));
        registerSetting(forceBlockAnimation = new TickSetting("Force block animation", true));
        registerSetting(allowFirstHit = new TickSetting("Allow first hit", true));
        registerSetting(allowDuringCombos = new TickSetting("Allow during combos", true));
        registerSetting(lagChance = new SliderSetting("Lag chance", 0.0D, 0.0D, 100.0D, 1.0D));
        registerSetting(lagDurationMs = new DoubleSliderSetting("Lag duration ms", 150.0D, 200.0D, 25.0D, 500.0D, 5.0D));
        registerSetting(preventDelayingAttacks = new TickSetting("Prevent delaying attacks", true));
        registerSetting(blockAgainImmediately = new TickSetting("Block again immediately", false));
        registerSetting(flushBeforeAttack = new TickSetting("Flush before attack", true));
    }

    @SubscribeEvent
    public void onOutgoing(PacketEvent.Send event) {
        if (PacketQueueManager.isReplaying()) return;
        if (ownedLag && !packetLagAvailable()) {
            stopOwnedLag(true);
            engine.reset();
        }

        Packet<?> packet = event.getPacket();
        EntityPlayer attackedTarget = attackedPlayer(packet);
        if (isAttackPacket(packet)) {
            apply(engine.onAcceptedAttack(
                    context(attackedTarget != null && isThreatTarget(attackedTarget)
                            ? attackedTarget : null), settings()));
        }

        if (ownedLag && shouldLag(packet)) {
            if (PacketQueueManager.size(PACKET_OWNER) >= MAX_LAG_PACKETS
                    || !PacketQueueManager.capture(PACKET_OWNER, packet, true)) {
                stopOwnedLag(true);
                engine.reset();
                return;
            }
            event.setCancelled(true);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.Pre event) {
        updateDamageHistory();
        if (previousMode != null && previousMode != mode.getMode()) {
            releaseEverything(true);
        }
        previousMode = mode.getMode();

        EntityPlayer threat = findNearestThreat();
        AutoBlockContext context = context(threat);
        apply(engine.tick(context, settings()));
        setSuffix(mode.getMode() + (ownedLag ? " · Lag" : ""));
    }

    private void apply(AutoBlockStep step) {
        for (AutoBlockAction action : step.actions()) {
            switch (action) {
                case START_BLOCK -> {
                    if (!beginInjectedBlock()) engine.reset();
                }
                case STOP_BLOCK -> releaseInjectedBlock();
                case START_LAG -> {
                    if (!beginOwnedLag()) engine.reset();
                }
                case STOP_LAG -> stopOwnedLag(true);
            }
        }
    }

    private boolean beginInjectedBlock() {
        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        if (!canInjectBlock() || InputLeaseManager.physicalState(useKey)) {
            return false;
        }
        InputLeaseManager.set(INPUT_OWNER, useKey, true, INPUT_PRIORITY);
        if (forceBlockAnimation.isToggled() && mc.thePlayer.getHeldItem() != null) {
            mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(),
                    mc.thePlayer.getHeldItem().getMaxItemUseDuration());
        }
        injectedBlock = true;
        return true;
    }

    private void releaseInjectedBlock() {
        if (!injectedBlock) return;
        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        if (PlayerUtils.isPlayerInGame() && mc.thePlayer.isUsingItem()) {
            mc.playerController.onStoppedUsingItem(mc.thePlayer);
        }
        InputLeaseManager.release(INPUT_OWNER, useKey);
        injectedBlock = false;
    }

    private boolean beginOwnedLag() {
        if (!packetLagAvailable()) return false;
        PacketQueueManager.acquire(PACKET_OWNER, PACKET_PRIORITY, true, false);
        if (!PacketQueueManager.isActive(PACKET_OWNER, true)) {
            PacketQueueManager.release(PACKET_OWNER, false);
            return false;
        }
        ownedLag = true;
        return true;
    }

    private void stopOwnedLag(boolean flush) {
        if (!ownedLag && PacketQueueManager.size(PACKET_OWNER) == 0) return;
        ownedLag = false;
        PacketQueueManager.release(PACKET_OWNER, flush && PlayerUtils.isPlayerInGame());
    }

    private AutoBlockContext context(EntityPlayer threat) {
        long nowMs = System.currentTimeMillis();
        boolean physicalUse = InputLeaseManager.physicalState(
                mc.gameSettings.keyBindUseItem.getKeyCode());
        return new AutoBlockContext(nowMs,
                basicContextValid(),
                canInjectBlock(),
                threat != null,
                physicalUse,
                Mouse.isButtonDown(0),
                Mouse.isButtonDown(1),
                elapsedSince(lastDamagedAtMs, nowMs) <= RECENT_DAMAGE_MS,
                packetLagAvailable(),
                ThreadLocalRandom.current().nextInt(100),
                ThreadLocalRandom.current().nextInt(100));
    }

    private AutoBlockSettings settings() {
        AutoBlockMode selected = switch (mode.getMode()) {
            case Predictive -> AutoBlockMode.PREDICTIVE;
            case Blatant -> AutoBlockMode.BLATANT;
            case BlockHit -> AutoBlockMode.BLOCK_HIT;
        };
        return new AutoBlockSettings(selected,
                randomInclusive(startDelayMs),
                randomInclusive(maximumHoldMs),
                randomInclusive(cooldownMs),
                (int) Math.round(chance.getInput()),
                requireLeftMouse.isToggled(),
                requireRightMouse.isToggled(),
                requireRecentDamage.isToggled(),
                selected == AutoBlockMode.BLATANT ? (int) Math.round(lagChance.getInput()) : 0,
                randomInclusive(lagDurationMs),
                preventDelayingAttacks.isToggled(),
                blockAgainImmediately.isToggled(),
                flushBeforeAttack.isToggled());
    }

    private EntityPlayer findNearestThreat() {
        if (!PlayerUtils.isPlayerInGame()) return null;
        EntityPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isThreatTarget(player)) continue;
            double distance = mc.thePlayer.getDistanceToEntity(player);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean isThreatTarget(EntityPlayer target) {
        if (!PlayerUtils.isPlayerInGame()
                || target == null
                || !TargetFilter.isValidPlayer(target)
                || mc.thePlayer.getDistanceToEntity(target) > range.getInput()) {
            return false;
        }
        if (!allowFirstHit.isToggled()
                && elapsedSince(lastDamagedAtMs, System.currentTimeMillis()) > RECENT_DAMAGE_MS) {
            return false;
        }
        if (!allowDuringCombos.isToggled() && target.hurtTime > 1) return false;
        int remainingHurtTimeMs = Math.max(0, mc.thePlayer.hurtTime) * 50;
        return mc.thePlayer.hurtTime == 0
                || remainingHurtTimeMs <= maximumHurtTimeMs.getInput();
    }

    private EntityPlayer attackedPlayer(Packet<?> packet) {
        if (!(packet instanceof C02PacketUseEntity attack) || mc.theWorld == null) {
            return null;
        }
        Entity entity = attack.getEntityFromWorld(mc.theWorld);
        return entity instanceof EntityPlayer player ? player : null;
    }

    private static boolean isAttackPacket(Packet<?> packet) {
        return packet instanceof C02PacketUseEntity attack
                && attack.getAction() == C02PacketUseEntity.Action.ATTACK;
    }

    private boolean basicContextValid() {
        return PlayerUtils.isPlayerInGame()
                && mc.currentScreen == null
                && (!mc.thePlayer.isUsingItem() || injectedBlock);
    }

    private boolean canInjectBlock() {
        return basicContextValid()
                && mc.thePlayer.getCurrentEquippedItem() != null
                && mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemSword;
    }

    private boolean packetLagAvailable() {
        return ownedLag
                ? PacketQueueManager.isActive(PACKET_OWNER, true)
                : PacketQueueManager.canAcquire(
                        PACKET_OWNER, PACKET_PRIORITY, true);
    }

    private static boolean shouldLag(Packet<?> packet) {
        return packet instanceof C03PacketPlayer
                || packet instanceof C07PacketPlayerDigging
                || packet instanceof C08PacketPlayerBlockPlacement
                || packet instanceof C02PacketUseEntity
                || packet instanceof C0APacketAnimation
                || packet instanceof C0BPacketEntityAction;
    }

    private void updateDamageHistory() {
        if (!PlayerUtils.isPlayerInGame()) {
            previousHurtTime = 0;
            return;
        }
        if (mc.thePlayer.hurtTime > previousHurtTime) {
            lastDamagedAtMs = System.currentTimeMillis();
        }
        previousHurtTime = mc.thePlayer.hurtTime;
    }

    private void releaseEverything(boolean flushLag) {
        releaseInjectedBlock();
        stopOwnedLag(flushLag);
        engine.reset();
    }

    private static long randomInclusive(DoubleSliderSetting setting) {
        long minimum = Math.round(Math.min(setting.getInputMin(), setting.getInputMax()));
        long maximum = Math.round(Math.max(setting.getInputMin(), setting.getInputMax()));
        return minimum == maximum
                ? minimum
                : ThreadLocalRandom.current().nextLong(minimum, maximum + 1L);
    }

    private static long elapsedSince(long timestamp, long nowMs) {
        return timestamp == Long.MIN_VALUE ? Long.MAX_VALUE : Math.max(0L, nowMs - timestamp);
    }

    @SubscribeEvent
    public void onWorld(WorldEvent event) {
        releaseEverything(false);
        PacketQueueManager.discard(PACKET_OWNER);
        lastDamagedAtMs = Long.MIN_VALUE;
        previousHurtTime = 0;
    }

    @SubscribeEvent
    public void onShutdown(ShutdownEvent event) {
        releaseEverything(false);
        PacketQueueManager.discard(PACKET_OWNER);
    }

    @Override
    public void onEnable() {
        releaseEverything(false);
        previousMode = mode.getMode();
        lastDamagedAtMs = Long.MIN_VALUE;
        previousHurtTime = 0;
    }

    @Override
    public void onDisable() {
        releaseEverything(true);
        InputLeaseManager.releaseOwner(INPUT_OWNER);
        PacketQueueManager.discard(PACKET_OWNER);
        previousMode = null;
        setSuffix(null);
    }

    public enum Mode {
        BlockHit,
        Predictive,
        Blatant
    }

    public void beforeAttack(EntityPlayer target) {
        if (!isEnabled() || !isThreatTarget(target)) return;
        apply(engine.beforeAttack(context(target), settings()));
    }

    public static AutoBlock getEnabledInstance() {
        if (Veyra.moduleManager == null) return null;
        Module module = Veyra.moduleManager.getModuleByClazz(AutoBlock.class);
        return module instanceof AutoBlock autoBlock && autoBlock.isEnabled()
                ? autoBlock : null;
    }
}
