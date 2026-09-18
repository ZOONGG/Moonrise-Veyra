package dev.veyra.weave.mixins;

import dev.veyra.client.combat.attack.AttackController;
import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.modules.combat.NoHitDelay;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow public int leftClickCounter;

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    public void clickMouseAfter(final CallbackInfo ci) {
        if (!AttackController.allowCurrentAttack()) {
            ci.cancel();
            return;
        }
        if (Veyra.moduleManager.getModuleByClazz(NoHitDelay.class).isEnabled()) {
            leftClickCounter = 0;
        }
    }
}
