package dev.veyra.client.module.modules.movement;

import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.modules.combat.Reach;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.module.Module;
import dev.veyra.weave.events.HitSlowDownEvent;
import net.weavemc.api.event.SubscribeEvent;

public class KeepSprint extends Module {
   public static SliderSetting speed;
   public static TickSetting reduce, sprint;

   public KeepSprint() {
      super("KeepSprint", ModuleCategory.Movement, 0);
      this.registerSetting(new DescriptionSetting("Default is 40% motion reduction"));
      this.registerSetting(new DescriptionSetting("and stopping sprint."));
      this.registerSetting(speed = new SliderSetting("Slow %", 40.0D, 0.0D, 100.0D, 1.0D));
      this.registerSetting(reduce = new TickSetting("Only reduce reach hits", false));
      this.registerSetting(sprint = new TickSetting("Stop sprint", false));
   }

   @SubscribeEvent
   public void onHitSlowDown(HitSlowDownEvent e) {
      double dist;
      Module reach = Veyra.moduleManager.getModuleByClazz(Reach.class);
      if (reduce.isToggled()
              && reach != null
              && reach.isEnabled()
              && mc.objectMouseOver != null
              && mc.objectMouseOver.hitVec != null
              && mc.getRenderViewEntity() != null
              && !mc.thePlayer.capabilities.isCreativeMode) {
         dist = mc.objectMouseOver.hitVec.distanceTo(mc.getRenderViewEntity().getPositionEyes(1.0F));
         double val;
         if (dist > 3.0D) {
            val = (100.0D - (double) ((float) speed.getInput())) / 100.0D;
         } else {
            val = 0.6D;
         }

         e.setSlowDown(val);
      } else {
         double val;
         val = (100.0D - (double) ((float) speed.getInput())) / 100.0D;
         e.setSlowDown(val);
      }
      e.setSprinting(!sprint.isToggled());
   }
}
