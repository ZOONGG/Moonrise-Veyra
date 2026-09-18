package dev.veyra.client.module.modules.combat;

import dev.veyra.client.main.Veyra;
import dev.veyra.client.coordination.input.InputLeaseManager;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.modules.movement.KeepSprint;
import dev.veyra.client.module.modules.movement.Sprint;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.DoubleSliderSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.SubscribeEvent;
import dev.veyra.weave.events.LivingUpdateEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Input-only sprint reset. A reset is tied to a real outgoing player attack,
 * then briefly releases the physical forward bind so vanilla can start a new
 * sprint. It never sends extra packets or writes player motion/sprint state.
 */
@SuppressWarnings("unused")
public final class WTap extends Module {
    private static final String INPUT_OWNER = "w-tap";
    private final SliderSetting range;
    private final SliderSetting chance;
    private final DoubleSliderSetting startDelayTicks;
    private final DoubleSliderSetting releaseTicks;
    private final DoubleSliderSetting cooldownMs;
    private final TickSetting onlySprinting;
    private final TickSetting weaponOnly;
    private final TickSetting hurtTimeCheck;

    private volatile PendingReset pendingReset;
    private volatile long nextAllowedAtMs;
    private boolean releasingForward;
    private int restoreAtTick = Integer.MIN_VALUE;
    private int lastObservedTick = Integer.MIN_VALUE;

    public WTap() {
        super("WTap", ModuleCategory.Combat, 0);
        registerSetting(new DescriptionSetting("Briefly releases forward after real player attacks."));
        registerSetting(range = new SliderSetting("Range", 3.3D, 2.0D, 4.5D, 0.1D));
        registerSetting(chance = new SliderSetting("Chance", 85.0D, 0.0D, 100.0D, 1.0D));
        registerSetting(startDelayTicks = new DoubleSliderSetting("Start delay ticks", 0.0D, 1.0D, 0.0D, 6.0D, 1.0D));
        registerSetting(releaseTicks = new DoubleSliderSetting("Release ticks", 1.0D, 2.0D, 1.0D, 3.0D, 1.0D));
        registerSetting(cooldownMs = new DoubleSliderSetting("Cooldown ms", 350.0D, 550.0D, 100.0D, 1000.0D, 25.0D));
        registerSetting(onlySprinting = new TickSetting("Only sprinting", true));
        registerSetting(weaponOnly = new TickSetting("Weapon only", true));
        registerSetting(hurtTimeCheck = new TickSetting("Hurt-time check", true));
    }

    @SubscribeEvent
    public void onAttack(PacketEvent.Send event) {
        if (!PlayerUtils.isPlayerInGame()
                || mc.currentScreen != null
                || !(event.getPacket() instanceof C02PacketUseEntity attack)
                || attack.getAction() != C02PacketUseEntity.Action.ATTACK) {
            return;
        }

        Entity attackedEntity = attack.getEntityFromWorld(mc.theWorld);
        if (!(attackedEntity instanceof EntityPlayer target)
                || !TargetFilter.isValidPlayer(target)
                || mc.thePlayer.getDistanceToEntity(target) > range.getInput()
                || (hurtTimeCheck.isToggled() && target.hurtTime > 1)
                || !canUseForwardInput(true)
                || isKeepSprintEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (releasingForward
                || pendingReset != null
                || now < nextAllowedAtMs
                || !rollChance(chance.getInput())) {
            return;
        }

        int currentTick = mc.thePlayer.ticksExisted;
        int delay = randomInclusive(startDelayTicks.getInputMin(), startDelayTicks.getInputMax());
        int duration = randomInclusive(releaseTicks.getInputMin(), releaseTicks.getInputMax());
        nextAllowedAtMs = now + randomInclusive(cooldownMs.getInputMin(), cooldownMs.getInputMax());

        // Execute from the local player's LivingUpdate PRE hook. That hook is
        // immediately before vanilla samples movement, so a one-tick release
        // is always observed even when the attack lands late in a client tick.
        int executeAtTick = currentTick + delay;
        pendingReset = new PendingReset(
                target.getEntityId(), executeAtTick, executeAtTick + 2, duration);
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (event.type != LivingUpdateEvent.Type.PRE) return;
        if (!PlayerUtils.isPlayerInGame() || mc.currentScreen != null) {
            clearState(true);
            return;
        }

        int currentTick = mc.thePlayer.ticksExisted;
        if (lastObservedTick != Integer.MIN_VALUE && currentTick < lastObservedTick) {
            clearState(true);
        }
        lastObservedTick = currentTick;

        if (releasingForward && currentTick >= restoreAtTick) {
            restorePhysicalForwardState();
            releasingForward = false;
            restoreAtTick = Integer.MIN_VALUE;
        }

        PendingReset pending = pendingReset;
        if (pending == null || currentTick < pending.executeAtTick()) {
            return;
        }

        pendingReset = null;
        if (currentTick <= pending.expiresAtTick()
                && isPendingTargetValid(pending.targetEntityId())
                && canUseForwardInput(false)
                && !isKeepSprintEnabled()) {
            beginReset(currentTick, pending.releaseTicks());
        }
    }

    private void beginReset(int currentTick, int durationTicks) {
        int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();
        if (!isPhysicalKeyDown(forwardKey)) {
            return;
        }

        // Input only. Vanilla movement code observes W as released and later
        // observes the user's real physical key state again.
        InputLeaseManager.set(INPUT_OWNER, forwardKey, false, 70);
        releasingForward = true;
        restoreAtTick = currentTick + Math.max(1, durationTicks);
        pauseAutoSprintThrough(restoreAtTick + 1);
    }

    private boolean canUseForwardInput(boolean requireConfiguredSprintState) {
        int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();
        if (!isPhysicalKeyDown(forwardKey)
                || mc.thePlayer.moveForward <= 0.0F
                || mc.thePlayer.isSneaking()
                || mc.thePlayer.isInWater()
                || mc.thePlayer.isInLava()
                || mc.thePlayer.isOnLadder()
                || mc.thePlayer.ridingEntity != null
                || mc.thePlayer.capabilities.isFlying) {
            return false;
        }
        if (weaponOnly.isToggled() && !PlayerUtils.isPlayerHoldingWeapon()) {
            return false;
        }
        return !requireConfiguredSprintState
                || !onlySprinting.isToggled()
                || mc.thePlayer.isSprinting();
    }

    private boolean isKeepSprintEnabled() {
        if (Veyra.moduleManager == null) {
            return false;
        }
        Module keepSprint = Veyra.moduleManager.getModuleByClazz(KeepSprint.class);
        return keepSprint != null && keepSprint.isEnabled();
    }

    private boolean isPendingTargetValid(int entityId) {
        Entity entity = mc.theWorld.getEntityByID(entityId);
        return entity instanceof EntityPlayer player
                && TargetFilter.isValidPlayer(player)
                && mc.thePlayer.getDistanceToEntity(player) <= range.getInput();
    }

    private void pauseAutoSprintThrough(int tick) {
        if (Veyra.moduleManager == null) {
            return;
        }
        Module sprintModule = Veyra.moduleManager.getModuleByClazz(Sprint.class);
        if (sprintModule instanceof Sprint sprint && sprint.isEnabled()) {
            sprint.pauseAutomationUntilTick(tick);
        }
    }

    private void clearState(boolean restoreForwardKey) {
        pendingReset = null;
        nextAllowedAtMs = 0L;
        if (restoreForwardKey && releasingForward) {
            restorePhysicalForwardState();
        }
        releasingForward = false;
        restoreAtTick = Integer.MIN_VALUE;
        lastObservedTick = Integer.MIN_VALUE;
    }

    private void restorePhysicalForwardState() {
        InputLeaseManager.releaseOwner(INPUT_OWNER);
    }

    private static boolean rollChance(double percent) {
        return percent >= 100.0D
                || (percent > 0.0D && ThreadLocalRandom.current().nextDouble(100.0D) < percent);
    }

    private static int randomInclusive(double minValue, double maxValue) {
        int min = (int) Math.round(Math.min(minValue, maxValue));
        int max = (int) Math.round(Math.max(minValue, maxValue));
        return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static boolean isPhysicalKeyDown(int keyCode) {
        return InputLeaseManager.physicalState(keyCode);
    }

    @Override
    public void onEnable() {
        clearState(false);
    }

    @Override
    public void onDisable() {
        clearState(true);
    }

    private record PendingReset(int targetEntityId, int executeAtTick,
                                int expiresAtTick, int releaseTicks) {
    }
}
