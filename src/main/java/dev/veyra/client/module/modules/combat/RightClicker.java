package dev.veyra.client.module.modules.combat;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.game.MouseManager;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.weavemc.api.event.RenderHandEvent;
import net.weavemc.api.event.SubscribeEvent;
import org.lwjgl.input.Mouse;

@SuppressWarnings("unused")
public class RightClicker extends Module {
   public static SliderSetting rCPS;
   public static TickSetting noConsumables, noSword, noBow, noRod;
   private long lastClickTime;
   private long nextClickDelay = 100L;
   private final int rmb = mc.gameSettings.keyBindUseItem.getKeyCode();

   public RightClicker() {
      super("RightClicker", ModuleCategory.Combat, 0);
      this.registerSetting(new DescriptionSetting("Click automatically"));
      this.registerSetting(rCPS = new SliderSetting("CPS", 10.0D, 1.0D, 20.0D, 1.0D));
      this.registerSetting(noConsumables = new TickSetting("Blacklist Consumables", false));
      this.registerSetting(noSword = new TickSetting("Blacklist Weapons", false));
      this.registerSetting(noBow = new TickSetting("Blacklist Bows", false));
      this.registerSetting(noRod = new TickSetting("Blacklist Rods", false));
   }

   public boolean isConsumable() {
      if (mc.thePlayer.getHeldItem() != null) {
         return noConsumables.isToggled() && (mc.thePlayer.getHeldItem().getItem() instanceof ItemFood || mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion);
      } else return false;
   }

   public boolean isRod() {
      if (mc.thePlayer.getHeldItem() != null) {
         return noRod.isToggled() && mc.thePlayer.getHeldItem().getItem() instanceof net.minecraft.item.ItemFishingRod;
      } else return false;
   }

   public boolean isBow() {
      if (mc.thePlayer.getHeldItem() != null) {
         return noBow.isToggled() && mc.thePlayer.getHeldItem().getItem() instanceof net.minecraft.item.ItemBow;
      } else return false;
   }

   @SubscribeEvent
   public void bop(RenderHandEvent e) {
      if (!PlayerUtils.isPlayerInGame() || !Mouse.isButtonDown(1) || mc.currentScreen != null) return;
      if (isConsumable() || isBow() || isRod()) return;
      if (noSword.isToggled() && PlayerUtils.isPlayerHoldingWeapon()) return;

      long now = System.currentTimeMillis();
      if (now - lastClickTime < nextClickDelay) return;

      KeyBinding.onTick(rmb);
      MouseManager.addRightClick();
      lastClickTime = now;

      double jitteredCps = rCPS.getInput() + (Math.random() - 0.5D) * 1.2D;
      jitteredCps = Math.max(1.0D, jitteredCps);
      nextClickDelay = Math.max(25L, Math.round(1000.0D / jitteredCps));
   }
}
