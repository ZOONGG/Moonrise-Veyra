package dev.veyra.client.module.modules.movement;

import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.player.movement.TimerSpeedPolicy;

public class Timer extends Module {
   public static SliderSetting speed;
   public static TickSetting strafe;
   private static volatile double horizontalMovementScale = 1.0D;

   public Timer() {
      super("Timer", ModuleCategory.Movement, 0);
      this.registerSetting(new DescriptionSetting(
              "Below 0.5 keeps controls responsive and slows local movement further."));
      this.registerSetting(speed = new SliderSetting("Speed", 1.0D, 0.1D, 2.5D, 0.1D));
      this.registerSetting(strafe = new TickSetting("Strafe only", false));
   }

   @SubscribeEvent
   public void onTick(TickEvent e) {
      if (mc.thePlayer == null || mc.currentScreen != null
              || strafe.isToggled() && mc.thePlayer.moveStrafing == 0.0F) {
         restoreNormalTiming();
         return;
      }

      TimerSpeedPolicy.Timing timing = TimerSpeedPolicy.resolve(speed.getInput());
      mc.timer.timerSpeed = timing.clientClockSpeed();
      horizontalMovementScale = timing.horizontalMovementScale();
   }

   public static double localHorizontalMovementScale() {
      return horizontalMovementScale;
   }

   @Override
   public void onDisable() {
      restoreNormalTiming();
   }

   private static void restoreNormalTiming() {
      mc.timer.timerSpeed = 1.0f;
      horizontalMovementScale = 1.0D;
   }
}
