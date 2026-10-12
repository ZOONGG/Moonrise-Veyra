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
    private long veyra$nextGuiUpdate;

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void veyra$updateTimer(CallbackInfo ci) {
        Veyra.pollFrameBinds();
        dev.veyra.client.module.modules.movement.Timer.updateRuntime();
    }

    // Vanilla updateTimer multiplies elapsed time by timerSpeed, so literal zero
    // stops ticks without division or blocking. GUI input normally lives in runTick;
    // service it on tickless frames using a separate real-time cadence.
    @Inject(method = "runGameLoop", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Timer;updateTimer()V", shift = At.Shift.AFTER))
    private void veyra$serviceTimerGui(CallbackInfo ci) throws java.io.IOException {
        Minecraft mc = (Minecraft) (Object) this;
        if (Veyra.moduleManager != null) {
            var clutch = (dev.veyra.client.module.modules.player.VoidClutch) Veyra.moduleManager
                    .getModuleByClazz(dev.veyra.client.module.modules.player.VoidClutch.class);
            if (clutch != null) clutch.afterTimerUpdate();
        }
        long now = System.nanoTime();
        if (mc.timer.timerSpeed >= 1.0F || mc.timer.elapsedTicks > 0) {
            veyra$nextGuiUpdate = now + 50_000_000L;
            return;
        }
        if (mc.currentScreen != null && now >= veyra$nextGuiUpdate) {
            veyra$nextGuiUpdate = now + 50_000_000L;
            mc.currentScreen.handleInput();
            if (mc.currentScreen != null) mc.currentScreen.updateScreen();
        }
    }

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
