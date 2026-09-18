package dev.veyra.client.module.modules.combat;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.*;
import dev.veyra.client.utils.game.MouseManager;
import dev.veyra.client.utils.math.MathUtils;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.client.settings.KeyBinding;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.MouseEvent;
import net.weavemc.api.event.TickEvent;

@SuppressWarnings("unused")
public class ClickAssist extends Module {

   public static SliderSetting chance;
   public static TickSetting cpsCheck;
   public static DoubleSliderSetting cd;
   public static ComboSetting<modes> mode;
   private long pendingClickAt = -1L;

   public ClickAssist() {
      super("ClickAssist", ModuleCategory.Combat, 0);
      this.registerSetting(new DescriptionSetting("Chance to double click."));
      this.registerSetting(chance = new SliderSetting("Chance", 50.0D, 0.0D, 100.0D, 1.0D));
      this.registerSetting(cd = new DoubleSliderSetting("Delay", 40, 100, 0, 300, 1));
      this.registerSetting(cpsCheck = new TickSetting("Only if above 5 CPS", true));
      this.registerSetting(mode = new ComboSetting<>("Mode", modes.LMB));
   }

   private int getButton() {
      return mode.getMode() == modes.LMB ? 0 : 1;
   }

   @SubscribeEvent
   public void onClick(MouseEvent e) {
      if (!PlayerUtils.isPlayerInGame() || mc.currentScreen != null || !e.getButtonState()) return;
      if (e.getButton() != getButton() || pendingClickAt >= 0L) return;

      int currentCps = mode.getMode() == modes.LMB
              ? MouseManager.getLeftClickCounter()
              : MouseManager.getRightClickCounter();
      if (cpsCheck.isToggled() && currentCps < 5) return;
      if (Math.random() * 100.0D >= chance.getInput()) return;

      pendingClickAt = System.currentTimeMillis()
              + MathUtils.randomInt(cd.getInputMin(), cd.getInputMax());
   }

   @SubscribeEvent
   public void onTick(TickEvent.Pre e) {
      if (pendingClickAt < 0L || System.currentTimeMillis() < pendingClickAt) return;
      if (!PlayerUtils.isPlayerInGame() || mc.currentScreen != null) {
         pendingClickAt = -1L;
         return;
      }

      if (mode.getMode() == modes.LMB) {
         KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
         MouseManager.addLeftClick();
      } else {
         KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
         MouseManager.addRightClick();
      }
      pendingClickAt = -1L;
   }

   public void onDisable() {
      pendingClickAt = -1L;
   }

   public enum modes {
      LMB, RMB
   }
}
