package dev.veyra.client.module.modules.client;

import dev.veyra.client.clickgui.ClickGui;
import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.Module;
import dev.veyra.client.module.setting.impl.ComboSetting;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.utils.Utils;
import dev.veyra.client.utils.player.PlayerUtils;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.weavemc.api.event.SubscribeEvent;
import net.weavemc.api.event.TickEvent;

public class ClickGuiModule extends Module {
    public static ComboSetting<Colors> clientTheme;
    public static TickSetting notifications;

    public ClickGuiModule() {
        super("ClickGui", Module.ModuleCategory.Client, 28);
        this.registerSetting(clientTheme = new ComboSetting<>("Theme", Colors.Veyra));
        this.registerSetting(notifications = new TickSetting("Bind notifications", true));
    }

    public static boolean notificationsEnabled() {
        return notifications == null || notifications.isToggled();
    }

    private final KeyBinding[] moveKeys = new KeyBinding[]{
            mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindJump, mc.gameSettings.keyBindSprint
    };

    @Override
    public void onEnable() {
        if (PlayerUtils.isPlayerInGame() && mc.currentScreen != Veyra.clickGui) {
            mc.displayGuiScreen(Veyra.clickGui);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent e) {
        boolean guiOwnsKeyboard = mc.currentScreen instanceof ClickGui;
        for (KeyBinding bind : moveKeys) {
            bind.pressed = !guiOwnsKeyboard && GameSettings.isKeyDown(bind);
        }
    }

    @Override
    public void onDisable() {
        if (PlayerUtils.isPlayerInGame() && mc.currentScreen instanceof ClickGui) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public boolean supportsHoldBind() {
        return false;
    }

    public enum Colors {
        Veyra, Aurora, Glacier, Ember, Lime, Mono
    }
}
