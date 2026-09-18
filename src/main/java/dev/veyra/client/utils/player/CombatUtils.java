package dev.veyra.client.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;

public class CombatUtils {
    public static boolean canTarget(Entity entity, boolean idk) {
        if(entity != null && entity != Minecraft.getMinecraft().thePlayer) {
            EntityLivingBase entityLivingBase = null;

            if(entity instanceof EntityLivingBase) {
                entityLivingBase = (EntityLivingBase)entity;
            }

            boolean isTeam = isTeam(Minecraft.getMinecraft().thePlayer, entity);
            boolean isVisible = (!entity.isInvisible());

            return !(entity instanceof EntityArmorStand) && isVisible && (entity instanceof EntityPlayer && !isTeam && !idk || entity instanceof EntityAnimal || entity instanceof EntityMob || entity instanceof EntityLivingBase && entityLivingBase.isEntityAlive());
        } else {
            return false;
        }
    }

    public static boolean isTeam(EntityPlayer player, Entity entity) {
        return entity instanceof EntityPlayer && TeamDetector.areTeammates(player, (EntityPlayer) entity);
    }


}
