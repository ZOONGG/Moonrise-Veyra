package dev.veyra.client.module.modules.player;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.DescriptionSetting;
import dev.veyra.client.utils.world.WorldUtils;
import dev.veyra.weave.events.UpdateEvent;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.weavemc.api.event.SubscribeEvent;

public class BedNuker extends Module {

   public BedNuker() {
      super("BedNuker", ModuleCategory.Player, 0);
      this.registerSetting(new DescriptionSetting("Breaks beds."));
   }

   @SubscribeEvent
   public void onUpdate(UpdateEvent e) {
      if (!e.isPre()) return;
      if (mc.thePlayer != null && mc.theWorld != null) {
         BlockPos bed = WorldUtils.findNearestBedPos(mc.theWorld, mc.thePlayer.getPosition(), 6);
         if (bed != null) breakBlock(bed);
      }
   }

   private void breakBlock(BlockPos pos) {
      if (mc.thePlayer != null && pos != null) {
         try {
            mc.playerController.onPlayerDamageBlock(pos, EnumFacing.NORTH);
            mc.thePlayer.swingItem();
         } catch (Exception ignored) {}
      }
   }
}
