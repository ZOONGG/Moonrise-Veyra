package dev.veyra.weave.mixins;
import dev.veyra.client.player.clutch.LocalFireballPractice;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFireball.class)
public abstract class PracticeFireChargeBlockMixin {
    @Inject(method="onItemUse", at=@At("HEAD"), cancellable=true, require=1)
    private void veyra$throwAtBlock(ItemStack stack, EntityPlayer player, World world,
          BlockPos pos, EnumFacing facing, float x, float y, float z, CallbackInfoReturnable<Boolean> cir) {
        if (LocalFireballPractice.accepts(world, stack)) {
            LocalFireballPractice.use(world, player, stack);
            cir.setReturnValue(true);
        }
    }
}
