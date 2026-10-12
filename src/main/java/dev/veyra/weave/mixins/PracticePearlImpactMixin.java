package dev.veyra.weave.mixins;
import dev.veyra.client.player.clutch.LocalFireballState;
import dev.veyra.client.player.clutch.LocalFireballPractice;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityEnderPearl.class)
public abstract class PracticePearlImpactMixin {
    @Inject(method="onImpact",at=@At("HEAD"),require=1)
    private void veyra$pearlHitsPracticeFireball(MovingObjectPosition hit, CallbackInfo ci) {
        if (hit.entityHit instanceof LocalFireballState ball && ball.veyra$isLocalPractice()) {
            LocalFireballPractice.traceEvent(((EntityEnderPearl)(Object)this).worldObj,"pearl-hit-fireball");
            ball.veyra$explodePractice();
        }
        // Vanilla handles the pearl's own impact and teleport.
    }
}
