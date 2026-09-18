package dev.veyra.client.module.modules.combat;

import dev.veyra.client.combat.attack.AttackController;
import dev.veyra.client.coordination.input.InputLeaseManager;
import dev.veyra.client.clickgui.Theme;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.math.TimerUtils;
import dev.veyra.client.utils.Utils;
import dev.veyra.client.utils.player.PlayerUtils;
import dev.veyra.weave.events.UpdateEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.weavemc.api.event.*;
import org.lwjgl.input.Mouse;

import java.util.Optional;
import java.util.Comparator;

@SuppressWarnings("unused")
public class Killaura extends Module {
    private static final String BLOCK_OWNER = "killaura-autoblock";
    static Optional<EntityPlayer> target = Optional.empty();
    public static SliderSetting range, frequency, hurtTimeAmt, rotRand;
    public static TickSetting shouldBlock, targetESP, testSetting, alwaysAB, rots, whenLooking;
    public TimerUtils timer = new TimerUtils();
    public boolean delaying, isAttacking = false;
    long lastClickTime = 0;
    long lastAttackTime = 0;
    int rmb = mc.gameSettings.keyBindUseItem.getKeyCode();

    public Killaura() {
        super("Killaura", ModuleCategory.Combat, 0);
        this.registerSetting(new DescriptionSetting("Automatically attacks the nearest valid target."));
        this.registerSetting(range = new SliderSetting("Range", 3, 3, 6, 0.1));
        this.registerSetting(frequency = new SliderSetting("CPS", 10, 1, 20, 0.5));
        this.registerSetting(hurtTimeAmt = new SliderSetting("Ignore before hurt time", 1, 1, 20, 1));
        this.registerSetting(rotRand = new SliderSetting("Rotation Randomization", 2, 0, 3, .01));
        this.registerSetting(rots = new TickSetting("Rotations (for bypassing)", false));
        this.registerSetting(whenLooking = new TickSetting("Only when looking at player", false));
        this.registerSetting(shouldBlock = new TickSetting("Autoblock (Hold RMB)", false));
        this.registerSetting(alwaysAB = new TickSetting("Autoblock", false));
        this.registerSetting(targetESP = new TickSetting("ESP", false));
    }

    @Override
    public void onDisable() {
        target = Optional.empty();
        isAttacking = false;
        InputLeaseManager.releaseOwner(BLOCK_OWNER);
    }
    @SubscribeEvent
    public void setTarget(TickEvent.Pre e) {
        if (PlayerUtils.isPlayerInGame()) {
            target = mc.theWorld != null
                    ? mc.theWorld.playerEntities.stream()
                    .filter(TargetFilter::isValidPlayer)
                    .filter(player -> player.getDistanceToEntity(mc.thePlayer) <= range.getInput())
                    .min(Comparator.comparingDouble(player -> player.getDistanceToEntity(mc.thePlayer)))
                    : Optional.empty();
        }
    }
    public boolean aBooleanCheck() {
        if (!whenLooking.isToggled()) return false;
        MovingObjectPosition result = mc.objectMouseOver;
        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && result.entityHit instanceof EntityPlayer targetPlayer) {
            return whenLooking.isToggled() && PlayerUtils.lookingAtPlayer(mc.thePlayer, targetPlayer, range.getInput() + 1);
        }
        else return false;
    }
    @SubscribeEvent
    public void experiMental(UpdateEvent.Pre e) {
        isAttacking = false;
        if (target.isEmpty() || !PlayerUtils.isPlayerInGame()) {
            return;
        }
        if (timer.hasReached(1000 / frequency.getInput() + Utils.Java.randomInt(-3, 3))
                && target.get().hurtTime < hurtTimeAmt.getInput()
                && mc.currentScreen == null) {
            if (target.isPresent()) {
                if (mc.thePlayer.isBlocking() || mc.thePlayer.isEating()) return;
                if (whenLooking.isToggled() && !aBooleanCheck()) return;
                if (target.get().deathTime > 0) return;
                if (!AttackController.allowTargetAttack(target.get())) return;
                mc.thePlayer.swingItem();
                mc.playerController.attackEntity(mc.thePlayer, target.get());
                timer.reset();
                isAttacking = true;
                lastAttackTime = System.currentTimeMillis();
            }
        }
    }
    public void finishDelay() {
        long currentTime = System.currentTimeMillis();
        int newdelay = Utils.Java.randomInt(20, 70);
        if (currentTime - lastClickTime >= newdelay) {
            lastClickTime = currentTime;
            InputLeaseManager.releaseOwner(BLOCK_OWNER);
            delaying = false;
        }
    }
    @SubscribeEvent
    public void onRender(RenderHandEvent e) {
        if (AutoBlock.getEnabledInstance() != null) {
            InputLeaseManager.releaseOwner(BLOCK_OWNER);
            delaying = false;
            return;
        }
        if (((Mouse.isButtonDown(1) && shouldBlock.isToggled()) || alwaysAB.isToggled()) && PlayerUtils.isPlayerHoldingWeapon() && isAttacking && mc.currentScreen == null) {
            long currentTime = System.currentTimeMillis();
            int delay = 1000 / (int) frequency.getInput() + Utils.Java.randomInt(-3, 3) - 4;
            if (currentTime - lastClickTime >= delay && !delaying) {
                lastClickTime = currentTime;
                InputLeaseManager.set(BLOCK_OWNER, rmb, true, 20);
                delaying = true;
            }
            if (delaying) {
                finishDelay();
            }
        }
    }
    @SubscribeEvent
    public void ESP(RenderWorldEvent e) {
        if (targetESP.isToggled() && target.isPresent()) {
            Utils.HUD.drawBoxAroundEntity(target.get(), 1, 0.0D, 0.0D, Theme.getMainColor().getRGB(), true);
            Utils.HUD.drawBoxAroundEntity(target.get(), 2, 0.0D, 0.0D, Theme.getMainColor().getRGB(), true);
        }
    }
    @SubscribeEvent
    public void unblockthings(TickEvent e) {
        if (!PlayerUtils.isPlayerInGame()) return;
        if (mc.thePlayer.isBlocking() && PlayerUtils.isPlayerHoldingWeapon()
                && !Mouse.isButtonDown(1) && mc.currentScreen == null
                && System.currentTimeMillis() - lastAttackTime > 140L) {
            InputLeaseManager.releaseOwner(BLOCK_OWNER);
        }
    }
    @SubscribeEvent
    public void onClientTick(RenderWorldEvent e) {
        if (PlayerUtils.isPlayerInGame() && target.isPresent() && mc.currentScreen == null && rots.isToggled() && !mc.thePlayer.isEating()) {
            double deltaX = target.get().posX - mc.thePlayer.posX;
            double deltaY = target.get().posY + target.get().getEyeHeight() - mc.thePlayer.posY - mc.thePlayer.getEyeHeight();
            double deltaZ = target.get().posZ - mc.thePlayer.posZ;
            double distance = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);

            float yaw = (float) (Math.atan2(deltaZ, deltaX) * (180 / Math.PI)) - 90.0F + (float) Utils.Java.randomInt(-rotRand.getInput(), rotRand.getInput());
            float pitch = (float) (-(Math.atan2(deltaY, distance) * (180 / Math.PI))) + (float) Utils.Java.randomInt(-rotRand.getInput(), rotRand.getInput());

            mc.thePlayer.rotationYaw = yaw;
            mc.thePlayer.rotationPitch = pitch;
        }
    }
}
