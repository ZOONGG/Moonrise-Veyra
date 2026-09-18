package dev.veyra.client.main;

import dev.veyra.client.utils.player.PlayerUtils;
import dev.veyra.client.command.RequeueCommand;
import dev.veyra.client.compat.BwhOverlayRefreshCompat;
import dev.veyra.client.compat.BwhObsidianAlertCompat;
import dev.veyra.client.compat.BwhGearAlertCompat;
import dev.veyra.client.compat.BwhOverlayTeamColorCompat;
import dev.veyra.client.module.modules.render.BedwarsBaseAnchorTracker;
import net.minecraft.client.Minecraft;
import net.weavemc.api.command.CommandBus;
import net.weavemc.api.event.EventBus;
import net.weavemc.api.event.KeyboardEvent;
import net.weavemc.api.event.MouseEvent;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;
import dev.veyra.client.clickgui.ClickGui;
import dev.veyra.client.config.ClientConfig;
import dev.veyra.client.config.ConfigManager;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.ModuleManager;
import dev.veyra.client.notification.NotificationManager;
import dev.veyra.client.coordination.input.InputLeaseManager;
import dev.veyra.client.utils.game.MouseManager;
import dev.veyra.client.utils.Utils;

public class Veyra {

   public static boolean debugger = false;
   public static ConfigManager configManager;
   public static ClientConfig clientConfig;
   public static ModuleManager moduleManager;
   public static ClickGui clickGui;
   public static NotificationManager notificationManager;
   public static BedwarsBaseAnchorTracker bedwarsBaseAnchorTracker;

   public static void init() {
      EventBus.subscribe(new Veyra());
      EventBus.subscribe(new MouseManager());
      EventBus.subscribe(new InputLeaseManager());
      EventBus.subscribe(new BwhOverlayRefreshCompat());
      EventBus.subscribe(new BwhObsidianAlertCompat());
      EventBus.subscribe(new BwhGearAlertCompat());
      EventBus.subscribe(new BwhOverlayTeamColorCompat());
      bedwarsBaseAnchorTracker = new BedwarsBaseAnchorTracker();
      EventBus.subscribe(bedwarsBaseAnchorTracker);
      notificationManager = new NotificationManager();
      EventBus.subscribe(notificationManager);
      RequeueCommand requeueCommand = new RequeueCommand();
      CommandBus.register(requeueCommand);
      EventBus.subscribe(requeueCommand);

      moduleManager = new ModuleManager();
      clickGui = new ClickGui();
      configManager = new ConfigManager();
      clientConfig = new ClientConfig();
      clientConfig.applyConfig();
   }

   @SubscribeEvent
   public void onKeyboard(KeyboardEvent event) {
      if (!PlayerUtils.isPlayerInGame() || moduleManager == null) return;
      for (Module module : moduleManager.getModules()) {
         module.keybindEvent(event.getKeyCode(), event.getKeyState());
      }
   }

   /** Mouse events make side-button binds respond immediately, like keyboard binds. */
   @SubscribeEvent
   public void onMouse(MouseEvent event) {
      if (!PlayerUtils.isPlayerInGame() || moduleManager == null
              || event.getButton() < 0) return;
      int mouseKeyCode = event.getButton() - 100;
      for (Module module : moduleManager.getModules()) {
         module.keybindEvent(mouseKeyCode, event.getButtonState());
      }
   }

   /**
    * LWJGL events can miss a short press between event callbacks. Polling once
    * per game tick guarantees that every held edge reaches the bind state
    * machine; keyboard/mouse events above still provide immediate response.
    */
   @SubscribeEvent
   public void onClientTick(TickEvent.Pre event) {
      pollBinds();
      if (PlayerUtils.isPlayerInGame()
              && Minecraft.getMinecraft().currentScreen instanceof ClickGui) {
         for (Module module : moduleManager.getModules()) module.guiUpdate();
      }
   }

   private void pollBinds() {
      if (!PlayerUtils.isPlayerInGame() || moduleManager == null) return;
      for (Module module : moduleManager.getModules()) module.keybind();
   }
}
