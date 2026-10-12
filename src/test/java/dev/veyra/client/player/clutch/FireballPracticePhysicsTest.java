package dev.veyra.client.player.clutch;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FireballPracticePhysicsTest {
    @Test void onlyExplosionImpulseIsAmplifiedAndExistingSpeedIsPreserved() {
        assertEquals(.875,FireballPracticePhysics.amplifyImpulse(.2,.7,1.35),1e-12);
        assertEquals(-.4,FireballPracticePhysics.amplifyImpulse(-.4,-.4,1.35),1e-12);
        assertEquals(.7,FireballPracticePhysics.amplifyImpulse(.2,.7,1),1e-12);
        assertEquals(-1.35,FireballPracticePhysics.amplifyImpulse(0,-1,1.35),1e-12);
    }
    @Test void nativeDragDoesNotChangeConfiguredAirSpeed() {
        for(double speed:new double[]{.25,1,1.5,3}) {
            double motion=speed, position=0;
            double acceleration=FireballPracticePhysics.accelerationForSpeed(speed);
            for(int tick=0;tick<100;tick++) {
                position+=motion;
                motion=(motion+acceleration)*.95F;
                assertEquals(speed,motion,1e-12);
            }
            assertEquals(speed*100,position,1e-10);
        }
    }
    @Test void everyDirectionRemainsCollinearWithoutRandomSpread() {
        double[] direction={.3,-.4,Math.sqrt(.75)};
        double speed=1.5,acceleration=FireballPracticePhysics.accelerationForSpeed(speed);
        for(double component:direction) {
            double motion=component*speed;
            for(int tick=0;tick<20;tick++)motion=(motion+component*acceleration)*.95F;
            assertEquals(component*speed,motion,1e-12);
        }
    }
}
