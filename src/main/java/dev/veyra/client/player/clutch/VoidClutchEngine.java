package dev.veyra.client.player.clutch;
import java.util.function.*;
/** Full upward input macro: mouse updates emit raw counts, ticks dispatch actions. */
public final class VoidClutchEngine {
 public static final int MIN_DELAY_MS=100,MAX_DELAY_MS=1000,DEFAULT_DELAY_MS=250;
 public interface Actions extends PearlFireballEngine.Actions {int upwardMouseCounts();void beginAim(LongSupplier delta);void clearAim();default void traceMouse(long tick,long elapsed,int delta){} }
 private enum State {Idle,AimInput,SelectPearl,WaitAfterPearlSelect,UsePearl,WaitBeforeFireballSelect,SelectFireball,WaitAfterFireballSelect,UseFireball,Complete}
 private final Actions actions;private final LongSupplier clock;
 private State state=State.Idle;private long started,due,lastTick=Long.MIN_VALUE;private int pearl,fireball,emitted,aimCounts;
 public VoidClutchEngine(Actions a){this(a,System::nanoTime);}
 public VoidClutchEngine(Actions a,LongSupplier c){actions=a;clock=c;}
 public static int normalizeDelay(int ms){return ms<MIN_DELAY_MS?DEFAULT_DELAY_MS:Math.min(MAX_DELAY_MS,ms);}
 public static int findHotbar(IntPredicate match){for(int i=0;i<9;i++)if(match.test(i))return i;return -1;}
 public boolean isActive(){return state!=State.Idle;}
 public void start(){
  if(isActive()||!actions.valid())return;
  pearl=actions.pearlSlot();fireball=actions.fireballSlot();
  if(pearl<0||fireball<0){if(pearl<0)actions.missing("Ender Pearl");if(fireball<0)actions.missing("Fireball");return;}
  started=clock.getAsLong();emitted=0;lastTick=Long.MIN_VALUE;state=State.AimInput;
  try{actions.capture();aimCounts=actions.upwardMouseCounts();actions.beginAim(this::mouseDelta);trace(started,"mouse-begin");}catch(RuntimeException e){cancel();throw e;}
 }
 /** Signed raw counts calculated for the starting pitch, sensitivity and inversion. */
 public long mouseDelta(){
  if(state!=State.AimInput)return 0;
  if(!actions.valid()){cancel();return 0;}
  double progress=Math.max(0,Math.min(1,(double)(clock.getAsLong()-started)/50_000_000L));
  int target=(int)Math.round(aimCounts*progress),delta=target-emitted;emitted=target;
  actions.traceMouse(lastTick,clock.getAsLong()-started,delta);
  if(progress>=1)state=State.SelectPearl;
  return delta;
 }
 public void advance(long now){if(isActive()&&!actions.valid())cancel();}
 public void tick(long tick,long now){
  advance(now);if(!isActive()||lastTick==tick)return;lastTick=tick;
  try{
   if(state==State.WaitAfterPearlSelect&&now>=due)state=State.UsePearl;
   if(state==State.WaitBeforeFireballSelect&&now>=due)state=State.SelectFireball;
   if(state==State.WaitAfterFireballSelect&&now>=due)state=State.UseFireball;
   switch(state){
    case SelectPearl:actions.clearAim();actions.select(pearl);trace(now,"hotbar-pearl-press/release");due=clock.getAsLong()+50_000_000L;state=State.WaitAfterPearlSelect;break;
    case UsePearl:actions.use(pearl,true);trace(now,"RMB-pearl-press/release");due=clock.getAsLong()+30_000_000L;state=State.WaitBeforeFireballSelect;break;
    case SelectFireball:actions.select(fireball);trace(now,"hotbar-fireball-press/release");due=clock.getAsLong()+30_000_000L;state=State.WaitAfterFireballSelect;break;
    case UseFireball:actions.use(fireball,false);trace(now,"RMB-fireball-press/release");state=State.Complete;finish(now,"complete");break;
    default:trace(now,"wait");break;
   }
  }catch(RuntimeException e){cancel();throw e;}
 }
 private void trace(long now,String action){actions.trace(lastTick,now-started,state.name(),action);}
 private void finish(long now,String action){if(!isActive())return;try{trace(now,action);}finally{state=State.Idle;due=0;emitted=0;actions.clearAim();actions.cleanup();}}
 public void cancel(){finish(clock.getAsLong(),"cancelled");}
}
