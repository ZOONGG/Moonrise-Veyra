package dev.veyra.weave.mixins;
import dev.veyra.client.player.clutch.LocalFireballPractice;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class PracticeFireChargeMixin {
    @Inject(method="useItemRightClick", at=@At("HEAD"), cancellable=true, require=1)
    private void veyra$throwPracticeFireball(World world, EntityPlayer player,
                                           CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack=(ItemStack)(Object)this;
        if (LocalFireballPractice.accepts(world, stack))
            cir.setReturnValue(LocalFireballPractice.use(world, player, stack));
    }
}
