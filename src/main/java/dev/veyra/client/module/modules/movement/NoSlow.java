package dev.veyra.client.module.modules.movement;

import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.player.PlayerUtils;
import dev.veyra.weave.events.SlowdownEvent;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.SubscribeEvent;
import dev.veyra.client.module.Module;

@SuppressWarnings("unused")
public class NoSlow extends Module {
   public static SliderSetting speed;
   public static TickSetting autosprint, noweapons, noconsumables;
   public static ComboSetting<modes> mode;
   public NoSlow() {
      super("NoSlow", ModuleCategory.Movement, 0);
      this.registerSetting(new DescriptionSetting("Removes item-use movement slowdown."));
      this.registerSetting(speed = new SliderSetting("Slowdown %", 0.0D, 0.0D, 80.0D, 1.0D));
      this.registerSetting(autosprint = new TickSetting("Allow Sprint", false));
      this.registerSetting(noweapons = new TickSetting("Blacklist Weapons", false));
      this.registerSetting(noconsumables = new TickSetting("Blacklist Consumables", false));
      this.registerSetting(mode = new ComboSetting<>("Mode", modes.Regular));
   }

   @SubscribeEvent
   public void onSlowdown(SlowdownEvent e) {
      if (!PlayerUtils.isPlayerInGame() || mode.getMode() != modes.Regular) return;
      if (noweapons.isToggled() && PlayerUtils.isPlayerHoldingWeapon()) return;
      if (noconsumables.isToggled() && consumableCheck()) return;
      e.setCancelled(true);
      mc.thePlayer.movementInput.moveForward *= (100.0F - (float) speed.getInput()) / 100.0F;
      mc.thePlayer.movementInput.moveStrafe *= (100.0F - (float) speed.getInput()) / 100.0F;
      if (autosprint.isToggled() && PlayerUtils.isPlayerMoving()) mc.thePlayer.setSprinting(true);
   }

   public static boolean consumableCheck() {
      if (mc.thePlayer.getHeldItem() != null) {
         return noconsumables.isToggled() && (mc.thePlayer.getHeldItem().getItem() instanceof ItemFood || mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion);
      } else return false;
   }

   @SubscribeEvent
   public void onPacket(PacketEvent.Send e) {
      if (!PlayerUtils.isPlayerInGame() || mode.getMode() != modes.NoItemRelease) return;
      if (noweapons.isToggled() && PlayerUtils.isPlayerHoldingWeapon()) return;
      if (noconsumables.isToggled() && consumableCheck()) return;
      if (e.getPacket() instanceof C07PacketPlayerDigging && ((C07PacketPlayerDigging) e.getPacket()).getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
         e.setCancelled(true);
      }
   }
   public enum modes {
      Regular, NoItemRelease
   }
}
