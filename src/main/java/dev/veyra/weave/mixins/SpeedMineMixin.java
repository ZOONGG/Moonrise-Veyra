package dev.veyra.weave.mixins;

import dev.veyra.client.module.modules.player.SpeedMine;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerControllerMP.class)
public abstract class SpeedMineMixin {
    @Redirect(method = {"clickBlock", "onPlayerDamageBlock"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/Block;getPlayerRelativeBlockHardness(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/util/BlockPos;)F"), require = 2)
    private float veyra$scaleMiningProgress(Block block, EntityPlayer player, World world, BlockPos pos) {
        return SpeedMine.scaleProgress(block.getPlayerRelativeBlockHardness(player, world, pos));
    }
}
