package dev.veyra.weave.mixins;
import dev.veyra.client.player.clutch.LocalFireballPractice;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Enables vanilla pearl projectiles in Creative too, solely inside the local practice session. */
@Mixin(ItemEnderPearl.class)
public abstract class PracticePearlUseMixin {
    @Inject(method="onItemRightClick",at=@At("HEAD"),cancellable=true,require=1)
    private void veyra$practicePearl(ItemStack stack, World world, EntityPlayer player,
                                    CallbackInfoReturnable<ItemStack> cir) {
        if(stack.stackSize>0 && LocalFireballPractice.acceptsWorld(world))
            cir.setReturnValue(LocalFireballPractice.usePearl(world,player,stack));
    }
}
