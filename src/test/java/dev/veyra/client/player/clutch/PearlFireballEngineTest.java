package dev.veyra.client.player.clutch;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PearlFireballEngineTest {
 static class Fake implements PearlFireballEngine.Actions {
  long now,tick;int slot=4,pearl=2,fireball=7,cleanups;boolean valid=true,fail;double random=.5;
  List<String> log=new ArrayList<>();List<Long> ticks=new ArrayList<>();
  final PearlFireballEngine engine=new PearlFireballEngine(this,()->now,()->random);
  public boolean valid(){return valid;}public int pearlSlot(){return pearl;}public int fireballSlot(){return fireball;}
  public void missing(String s){log.add(s);}public void capture(){}
  public void select(int s){slot=s;log.add("select"+s);ticks.add(tick);}
  public void use(int s,boolean p){assertEquals(s,slot);if(fail)throw new IllegalStateException();log.add(p?"pearl":"fireball");ticks.add(tick);}
  public void cleanup(){cleanups++;}void step(long ns){now+=ns;engine.tick(++tick,now);}
 }
 @Test void orderSeparateTicksExactlyOneUseDelayAndNoRestore(){
  Fake f=new Fake();f.engine.start(250);assertTrue(f.log.isEmpty());f.step(0);f.engine.tick(f.tick,f.now);assertEquals(List.of("select2"),f.log);
  f.step(50_000_000);f.engine.start(100);f.engine.advance(f.now+999_000_000);assertEquals(List.of("select2","pearl"),f.log);
  f.step(249_999_999);assertEquals(2,f.log.size());f.step(1);assertEquals(7,f.slot);f.step(50_000_000);
  assertEquals(List.of("select2","pearl","select7","fireball"),f.log);assertEquals(List.of(1L,2L,4L,5L),f.ticks);
  assertFalse(f.engine.isActive());assertEquals(7,f.slot);f.step(999_000_000);assertEquals(4,f.log.size());assertEquals(1,f.cleanups);
 }
 @Test void cancelEveryStateLeavesNoDeferredUse(){for(int state=0;state<4;state++){Fake f=new Fake();f.engine.start(250);for(int i=0;i<state;i++)f.step(250_000_000);int count=f.log.size();f.engine.cancel();f.engine.cancel();f.step(999_000_000);assertEquals(count,f.log.size());assertFalse(f.engine.isActive());assertEquals(1,f.cleanups);}}
 @Test void invalidationAndExceptionsCleanUp(){Fake f=new Fake();f.engine.start(250);f.valid=false;f.engine.advance(0);assertFalse(f.engine.isActive());assertTrue(f.log.isEmpty());f=new Fake();f.engine.start(250);f.step(0);f.fail=true;Fake g=f;assertThrows(IllegalStateException.class,()->g.step(1));assertFalse(f.engine.isActive());}
 @Test void missingAndUnavailableNeverStart(){for(int i=0;i<3;i++){Fake f=new Fake();if(i==0)f.pearl=-1;if(i==1)f.fireball=-1;if(i==2)f.valid=false;f.engine.start(250);assertFalse(f.engine.isActive());assertEquals(4,f.slot);}}
 @Test void existingJitterAndDelayBounds(){for(int delay:new int[]{100,250,1000})for(double r:new double[]{0,.999999}){Fake f=new Fake();f.random=r;f.engine.start(delay);f.step(0);f.step(1);int ms=Math.max(100,Math.min(1000,delay+(r==0?-10:10)));f.step(ms*1_000_000L-1);assertEquals(2,f.log.size());f.step(1);assertEquals("select7",f.log.get(2));}}
 @Test void everyDistinctHotbarPair(){for(int p=0;p<9;p++)for(int b=0;b<9;b++){if(p==b)continue;Fake f=new Fake();f.pearl=p;f.fireball=b;f.engine.start(250);f.step(0);f.step(1);f.step(250_000_000);f.step(1);assertEquals(b,f.slot);assertFalse(f.engine.isActive());}}
}
