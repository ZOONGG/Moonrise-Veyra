package dev.veyra.client.player.clutch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import dev.veyra.weave.mixins.ClutchMinecraftAccess;
/** One-shot vanilla hotbar input and right-click entry point, with no packet generation. */
public final class ClutchInput {
 private static java.util.function.LongSupplier rawAim;
 private static int lastDelta;
 private KeyBinding queuedSlot;
 public void select(Minecraft mc,int slot){
  clearSlot();queuedSlot=mc.gameSettings.keyBindsHotbar[slot];
  debug(mc,"select-input",mc.thePlayer.inventory.currentItem,slot);
  KeyBinding.onTick(queuedSlot.getKeyCode());
 }
 /** A complete hotbar tap, retaining any key the user was already holding. */
 public void tapHotbar(Minecraft mc,int slot){
  clearSlot();queuedSlot=mc.gameSettings.keyBindsHotbar[slot];
  int key=queuedSlot.getKeyCode();boolean held=queuedSlot.isKeyDown();
  KeyBinding.setKeyBindState(key,true);
  try{KeyBinding.onTick(key);}finally{KeyBinding.setKeyBindState(key,held);}
 }
 public void clearSlot(){if(queuedSlot!=null){while(queuedSlot.isPressed()){}queuedSlot=null;}}
 /** One vanilla RMB action; no held use binding, repeat, controller call or custom packet. */
 public void use(Minecraft mc){
  clearSlot();int before=mc.thePlayer.inventory.currentItem;
  ((ClutchMinecraftAccess)mc).veyra$rightClick();
  debug(mc,"vanilla-use-invocation",before,mc.thePlayer.inventory.currentItem);
 }
 private static void debug(Minecraft mc,String action,int before,int after){
  if(Boolean.getBoolean("veyra.clutch.debug"))System.out.printf("clutch nano=%d playerTick=%d action=%s slotBefore=%d slotAfterOrRequested=%d source=client-tick-pre%n",System.nanoTime(),mc.thePlayer.ticksExisted,action,before,after);
 }
 public static boolean hasAim(){return rawAim!=null;}
 public static void clearAim(){rawAim=null;lastDelta=0;}
 public static void beginRawAim(java.util.function.LongSupplier delta){rawAim=delta;lastDelta=0;}
 /** Appended once at MouseHelper RETURN, before vanilla camera processing. */
 public static int mouseDelta(Minecraft mc,int physical){
  lastDelta=rawAim==null?0:(int)rawAim.getAsLong();return physical+lastDelta;
 }
 public static int lastDelta(){return lastDelta;}
}
