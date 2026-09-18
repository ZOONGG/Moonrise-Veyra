package dev.veyra.client.module.modules.render;

import dev.veyra.client.clickgui.Theme;
import dev.veyra.client.module.Module;
import dev.veyra.client.utils.Utils;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.weavemc.api.event.RenderWorldEvent;
import net.weavemc.api.event.SubscribeEvent;

import java.util.Iterator;

public class ChestESP extends Module {
   public ChestESP() {
      super("ChestESP", ModuleCategory.Render, 0);
   }

   @SubscribeEvent
   public void onRenderWorldLast(RenderWorldEvent e) {
      if (PlayerUtils.isPlayerInGame()) {
         Iterator<TileEntity> var3 = mc.theWorld.loadedTileEntityList.iterator();

         while(true) {
            TileEntity te;
            do {
               if (!var3.hasNext()) {
                  return;
               }

               te = var3.next();
            } while(!(te instanceof TileEntityChest) && !(te instanceof TileEntityEnderChest));

            Utils.HUD.re(te.getPos(), Theme.getMainColor().getRGB(), true);
         }
      }
   }
}
