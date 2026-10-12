package dev.veyra.client.module.modules.movement;

import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.player.movement.TimerRuntimeController;
import dev.veyra.client.player.movement.TimerRuntimeOwnership;

public class Timer extends Module {
   public static SliderSetting speed;
   public static TickSetting strafe;
   private static TickSetting experimentalFreeze;
   private static TickSetting autoReset;
   private static TickSetting autoDisable;
   private static SliderSetting resumeDelay;
   private static final java.util.concurrent.ConcurrentMap<String, Long> correctionTimings =
           new java.util.concurrent.ConcurrentHashMap<>();
   private static volatile long freezeStarted;
   private static volatile String timingServer;
   private static volatile boolean timingObserved;
   private static final dev.veyra.client.player.movement.FreezeRecoveryController recovery =
           new dev.veyra.client.player.movement.FreezeRecoveryController();

   public static void serverCorrection() {
      Timer module = Veyra.moduleManager == null ? null
              : (Timer) Veyra.moduleManager.getModuleByClazz(Timer.class);
      if (module != null && module.isEnabled() && experimentalFreeze != null
              && experimentalFreeze.isToggled() && autoReset != null
              && (autoReset.isToggled() || autoDisable.isToggled())) {
         long now = System.nanoTime();
         if (!timingObserved && freezeStarted != 0 && timingServer != null) {
            timingObserved = true;
            correctionTimings.put(timingServer, Math.max(0L, now - freezeStarted));
         }
         recovery.corrected(autoReset.isToggled() && !autoDisable.isToggled(), System.nanoTime(),
                 (long) (resumeDelay.getInput() * 1_000_000_000L));
      }
   }
   private static final TimerRuntimeController runtime = new TimerRuntimeController();

   private static final TimerRuntimeOwnership ownership = new TimerRuntimeOwnership();
   public static boolean freezeMovement(Object entity) {
      if (entity != mc.thePlayer) return false;
      boolean frozen = shouldFreezeMovement();
      if (frozen) {
         mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0.0D;
         // Cancelling vanilla movement also skips its collision-state refresh.
         // Recompute support from the actual bounding box instead of retaining
         // the onGround value from the tick before the freeze.
         boolean supported = !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer,
                 mc.thePlayer.getEntityBoundingBox().offset(0.0D, -0.001D, 0.0D)).isEmpty();
         mc.thePlayer.onGround = supported;
         mc.thePlayer.isCollidedVertically = supported;
         mc.thePlayer.isCollidedHorizontally = false;
         mc.thePlayer.isCollided = supported;
      }
      return frozen;
   }

   public Timer() {
      super("Timer", ModuleCategory.Movement, 0);
      this.registerSetting(new DescriptionSetting("0.00 pauses game ticks; client controls remain available."));
      this.registerSetting(speed = new SliderSetting("Speed", 1.0D, 0.0D, 2.5D, 0.01D));
      this.registerSetting(strafe = new TickSetting("Strafe only", false));
      this.registerSetting(experimentalFreeze = new TickSetting("Experimental freeze", false));
      this.registerSetting(autoReset = new TickSetting("Auto resume", false) {
         @Override public boolean isVisible() { return experimentalFreeze.isToggled(); }
         @Override public void toggle() { setEnabled(!isToggled()); }
         @Override public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (enabled && autoDisable != null) autoDisable.disable();
         }
      });
      this.registerSetting(resumeDelay = new SliderSetting("Resume delay (seconds)", 0.1D, 0.05D, 1.0D, 0.01D) {
         @Override public boolean isVisible() {
            return experimentalFreeze.isToggled() && autoReset.isToggled();
         }
      });
      this.registerSetting(autoDisable = new TickSetting("Auto disable on correction", false) {
         @Override public boolean isVisible() { return experimentalFreeze.isToggled(); }
         @Override public void toggle() { setEnabled(!isToggled()); }
         @Override public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (enabled && autoReset != null) autoReset.disable();
         }
      });
      this.registerSetting(new DescriptionSetting("Freeze stops local movement; ticks and clicks remain normal. Speed is ignored."));
   }

   public static TimerRuntimeController.Override temporaryOverride(double value) {
      return runtime.override(value);
   }

   /** Called every frame, including frames without game ticks. */
   public static void updateRuntime() {
      if (Veyra.moduleManager == null || speed == null) return;
      Timer module = (Timer) Veyra.moduleManager.getModuleByClazz(Timer.class);
      if (module != null && module.isEnabled()) {
         if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead) {
            module.disable();
         } else {
            recovery.advance(System.nanoTime());
            if (!experimentalFreeze.isToggled() || !autoReset.isToggled() && !autoDisable.isToggled())
               recovery.arm();
            if (recovery.shouldDisable()) module.disable();
         }
      }
      boolean active = module != null && module.isEnabled() && mc.thePlayer != null
              && (!strafe.isToggled() || mc.currentScreen == null
                    && isBindDown(mc.gameSettings.keyBindLeft.getKeyCode())
                        != isBindDown(mc.gameSettings.keyBindRight.getKeyCode()));
      runtime.setManual(active, experimentalFreeze.isToggled() ? 1.0D : speed.getInput());
      float current = mc.timer.timerSpeed;
      float next = ownership.update(current,
              mc.thePlayer != null && runtime.isActive(), (float) runtime.speed());
      if (Float.compare(current, next) != 0) mc.timer.timerSpeed = next;
      if (active && experimentalFreeze.isToggled()) {
         String server = mc.getCurrentServerData() == null ? "singleplayer" : mc.getCurrentServerData().serverIP;
         if (freezeStarted == 0 || !server.equals(timingServer)) {
            freezeStarted = System.nanoTime();
            timingServer = server;
            timingObserved = false;
         }
         dev.veyra.weave.mixins.ExperimentalTimerFreezeMixin
                 .connectActualClass(net.minecraft.entity.Entity.class, Timer.class);
         if (autoReset.isToggled() || autoDisable.isToggled()) {
            dev.veyra.weave.mixins.ExperimentalTimerFreezeMixin
                    .connectCorrectionClass(net.minecraft.client.network.NetHandlerPlayClient.class, Timer.class);
         }
      } else {
         freezeStarted = 0;
         timingObserved = false;
      }
   }

   @net.weavemc.api.event.SubscribeEvent
   public void onOverlay(net.weavemc.api.event.RenderGameOverlayEvent.Post event) {
      if (!isEnabled() || !experimentalFreeze.isToggled() || !autoDisable.isToggled()
              || mc.thePlayer == null || mc.currentScreen != null || mc.gameSettings.hideGUI
              || freezeStarted == 0) return;
      double elapsed = (System.nanoTime() - freezeStarted) / 1_000_000_000D;
      Long previous = timingServer == null ? null : correctionTimings.get(timingServer);
      String text;
      int color = 0xFFFFFFFF;
      if (previous == null) text = String.format(java.util.Locale.ROOT, "%.1f", elapsed);
      else {
         double remaining = previous / 1_000_000_000D - elapsed;
         text = String.format(java.util.Locale.ROOT, "%.1f", Math.max(0.0D, remaining));
         if (remaining <= 0.3D) color = 0xFFFF5555;
         else if (remaining <= 1.0D) color = 0xFFFFD54F;
      }
      net.minecraft.client.gui.ScaledResolution resolution = new net.minecraft.client.gui.ScaledResolution(mc);
      mc.fontRendererObj.drawStringWithShadow(text,
              (resolution.getScaledWidth() - mc.fontRendererObj.getStringWidth(text)) / 2F,
              resolution.getScaledHeight() / 2F + 16F, color);
   }

   /** Evaluated at movement time so disable, world changes and overrides take effect immediately. */
   public static boolean shouldFreezeMovement() {
      if (Veyra.moduleManager == null || experimentalFreeze == null
              || !experimentalFreeze.isToggled() || recovery.blocksFreeze() || runtime.hasOverrides()
              || mc.thePlayer == null || mc.theWorld == null
              || mc.thePlayer.isDead || mc.thePlayer.isRiding()
              || mc.thePlayer.isSpectator()) return false;
      Timer module = (Timer) Veyra.moduleManager.getModuleByClazz(Timer.class);
      return module != null && module.isEnabled()
              && (!strafe.isToggled() || mc.currentScreen == null
                    && isBindDown(mc.gameSettings.keyBindLeft.getKeyCode())
                        != isBindDown(mc.gameSettings.keyBindRight.getKeyCode()));
   }

   @Override
   public void onEnable() { freezeStarted = 0; timingObserved = false; recovery.arm(); updateRuntime(); }

   @Override
   public void onDisable() { freezeStarted = 0; recovery.disarm(); updateRuntime(); }
}
