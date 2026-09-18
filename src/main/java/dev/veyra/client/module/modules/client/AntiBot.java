package dev.veyra.client.module.modules.client;

import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.player.AntiBotNameClassifier;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

import java.util.HashMap;

public class AntiBot extends Module {
   private static final HashMap<EntityPlayer, Long> newEnt = new HashMap<>();
   public static TickSetting wait;

   public AntiBot() {
      super("AntiBot", ModuleCategory.Client, 0);
      this.registerSetting(wait = new TickSetting("Wait 80 ticks", false));
   }

   @Override
   public void onDisable() {
      newEnt.clear();
   }

   @SubscribeEvent
   public void onTick(TickEvent e) {
      if (!wait.isToggled() || !PlayerUtils.isPlayerInGame()) {
         newEnt.clear();
         return;
      }

      long now = System.currentTimeMillis();
      for (EntityPlayer player : mc.theWorld.playerEntities) {
         if (player != mc.thePlayer) newEnt.putIfAbsent(player, now);
      }
      newEnt.entrySet().removeIf(entry ->
              entry.getValue() < now - 4000L || !entry.getKey().isEntityAlive());
   }

   public static boolean bot(Entity en) {
      if(!PlayerUtils.isPlayerInGame()) return false;
      Module antiBot = Veyra.moduleManager.getModuleByClazz(AntiBot.class);
      if (antiBot != null && !antiBot.isEnabled()) {
         return false;
      } else if (wait.isToggled() && !newEnt.isEmpty() && newEnt.containsKey(en)) {
         return true;
      } else if (en.getName().startsWith("§c")) {
         return true;
      } else {
         String n = en.getDisplayName().getUnformattedText();
         String formatted = en.getDisplayName().getFormattedText();
         if (AntiBotNameClassifier.isKnownNpc(formatted, n)) return true;
         if (en instanceof EntityPlayer player
                 && mc.getNetHandler() != null
                 && mc.getNetHandler().getPlayerInfo(player.getUniqueID()) == null) return true;
         if (n.isEmpty() && en.getName().isEmpty()) {
            return true;
         }

         if (n.length() == 10) {
            int num = 0;
            int let = 0;
            char[] var4 = n.toCharArray();

            for (char c : var4) {
               if (Character.isLetter(c)) {
                  if (Character.isUpperCase(c)) {
                     return false;
                  }

                  ++let;
               } else {
                  if (!Character.isDigit(c)) {
                     return false;
                  }

                  ++num;
               }
            }

            return num >= 2 && let >= 2;
         }

         return false;
       }
   }
}
