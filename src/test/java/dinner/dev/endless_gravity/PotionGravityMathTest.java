package dinner.dev.endless_gravity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PotionGravityMathTest {

    private static final double BASE = 0.08;

    @Test
    void weightlessnessScalesWithAmbientGravityAndNeverGoesNegative() {
        // 1.0g => exact configured value
        assertEquals(0.05, PotionGravityMath.weightlessnessOffset(0.05, 0, BASE, BASE), 1e-9);
        assertEquals(0.08, PotionGravityMath.weightlessnessOffset(0.05, 1, BASE, BASE), 1e-9);

        // Low gravity (End: 0.025) => works less, never exceeds remaining gravity
        double endBonusI = PotionGravityMath.weightlessnessOffset(0.05, 0, BASE, 0.025);
        assertTrue(endBonusI > 0.0 && endBonusI < 0.025);
        assertEquals(0.025 * (0.05 / BASE), endBonusI, 1e-9);
        assertEquals(0.025, PotionGravityMath.weightlessnessOffset(0.05, 1, BASE, 0.025), 1e-9);

        // Vacuum (0g) => no effect
        assertEquals(0.0, PotionGravityMath.weightlessnessOffset(0.05, 0, BASE, 0.0), 1e-9);

        // High gravity (2.0g: 0.16) => stronger relief
        double highBonusI = PotionGravityMath.weightlessnessOffset(0.05, 0, BASE, 0.16);
        assertEquals(0.10, highBonusI, 1e-9);
    }

    @Test
    void heavinessWorksStrongerInLowGravityAndGentlerInHighGravity() {
        // 1.0g => exact configured value
        assertEquals(0.02, PotionGravityMath.heavinessPull(0.02, 0, BASE, BASE), 1e-9);
        assertEquals(0.04, PotionGravityMath.heavinessPull(0.02, 1, BASE, BASE), 1e-9);

        // Vacuum (0g) => 2x pull (Level II restores full 1.0g)
        assertEquals(0.04, PotionGravityMath.heavinessPull(0.02, 0, BASE, 0.0), 1e-9);
        assertEquals(0.08, PotionGravityMath.heavinessPull(0.02, 1, BASE, 0.0), 1e-9);

        // Low gravity > 1g pull > high gravity pull
        double lowPull = PotionGravityMath.heavinessPull(0.02, 0, BASE, 0.025);
        double normalPull = PotionGravityMath.heavinessPull(0.02, 0, BASE, BASE);
        double highPull = PotionGravityMath.heavinessPull(0.02, 0, BASE, 0.16);
        assertTrue(lowPull > normalPull && normalPull > highPull);
    }
}
