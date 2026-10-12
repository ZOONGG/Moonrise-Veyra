package dev.veyra.weave.mixins;
import dev.veyra.client.player.clutch.LocalFireballPractice;
import dev.veyra.client.player.clutch.LocalFireballState;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLargeFireball.class)
public abstract class PracticeLargeFireballMixin implements LocalFireballState {
    @Unique private boolean veyra$practice, veyra$blocks;
    @Unique private float veyra$power;
    @Unique private double veyra$knockback = 1.35;
    public void veyra$configurePractice(float power, boolean blocks, double knockback) {
        veyra$practice=true; veyra$power=Math.max(.5f,Math.min(4,power)); veyra$blocks=blocks; veyra$knockback=Math.max(1,Math.min(3,knockback));
    }
    public boolean veyra$isLocalPractice() { return veyra$practice; }
    public void veyra$explodePractice() {
        EntityLargeFireball self = (EntityLargeFireball)(Object)this;
        if (!veyra$practice || self.worldObj.isRemote || self.isDead) return;
        LocalFireballPractice.traceEvent(self.worldObj,"explode-fireball");
        double radius=veyra$power*2;
        java.util.Map<net.minecraft.entity.Entity, double[]> velocities=new java.util.HashMap<>();
        for(net.minecraft.entity.Entity entity:self.worldObj.getEntitiesWithinAABBExcludingEntity(self,
                self.getEntityBoundingBox().expand(radius,radius,radius))) {
            if(entity instanceof net.minecraft.entity.EntityLivingBase)
                velocities.put(entity,new double[]{entity.motionX,entity.motionY,entity.motionZ});
        }
        self.setDead();
        // Native Explosion implements damage, visibility checks and player knockback.
        self.worldObj.newExplosion(self, self.posX, self.posY, self.posZ, veyra$power, false, veyra$blocks);
        for(java.util.Map.Entry<net.minecraft.entity.Entity,double[]> entry:velocities.entrySet()) {
            net.minecraft.entity.Entity entity=entry.getKey(); double[] before=entry.getValue();
            if(entity.motionX==before[0] && entity.motionY==before[1] && entity.motionZ==before[2])continue;
            entity.motionX=dev.veyra.client.player.clutch.FireballPracticePhysics.amplifyImpulse(before[0],entity.motionX,veyra$knockback);
            entity.motionY=dev.veyra.client.player.clutch.FireballPracticePhysics.amplifyImpulse(before[1],entity.motionY,veyra$knockback);
            entity.motionZ=dev.veyra.client.player.clutch.FireballPracticePhysics.amplifyImpulse(before[2],entity.motionZ,veyra$knockback);
            entity.velocityChanged=true;
        }
    }
    @Inject(method="onImpact",at=@At("HEAD"),cancellable=true,require=1)
    private void veyra$practiceImpact(MovingObjectPosition hit, CallbackInfo ci) {
        if (veyra$practice) {
            EntityLargeFireball self=(EntityLargeFireball)(Object)this;
            LocalFireballPractice.traceEvent(self.worldObj,"fireball-impact-"+hit.typeOfHit);
            veyra$explodePractice(); ci.cancel();
        }
    }
    @Inject(method="writeEntityToNBT",at=@At("RETURN"),require=1)
    private void veyra$save(NBTTagCompound tag, CallbackInfo ci) {
        if (veyra$practice) {
            tag.setBoolean(LocalFireballPractice.TAG,true);
            tag.setDouble("VeyraLocalFireballKnockback",veyra$knockback);
            tag.setFloat(LocalFireballPractice.POWER_TAG,veyra$power);
            tag.setBoolean(LocalFireballPractice.BLOCKS_TAG,veyra$blocks);
        }
    }
    @Inject(method="readEntityFromNBT",at=@At("RETURN"),require=1)
    private void veyra$load(NBTTagCompound tag, CallbackInfo ci) {
        veyra$practice=tag.getBoolean(LocalFireballPractice.TAG);
        veyra$knockback=tag.hasKey("VeyraLocalFireballKnockback") ? Math.max(1,Math.min(3,tag.getDouble("VeyraLocalFireballKnockback"))) : 1.35;
        veyra$power=Math.max(.5f, Math.min(4,tag.getFloat(LocalFireballPractice.POWER_TAG)));
        veyra$blocks=tag.getBoolean(LocalFireballPractice.BLOCKS_TAG);
    }
}
