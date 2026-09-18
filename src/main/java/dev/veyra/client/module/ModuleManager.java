package dev.veyra.client.module;

import dev.veyra.client.module.modules.client.AntiBot;
import dev.veyra.client.module.modules.client.ClickGuiModule;
import dev.veyra.client.module.modules.combat.*;
import dev.veyra.client.module.modules.movement.*;
import dev.veyra.client.module.modules.player.*;
import dev.veyra.client.module.modules.render.*;
import net.minecraft.client.gui.FontRenderer;
import dev.veyra.client.module.modules.client.ArrayListModule;
import dev.veyra.client.module.modules.movement.Timer;
import dev.veyra.client.utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ModuleManager {
   private final List<Module> modules = new ArrayList<>();
   public static boolean initialized = false;

   public ModuleManager() {
      if (initialized) return;

      addModule(new AutoClicker());
      // Clutch remains in source for future work, but is intentionally not
      // registered: it cannot appear in ClickGUI, configs, keybinds or events.
      addModule(new RightClicker());
      addModule(new AimAssist());
      addModule(new ClickAssist());
      addModule(new Reach());
      addModule(new Velocity());
      addModule(new JumpReset());
      addModule(new TargetFilter());
      addModule(new HitSelect());
      addModule(new InvMove());
      addModule(new NoHitDelay());
      addModule(new NoJumpDelay());
      addModule(new Backtrack());
      addModule(new KeepSprint());
      addModule(new NoSlow());
      addModule(new Timer());
      addModule(new AntiAFK());
      addModule(new AutoTool());
      addModule(new SafeWalk());
      addModule(new AutoBlockIn());
      addModule(new AutoFish());
      addModule(new BedNuker());
      addModule(new FastPlace());
      addModule(new NoUseDelay());
      addModule(new AntiBot());
      addModule(new Chams());
      addModule(new ChestESP());
      addModule(new Nametags());
      addModule(new PlayerESP());
      addModule(new BedESP());
      addModule(new BedPlates());
      addModule(new MurderFinder());
      addModule(new ArrayListModule());
      addModule(new ClickGuiModule());
      addModule(new ClosetSpeed());
      addModule(new Blink());
      addModule(new NoRotate());
      addModule(new Bhop());
      addModule(new Killaura());
      addModule(new AntiVoid());
      addModule(new Sprint());
      addModule(new AutoDodge());
      addModule(new FakeLag());
      addModule(new Flight());
      addModule(new Freecam());
      addModule(new AutoBlock());
      addModule(new Strafe());
      addModule(new Criticals());
      addModule(new Stealer());
      addModule(new Manager());
      addModule(new WTap());

      initialized = true;
   }

   private void addModule(Module m) {
      modules.add(m);
      modules.sort(Comparator.comparing(module -> module.getName().toLowerCase(Locale.ROOT)));
   }

   public Module getModuleByClazz(Class<? extends Module> c) {
      if (!initialized) return null;

      for (Module module : modules) {
         if (module.getClass().equals(c))
            return module;
      }
      return null;
   }


   public List<Module> getModules() {
      return modules;
   }

   public List<Module> getModulesInCategory(Module.ModuleCategory categ) {
      java.util.ArrayList<Module> modulesOfCat = new java.util.ArrayList<>();

      for (Module mod : modules) {
         if (mod.moduleCategory().equals(categ)) {
            modulesOfCat.add(mod);
         }
      }

      return modulesOfCat;
   }

   public void sort() {
      if (ArrayListModule.sortMode != null
              && ArrayListModule.sortMode.getMode() == ArrayListModule.SortMode.Alphabetical) {
         modules.sort(Comparator.comparing(Module::getName));
      } else {
         modules.sort((o1, o2) -> Utils.mc.fontRendererObj.getStringWidth(o2.getDisplayName()) - Utils.mc.fontRendererObj.getStringWidth(o1.getDisplayName()));
      }

   }

   public void sortLongShort() {
      modules.sort(Comparator.comparingInt(o2 -> Utils.mc.fontRendererObj.getStringWidth(o2.getDisplayName())));
   }

   public void sortShortLong() {
      modules.sort((o1, o2) -> Utils.mc.fontRendererObj.getStringWidth(o2.getDisplayName()) - Utils.mc.fontRendererObj.getStringWidth(o1.getDisplayName()));
   }

   public int getLongestActiveModule(FontRenderer fr) {
      int length = 0;
      for(Module mod : modules) {
         if(mod.isEnabled()){
            if(fr.getStringWidth(mod.getDisplayName()) > length){
               length = fr.getStringWidth(mod.getDisplayName());
            }
         }
      }
      return length;
   }

   public int getBoxHeight(FontRenderer fr, int margin) {
      int length = 0;
      for(Module mod : modules) {
         if(mod.isEnabled()){
            length += fr.FONT_HEIGHT + margin;
         }
      }
      return length;
   }
}
