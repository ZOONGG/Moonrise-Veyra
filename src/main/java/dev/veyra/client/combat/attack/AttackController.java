package dev.veyra.client.combat.attack;

import dev.veyra.client.module.modules.combat.HitSelect;
import dev.veyra.client.module.modules.combat.AutoBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

/** Single decision point for attacks reaching Minecraft's normal click path. */
public final class AttackController {
    private AttackController() {
    }

    public static boolean allowCurrentAttack() {
        EntityPlayer target = currentPlayerTarget();
        HitSelect hitSelect = HitSelect.getEnabledInstance();
        if (hitSelect != null && !apply(hitSelect.evaluateCurrentAttack())) return false;
        prepareAutoBlock(target);
        return true;
    }

    public static boolean allowTargetAttack(EntityPlayer target) {
        HitSelect hitSelect = HitSelect.getEnabledInstance();
        if (hitSelect != null && !apply(hitSelect.evaluateTarget(target))) return false;
        prepareAutoBlock(target);
        return true;
    }

    private static boolean apply(HitSelectDecision decision) {
        if (!decision.attackAllowed() && decision.swingClientSide()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) mc.thePlayer.swingItem();
        }
        return decision.attackAllowed();
    }

    private static void prepareAutoBlock(EntityPlayer target) {
        if (target == null) return;
        AutoBlock autoBlock = AutoBlock.getEnabledInstance();
        if (autoBlock != null) autoBlock.beforeAttack(target);
    }

    private static EntityPlayer currentPlayerTarget() {
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) {
            return null;
        }
        Entity entity = hit.entityHit;
        return entity instanceof EntityPlayer player ? player : null;
    }
}
