package dev.veyra.weave.mixins;

import dev.veyra.client.player.clutch.ClutchInput;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MouseHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a one-shot delta after physical input; Minecraft handles camera scaling and inversion. */
@Mixin(MouseHelper.class)
public abstract class ClutchMouseInputMixin {
    @Shadow public int deltaY;

    @Inject(method = "mouseXYChange", at = @At("RETURN"), require = 1)
    private void veyra$appendMouseInput(CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ClutchInput.hasAim()) return;
        if (mc.thePlayer == null || !mc.inGameHasFocus || mc.currentScreen != null
                || mc.gameSettings.smoothCamera) {
            ClutchInput.clearAim();
            return;
        }
        deltaY = ClutchInput.mouseDelta(mc, deltaY);
    }
}
