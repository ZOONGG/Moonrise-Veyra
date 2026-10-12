package dev.veyra.client.player.clutch;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.function.LongSupplier;
import java.nio.file.*;

class VoidClutchEngineTest {
 static class Fake extends PearlFireballEngineTest.Fake implements VoidClutchEngine.Actions {
  LongSupplier pending;int captures,aimCounts=1050;List<Long> times=new ArrayList<>();List<Integer> deltas=new ArrayList<>();
  final VoidClutchEngine clutch=new VoidClutchEngine(this,()->now);
  public void capture(){captures++;}
  public int upwardMouseCounts(){return aimCounts;}
  public void beginAim(LongSupplier delta){pending=delta;}
  public void clearAim(){pending=null;}
  public void select(int s){super.select(s);times.add(now);}
  public void use(int s,boolean p){super.use(s,p);times.add(now);}
  void mouse(long ns){now=ns;if(pending!=null)deltas.add((int)pending.getAsLong());}
  void tickAt(long ns){now=ns;clutch.tick(++tick,now);}
  void aimed(){clutch.start();mouse(0);mouse(10_000_000);mouse(25_000_000);mouse(49_999_999);mouse(50_000_000);}
 }
 @Test void exactSequenceMinimumDeadlinesAndSingleUses(){
  Fake f=new Fake();f.aimed();assertTrue(f.log.isEmpty());
  f.tickAt(50_000_000);assertEquals(List.of("select2"),f.log);
  f.tickAt(99_999_999);assertEquals(1,f.log.size());
  f.tickAt(100_000_000);assertEquals(List.of("select2","pearl"),f.log);
  f.tickAt(129_999_999);assertEquals(2,f.log.size());
  f.tickAt(130_000_000);assertEquals(List.of("select2","pearl","select7"),f.log);
  f.tickAt(159_999_999);assertEquals(3,f.log.size());
  f.tickAt(160_000_000);assertEquals(List.of("select2","pearl","select7","fireball"),f.log);
  assertEquals(List.of(50_000_000L,100_000_000L,130_000_000L,160_000_000L),f.times);
  assertEquals(7,f.slot);assertNull(f.pending);assertFalse(f.clutch.isActive());assertEquals(1,f.cleanups);
  f.tickAt(999_000_000);assertEquals(4,f.log.size());
 }
 @Test void rawAimRunsOnMouseUpdatesFor50msWithoutTargetPitch(){
  Fake f=new Fake();f.clutch.start();f.tickAt(25_000_000);assertTrue(f.deltas.isEmpty());
  f.mouse(25_000_000);assertEquals(525,f.deltas.get(0));
  f.mouse(25_000_000);assertEquals(0,f.deltas.get(1));
  f.mouse(49_000_000);f.tickAt(49_000_000);assertTrue(f.log.isEmpty());
  f.mouse(50_000_000);assertEquals(1050,f.deltas.stream().mapToInt(i->i).sum());
  f.mouse(90_000_000);assertEquals(0,f.deltas.get(f.deltas.size()-1));
  f.tickAt(90_000_000);assertEquals(List.of("select2"),f.log);
 }
 @Test void duplicateTickAndFrameCallbacksCannotDispatchActions(){
  Fake f=new Fake();f.aimed();f.tickAt(50_000_000);
  for(int i=0;i<10;i++){f.clutch.tick(f.tick,100_000_000);f.clutch.advance(900_000_000);}
  assertEquals(List.of("select2"),f.log);
  f.tickAt(100_000_000);assertEquals(List.of("select2","pearl"),f.log);
 }
 @Test void fullUpwardAimReachesVanillaClampAtEverySensitivityAndInversion(){
  for(float sensitivity:new float[]{0,.05f,.1f,.25f,.28f,.5f,.56f,1})
   for(float startingPitch:new float[]{-90,-89.999f,-45,0,30,90})
    for(boolean inverted:new boolean[]{false,true}){
     Fake f=new Fake();f.aimCounts=NativeMouseDelta.fullUpwardCounts(startingPitch,sensitivity,inverted);
     float pitch=startingPitch,gain=NativeMouseDelta.sensitivityGain(sensitivity);
     f.clutch.start();
     for(int frame=1;frame<=10;frame++){
      f.mouse(frame*5_000_000L);
      int delta=f.deltas.get(f.deltas.size()-1);
      // EntityRenderer scales the raw input; Entity.setAngles subtracts Y * .15.
      float scaled=delta*gain*(inverted?-1:1);
      pitch=(float)(pitch-scaled*.15D);pitch=Math.max(-90,Math.min(90,pitch));
     }
     assertEquals(-90f,pitch,"sensitivity="+sensitivity+", start="+startingPitch+", inverted="+inverted);
     assertEquals(f.aimCounts,f.deltas.stream().mapToInt(i->i).sum());
     f.tickAt(50_000_000);assertEquals(List.of("select2"),f.log);
    }
 }
 @Test void secondBindIsIgnored(){
  Fake f=new Fake();f.clutch.start();f.mouse(25_000_000);f.clutch.start();
  assertEquals(1,f.captures);f.mouse(50_000_000);f.tickAt(50_000_000);
  assertEquals(1050,f.deltas.stream().mapToInt(i->i).sum());
  assertEquals(List.of("select2"),f.log);
 }
 @Test void cancelDisconnectDeathClearPendingAtEveryStage(){
  for(int stage=0;stage<5;stage++)for(boolean invalid:new boolean[]{true,false}){
   Fake f=new Fake();f.clutch.start();
   if(stage>0){f.mouse(50_000_000);f.tickAt(50_000_000);}
   if(stage>1)f.tickAt(100_000_000);
   if(stage>2)f.tickAt(130_000_000);
   if(stage>3)f.tickAt(159_000_000);
   int count=f.log.size();
   if(invalid){f.valid=false;f.clutch.advance(f.now);}else f.clutch.cancel();
   f.clutch.cancel();f.tickAt(900_000_000);f.mouse(999_000_000);
   assertEquals(count,f.log.size());assertNull(f.pending);assertFalse(f.clutch.isActive());assertEquals(1,f.cleanups);
  }
 }
 @Test void missingItemsProduceNoInput(){
  for(boolean pearl:new boolean[]{true,false}){Fake f=new Fake();if(pearl)f.pearl=-1;else f.fireball=-1;
   f.clutch.start();assertFalse(f.clutch.isActive());assertNull(f.pending);assertEquals(0,f.captures);assertEquals(1,f.log.size());}
 }
 @Test void failureCancelsRemainingActions(){
  Fake f=new Fake();f.aimed();f.tickAt(50_000_000);f.fail=true;
  assertThrows(IllegalStateException.class,()->f.tickAt(100_000_000));
  assertFalse(f.clutch.isActive());assertNull(f.pending);assertEquals(1,f.cleanups);
 }
 @Test void everyDetectedHotbarPairUsesDetectedSlots(){
  for(int p=0;p<9;p++)for(int b=0;b<9;b++){if(p==b)continue;Fake f=new Fake();f.pearl=p;f.fireball=b;
   f.aimed();f.tickAt(50_000_000);f.tickAt(100_000_000);f.tickAt(130_000_000);f.tickAt(160_000_000);
   assertEquals(List.of("select"+p,"pearl","select"+b,"fireball"),f.log);assertEquals(b,f.slot);}
 }
 @Test void nativeInputSourceHasNoRotationSlotWritesPacketsTimerOrRestores() throws Exception {
  String module=Files.readString(Path.of("src/main/java/dev/veyra/client/module/modules/player/VoidClutch.java"));
  String engine=Files.readString(Path.of("src/main/java/dev/veyra/client/player/clutch/VoidClutchEngine.java"));
  String input=Files.readString(Path.of("src/main/java/dev/veyra/client/player/clutch/ClutchInput.java"));
  String hook=Files.readString(Path.of("src/main/java/dev/veyra/weave/mixins/ClutchMouseInputMixin.java"));
  for(String s:List.of(module,engine,input,hook)){
   assertFalse(s.matches("(?s).*rotation(Pitch|Yaw)\\s*=.*"));
   assertFalse(s.matches("(?s).*currentItem\\s*=.*"));
   for(String forbidden:List.of("sendUseItem","sendPacket","Thread.sleep","TimerRuntimeController","Robot","restore","ThreadLocalRandom"))assertFalse(s.contains(forbidden),forbidden);
  }
  assertTrue(input.contains("KeyBinding.onTick"));assertTrue(input.contains("veyra$rightClick"));
  assertTrue(module.contains("input.tapHotbar(mc, slot)"));
  assertTrue(input.contains("KeyBinding.setKeyBindState(key,true)"));
  assertTrue(input.contains("finally{KeyBinding.setKeyBindState(key,held);}"));
  assertTrue(hook.contains("@Mixin(MouseHelper.class)"));assertTrue(hook.contains("@At(\"RETURN\")"));
  assertFalse(input.contains("targetPitch"));assertFalse(input.contains("upwardCounts"));
  assertFalse(module.contains("SliderSetting"));
 }
}
