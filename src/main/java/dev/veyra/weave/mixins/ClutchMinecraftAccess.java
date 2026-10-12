package dev.veyra.weave.mixins;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Minecraft.class)
public interface ClutchMinecraftAccess { @Invoker("rightClickMouse") void veyra$rightClick(); }
