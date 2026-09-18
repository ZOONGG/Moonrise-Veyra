package dev.veyra.client.module.modules.combat;

import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.SliderSetting;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.weave.mixins.IS12PacketEntityVelocity;
import net.minecraft.entity.Entity;
import net.weavemc.api.event.PacketEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

@SuppressWarnings("unused")
public class Velocity extends Module {
   public static SliderSetting horizontal, vertical, chance;
   public static ComboSetting<velomode> velomodes;

   public Velocity() {
      super("Velocity", Module.ModuleCategory.Combat, 0);
      this.registerSetting(horizontal = new SliderSetting("Horizontal", 90.0D, 0.0D, 200.0D, 1.0D));
      this.registerSetting(vertical = new SliderSetting("Vertical", 100.0D, 0.0D, 200.0D, 1.0D));
      this.registerSetting(chance = new SliderSetting("Chance", 100.0D, 0.0D, 100.0D, 1.0D));
      this.registerSetting(velomodes = new ComboSetting<>("Mode", velomode.Normal));
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive e) {
      if (mc.thePlayer == null) return;
      if (e.getPacket() instanceof S12PacketEntityVelocity packet && packet.getEntityID() == mc.thePlayer.entityId) {
         if (velomodes.getMode() == velomode.Cancel) {
            e.setCancelled(true);
            return;
         }
      if (velomodes.getMode() == velomode.Minemen && mc.thePlayer.onGround) return;
         if (chance.getInput() != 100.0D && Math.random() * 100.0D >= chance.getInput()) {
            return;
         }
         IS12PacketEntityVelocity mutablePacket = (IS12PacketEntityVelocity) packet;
         mutablePacket.setMotionX((int) (packet.getMotionX() * horizontal.getInput() / 100.0D));
         mutablePacket.setMotionZ((int) (packet.getMotionZ() * horizontal.getInput() / 100.0D));
         mutablePacket.setMotionY((int) (packet.getMotionY() * vertical.getInput() / 100.0D));
      }
   }

   public enum velomode {
      Normal, Cancel, Minemen
   }
}
