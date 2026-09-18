package dev.veyra.client.module;

import com.google.gson.JsonObject;
import dev.veyra.client.main.Veyra;
import dev.veyra.client.module.setting.impl.TickSetting;
import dev.veyra.client.module.bind.BindAction;
import dev.veyra.client.module.bind.BindMode;
import dev.veyra.client.module.bind.BindStateMachine;
import dev.veyra.client.module.bind.BindNotificationTracker;
import dev.veyra.client.module.bind.ModuleTransitionSource;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.weavemc.api.event.EventBus;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import dev.veyra.client.module.setting.Setting;

import java.util.ArrayList;

public abstract class Module {
   protected ArrayList<Setting> settings;
   private final String moduleName;
   private final ModuleCategory moduleCategory;
   protected boolean enabled = false;
   protected boolean defaultEnabled = false;
   protected int keycode;
   protected int defaultKeycode;
   private BindMode bindMode = BindMode.TOGGLE;
   private final BindMode defaultBindMode = BindMode.TOGGLE;

   protected static Minecraft mc = Minecraft.getMinecraft();
   private final BindStateMachine bindState = new BindStateMachine();
   private final BindNotificationTracker notificationTracker = new BindNotificationTracker();
   private boolean favorite;
   private int favoriteOrder = Integer.MAX_VALUE;

   @Getter @Setter
   private String suffix;

   public Module(String name, ModuleCategory category, int keybind) {
      this.moduleName = name;
      this.moduleCategory = category;
      this.settings = new ArrayList<>();
      this.keycode = keybind;
      this.defaultKeycode = keybind;
   }

   public JsonObject getConfigAsJson(){
      JsonObject settings = new JsonObject();

      for(Setting setting : this.settings){
         JsonObject settingData = setting.getConfigAsJson();
         settings.add(setting.settingName, settingData);
      }

      JsonObject data = new JsonObject();
      data.addProperty("enabled", bindMode == BindMode.TOGGLE && enabled);
      data.addProperty("keycode", keycode);
      data.addProperty("bindMode", bindMode.name());
      data.addProperty("favorite", favorite);
      data.addProperty("favoriteOrder", favoriteOrder);
      data.add("settings", settings);

      return data;
   }

   public void applyConfigFromJson(JsonObject data){
      try {
         if (data.has("keycode") && data.get("keycode").isJsonPrimitive()) {
            this.keycode = data.get("keycode").getAsInt();
         } else {
            this.keycode = defaultKeycode;
         }
         BindMode configuredMode = BindMode.fromConfig(
                 data.has("bindMode") ? data.get("bindMode").getAsString() : null,
                 defaultBindMode);
         this.bindMode = isBindModeSupported(configuredMode) ? configuredMode : defaultBindMode;
         this.favorite = data.has("favorite") && data.get("favorite").getAsBoolean();
         this.favoriteOrder = data.has("favoriteOrder")
                 ? data.get("favoriteOrder").getAsInt() : Integer.MAX_VALUE;
         JsonObject settingsData = data.has("settings") && data.get("settings").isJsonObject()
                 ? data.getAsJsonObject("settings") : new JsonObject();
         for (Setting setting : getSettings()) {
            if (settingsData.has(setting.getName())
                    && settingsData.get(setting.getName()).isJsonObject()) {
               try {
                  setting.applyConfigFromJson(
                          settingsData.getAsJsonObject(setting.getName()));
               } catch (RuntimeException ignored) {
                  setting.resetToDefaults();
               }
            } else {
               setting.resetToDefaults();
            }
         }
         boolean configuredEnabled = data.has("enabled") && data.get("enabled").getAsBoolean();
         setToggled(bindMode == BindMode.TOGGLE && configuredEnabled, ModuleTransitionSource.CONFIG);
      } catch (RuntimeException ignored){
         resetToConfigDefaults();

      }
   }

   public void keybind() {
      boolean inputAllowed = mc.currentScreen == null && keycode != 0 && canBeEnabled();
      BindAction action = inputAllowed
              ? bindState.update(bindMode, isBindDown(keycode))
              : bindState.reset(bindMode);
      handleBindAction(action);
   }

   /**
    * Handles the exact state captured by LWJGL's input event. Reading
    * Keyboard.isKeyDown from inside that callback can still expose the
    * previous global state on some clients and lose the press edge.
    */
   public void keybindEvent(int inputCode, boolean down) {
      if (inputCode != keycode || keycode == 0) return;
      boolean inputAllowed = mc.currentScreen == null && canBeEnabled();
      BindAction action = inputAllowed
              ? bindState.update(bindMode, down)
              : bindState.reset(bindMode);
      handleBindAction(action);
   }

   private void handleBindAction(BindAction action) {
      switch (action) {
         case TOGGLE -> toggle(ModuleTransitionSource.KEYBIND);
         case ENABLE -> setToggled(true, ModuleTransitionSource.KEYBIND);
         case DISABLE -> setToggled(false, ModuleTransitionSource.KEYBIND);
         case PRESS -> onBindPressed();
         case NONE -> {
         }
      }
   }

   /** Minecraft stores mouse binds as -100 + the LWJGL mouse button. */
   private static boolean isBindDown(int keycode) {
      if (keycode < 0) {
         int mouseButton = keycode + 100;
         return mouseButton >= 0 && mouseButton < Mouse.getButtonCount()
                 && Mouse.isButtonDown(mouseButton);
      }
      return keycode < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(keycode);
   }

   public boolean canBeEnabled() {
      return true;
   }

   public void enable() {
      setToggled(true, ModuleTransitionSource.AUTO);
   }

   public void disable() {
      setToggled(false, ModuleTransitionSource.AUTO);
   }

   public void setToggled(boolean enabled) {
      setToggled(enabled, ModuleTransitionSource.AUTO);
   }

   public void setToggled(boolean enabled, ModuleTransitionSource source) {
      if (this.enabled == enabled) return;
      boolean shouldNotify = notificationTracker.transition(enabled, source);
      this.enabled = enabled;
      if (enabled) {
         EventBus.subscribe(this);
         onEnable();
      } else {
         EventBus.unsubscribe(this);
         onDisable();
      }
      if (shouldNotify && Veyra.notificationManager != null) {
         Veyra.notificationManager.postModuleState(moduleName, enabled);
      }
   }

   public String getName() {
      return this.moduleName;
   }

   public String getDisplayName() {
      return suffix == null || suffix.isBlank() ? moduleName : moduleName + " - " + suffix;
   }

   public ArrayList<Setting> getSettings() {
      return this.settings;
   }

   public void registerSetting(Setting Setting) {
      this.settings.add(Setting);
   }

   public ModuleCategory moduleCategory() {
      return this.moduleCategory;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public boolean isFavorite() {
      return favorite;
   }

   public void setFavorite(boolean favorite) {
      this.favorite = favorite;
      if (favorite && favoriteOrder == Integer.MAX_VALUE) {
         int lastOrder = -1;
         if (Veyra.moduleManager != null) {
            for (Module module : Veyra.moduleManager.getModules()) {
               if (module != this && module.favorite && module.favoriteOrder != Integer.MAX_VALUE) {
                  lastOrder = Math.max(lastOrder, module.favoriteOrder);
               }
            }
         }
         favoriteOrder = lastOrder + 1;
      }
      saveUserState();
   }

   public void toggleFavorite() {
      setFavorite(!favorite);
   }

   public int getFavoriteOrder() {
      return favoriteOrder;
   }

   public void setFavoriteOrder(int favoriteOrder) {
      this.favoriteOrder = Math.max(0, favoriteOrder);
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public void onBindPressed() {
   }

   public void toggle() {
      toggle(ModuleTransitionSource.AUTO);
   }

   public void toggle(ModuleTransitionSource source) {
      setToggled(!enabled, source);
      if (source == ModuleTransitionSource.GUI
              || source == ModuleTransitionSource.KEYBIND && bindMode == BindMode.TOGGLE) {
         saveUserState();
      }
   }

   public void guiUpdate() {
   }

   public void guiButtonToggled(TickSetting b) {
   }

   public int getKeycode() {
      return this.keycode;
   }

   public void setBind(int key) {
      BindAction release = bindState.reset(bindMode);
      if (release == BindAction.DISABLE) setToggled(false, ModuleTransitionSource.GUI);
      this.keycode = key;
      saveUserState();
   }

   public BindMode getBindMode() {
      return bindMode;
   }

   public void setBindMode(BindMode mode) {
      BindMode requested = mode == null ? defaultBindMode : mode;
      if (!isBindModeSupported(requested)) requested = defaultBindMode;
      if (bindMode == requested) return;
      BindAction release = bindState.reset(bindMode);
      if (release == BindAction.DISABLE) setToggled(false, ModuleTransitionSource.GUI);
      bindMode = requested;
      saveUserState();
   }

   public void cycleBindMode() {
      BindMode next = switch (bindMode) {
         case TOGGLE -> supportsHoldBind() ? BindMode.HOLD
                 : supportsPressBind() ? BindMode.PRESS : BindMode.TOGGLE;
         case HOLD -> supportsPressBind() ? BindMode.PRESS : BindMode.TOGGLE;
         case PRESS -> BindMode.TOGGLE;
      };
      setBindMode(next);
   }

   public boolean supportsHoldBind() {
      return true;
   }

   public boolean supportsPressBind() {
      return false;
   }

   private boolean isBindModeSupported(BindMode mode) {
      return mode == BindMode.TOGGLE
              || mode == BindMode.HOLD && supportsHoldBind()
              || mode == BindMode.PRESS && supportsPressBind();
   }

   public void resetToDefaults() {
      resetToDefaults(ModuleTransitionSource.GUI);
      saveUserState();
   }

   public void resetToConfigDefaults() {
      favorite = false;
      favoriteOrder = Integer.MAX_VALUE;
      resetToDefaults(ModuleTransitionSource.CONFIG);
   }

   private void resetToDefaults(ModuleTransitionSource source) {
      bindState.reset(bindMode);
      this.keycode = defaultKeycode;
      this.bindMode = defaultBindMode;

      for(Setting setting : this.settings){
         setting.resetToDefaults();
      }
      this.setToggled(defaultEnabled, source);
   }

   private static void saveUserState() {
      if (Veyra.configManager != null) {
         Veyra.configManager.save();
      }
   }

   public enum ModuleCategory {
      Combat, Movement, Player, Render, Client
   }
}
