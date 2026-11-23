package dinner.dev.endless_gravity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MuffleMathTest {

    @Test
    void perceptualCurvesRollOffSmoothlyAndReachEndpoints() {
        assertEquals(1.0, MuffleMath.perceptualGain(0.0, 0.01), 1e-6);
        assertEquals(0.01, MuffleMath.perceptualGain(1.0, 0.01), 1e-6);
        assertEquals(1.0, MuffleMath.perceptualGainHF(0.0, 0.005), 1e-6);
        assertEquals(0.005, MuffleMath.perceptualGainHF(1.0, 0.005), 1e-6);

        // At mid-altitude, high frequencies roll off noticeably earlier than overall loudness
        double midGain = MuffleMath.perceptualGain(0.5, 0.01);
        double midHF = MuffleMath.perceptualGainHF(0.5, 0.005);
        assertTrue(midHF < midGain, "HF should attenuate faster than overall gain in thin air");
        assertTrue(midHF < 0.1, "Logarithmic HF should be audibly muffled at 50% atmosphere");
    }

    @Test
    void selfSuitStaysAudibleInVacuumAndUnfilteredAtSurface() {
        assertEquals(1.0f, MuffleMath.selfSuitGain(1.0f), 1e-6);
        assertEquals(1.0f, MuffleMath.selfSuitGainHF(1.0f), 1e-6);

        float vacuumSelfGain = MuffleMath.selfSuitGain(0.01f);
        float vacuumSelfHF = MuffleMath.selfSuitGainHF(0.005f);
        assertTrue(vacuumSelfGain >= 0.35f, "Self suit sounds should stay audible in vacuum");
        assertTrue(vacuumSelfHF > 0.05f && vacuumSelfHF < 0.25f, "Self suit HF should be warm/muffled");
    }

    @Test
    void temporalSmoothingConvergesWithoutOvershoot() {
        float val = 1.0f;
        for (int i = 0; i < 40; i++) {
            val = MuffleMath.smooth(val, 0.05f);
            assertTrue(val >= 0.05f && val <= 1.0f);
        }
        assertEquals(0.05f, val, 1e-4f);
    }
}
