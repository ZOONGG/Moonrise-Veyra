package dev.veyra.client.module.modules.combat;

import dev.veyra.client.combat.attack.HitSelectContext;
import dev.veyra.client.combat.attack.HitSelectDecision;
import dev.veyra.client.combat.attack.HitSelectEngine;
import dev.veyra.client.combat.attack.HitSelectMode;
import dev.veyra.client.combat.attack.HitSelectSettings;
import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.potion.Potion;
import net.minecraft.util.MovingObjectPosition;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.SubscribeEvent;

import java.util.concurrent.ThreadLocalRandom;

/** Filters only attack attempts that cannot produce the configured result. */
@SuppressWarnings("unused")
public final class HitSelect extends Module {
    private final ComboSetting<Mode> mode;
    private final SliderSetting pauseDurationMs;
    private final SliderSetting range;
    private final SliderSetting combatCancelRate;
    private final SliderSetting missedSwingCancelRate;
    private final TickSetting weaponOnly;
    private final TickSetting useServerAttackTime;
    private final TickSetting fakeSwing;
    private final TickSetting waitForFirstHit;
    private final TickSetting hitLaterInTrades;
    private final TickSetting disableDuringKnockback;
    private final TickSetting onlyWhileDamaged;

    private final HitSelectEngine engine = new HitSelectEngine();

    public HitSelect() {
        super("HitSelect", ModuleCategory.Combat, 0);
        registerSetting(new DescriptionSetting("Filters wasted clicks without delaying the next valid hit."));
        registerSetting(mode = new ComboSetting<>("Mode", Mode.Burst));
        registerSetting(pauseDurationMs = new SliderSetting("Pause duration ms", 450.0D, 0.0D, 500.0D, 10.0D));
        registerSetting(range = new SliderSetting("Range", 3.3D, 2.0D, 4.5D, 0.1D));
        registerSetting(combatCancelRate = new SliderSetting("Cancel rate combat", 100.0D, 0.0D, 100.0D, 1.0D));
        registerSetting(missedSwingCancelRate = new SliderSetting("Cancel rate misses", 0.0D, 0.0D, 100.0D, 1.0D));
        registerSetting(weaponOnly = new TickSetting("Weapon only", true));
        registerSetting(useServerAttackTime = new TickSetting("Server attack time", false));
        registerSetting(fakeSwing = new TickSetting("Fake swing", true));
        registerSetting(waitForFirstHit = new TickSetting("Wait for first hit", false));
        registerSetting(hitLaterInTrades = new TickSetting("Hit later in trades", false));
        registerSetting(disableDuringKnockback = new TickSetting("Disable in knockback", true));
        registerSetting(onlyWhileDamaged = new TickSetting("Only while damaged", false));
    }

    /** Called once from Minecraft's central clickMouse path. */
    public HitSelectDecision evaluateCurrentAttack() {
        if (!PlayerUtils.isPlayerInGame()
                || mc.currentScreen != null
                || (weaponOnly.isToggled() && !isHoldingWeapon())) {
            return HitSelectDecision.allow();
        }

        long nowMs = System.nanoTime() / 1_000_000L;
        int chanceRoll = ThreadLocalRandom.current().nextInt(100);
        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY
                || !(hit.entityHit instanceof EntityPlayer target)) {
            return engine.decide(HitSelectContext.missedSwing(nowMs, chanceRoll), settings());
        }
        return evaluateTarget(target, nowMs, chanceRoll);
    }

    public HitSelectDecision evaluateTarget(EntityPlayer target) {
        return evaluateTarget(target, System.nanoTime() / 1_000_000L,
                ThreadLocalRandom.current().nextInt(100));
    }

    private HitSelectDecision evaluateTarget(EntityPlayer target, long nowMs, int chanceRoll) {
        // TargetFilter controls assistance eligibility, not the user's ability to
        // attack a friend/team member manually.
        if (!TargetFilter.isValidPlayer(target)
                || mc.thePlayer.getDistanceToEntity(target) > range.getInput()) {
            return HitSelectDecision.allow();
        }

        HitSelectContext context = HitSelectContext.playerTarget(
                        target.getUniqueID().toString(), target.hurtTime, nowMs, chanceRoll)
                .withSelfHurtTime(mc.thePlayer.hurtTime)
                .withVerticalState(mc.thePlayer.motionY > 0.0D,
                        mc.thePlayer.motionY <= 0.0D, criticalHitPossible());
        return engine.decide(context, settings());
    }

    private HitSelectSettings settings() {
        HitSelectMode selected = mode.getMode() == Mode.Criticals
                ? HitSelectMode.CRITICALS : HitSelectMode.BURST;
        return new HitSelectSettings(selected,
                (int) Math.round(combatCancelRate.getInput()),
                (int) Math.round(missedSwingCancelRate.getInput()),
                Math.round(pauseDurationMs.getInput()),
                waitForFirstHit.isToggled(),
                hitLaterInTrades.isToggled(),
                useServerAttackTime.isToggled(),
                fakeSwing.isToggled(),
                disableDuringKnockback.isToggled(),
                onlyWhileDamaged.isToggled());
    }

    @SubscribeEvent
    public void onSentAttack(PacketEvent.Send event) {
        if (!PlayerUtils.isPlayerInGame()
                || !(event.getPacket() instanceof C02PacketUseEntity attack)
                || attack.getAction() != C02PacketUseEntity.Action.ATTACK) {
            return;
        }
        Entity entity = attack.getEntityFromWorld(mc.theWorld);
        if (entity instanceof EntityPlayer target) {
            engine.recordOwnAttack(target.getUniqueID().toString(),
                    System.nanoTime() / 1_000_000L, hitLaterInTrades.isToggled());
        }
    }

    private boolean criticalHitPossible() {
        return !mc.thePlayer.onGround
                && !mc.thePlayer.isInWater()
                && !mc.thePlayer.isOnLadder()
                && !mc.thePlayer.isRiding()
                && !mc.thePlayer.capabilities.isFlying
                && !mc.thePlayer.isPotionActive(Potion.blindness)
                && !mc.thePlayer.isEntityInsideOpaqueBlock();
    }

    private boolean isHoldingWeapon() {
        if (mc.thePlayer.getCurrentEquippedItem() == null) return false;
        return mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemSword
                || mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemAxe;
    }

    public static HitSelect getEnabledInstance() {
        if (Veyra.moduleManager == null) return null;
        Module module = Veyra.moduleManager.getModuleByClazz(HitSelect.class);
        return module instanceof HitSelect hitSelect && hitSelect.isEnabled() ? hitSelect : null;
    }

    @Override
    public void onEnable() {
        engine.reset();
    }

    @Override
    public void onDisable() {
        engine.reset();
    }

    public enum Mode {
        Burst,
        Criticals
    }
}
