package dev.veyra.client.player.clutch;
import java.util.function.*;
import java.util.concurrent.ThreadLocalRandom;
/** Actions run exclusively once per client tick; frame service only validates lifecycle. */
public final class PearlFireballEngine {
 public interface Actions {
  boolean valid(); int pearlSlot(); int fireballSlot(); void missing(String item);
  void capture(); void select(int slot); void use(int slot, boolean pearl); void cleanup();
  default void trace(long tick, long elapsed, String state, String action) {}
 }
 private enum State { IDLE, SELECT_PEARL, USE_PEARL, WAIT, USE_FIREBALL }
 private final Actions actions; private final LongSupplier clock; private final DoubleSupplier random; private final boolean prepareDuringDelay;
 private State state=State.IDLE; private int pearl,fireball,delay; private long started,due,lastTick=Long.MIN_VALUE;
 public PearlFireballEngine(Actions a) { this(a,System::nanoTime,()->ThreadLocalRandom.current().nextDouble()); }
 public PearlFireballEngine(Actions a,LongSupplier c,DoubleSupplier r) { this(a,c,r,false); }
 /** VoidClutch prepares Fireball during cooldown; PearlFireball retains its existing timing. */
 public PearlFireballEngine(Actions a,LongSupplier c,DoubleSupplier r,boolean prepareDuringDelay) {
  actions=a;clock=c;random=r;this.prepareDuringDelay=prepareDuringDelay;
 }
 public boolean isActive(){return state!=State.IDLE;}
 public void start(int milliseconds){
  if(isActive()||!actions.valid())return;
  pearl=actions.pearlSlot();fireball=actions.fireballSlot();
  if(pearl<0||fireball<0){if(pearl<0)actions.missing("Ender Pearl");if(fireball<0)actions.missing("Fireball");return;}
  delay=Math.max(100,Math.min(1000,VoidClutchEngine.normalizeDelay(milliseconds)+(prepareDuringDelay?0:(int)(random.getAsDouble()*21)-10)));
  started=clock.getAsLong();state=State.SELECT_PEARL;lastTick=Long.MIN_VALUE;
  try{actions.capture();advance(started);}catch(RuntimeException e){cancel();throw e;}
 }
 public void advance(long now){if(isActive()&&!actions.valid())cancel();}
 public void tick(long tick,long now){
  advance(now);if(!isActive()||lastTick==tick)return;lastTick=tick;
  try { switch(state){
   case SELECT_PEARL: actions.select(pearl);trace(now,"select-pearl");state=State.USE_PEARL;break;
   case USE_PEARL: actions.use(pearl,true);trace(now,"use-pearl");due=clock.getAsLong()+delay*1_000_000L;state=State.WAIT;break;
   case WAIT: if(prepareDuringDelay || now>=due){actions.select(fireball);trace(now,"select-fireball");state=State.USE_FIREBALL;}break;
   case USE_FIREBALL: if(now<due)break; actions.use(fireball,false);trace(now,"use-fireball");finish("completed");break;
   default: break;
  }}catch(RuntimeException e){cancel();throw e;}
 }
 private void trace(long now,String action){actions.trace(lastTick,now-started,state.name(),action);}
 private void finish(String action){if(!isActive())return;try{trace(clock.getAsLong(),action);}finally{state=State.IDLE;due=started=0;pearl=fireball=delay=0;actions.cleanup();}}
 public void cancel(){finish("cancelled");}
}
