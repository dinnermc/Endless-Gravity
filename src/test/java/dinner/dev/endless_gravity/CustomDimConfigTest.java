package dinner.dev.endless_gravity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomDimConfigTest {
    private static final double EPS = 1.0e-9;

    @Test
    void atmosphereProfileUsesItsOwnGravityAndPressureCurve() {
        CustomDimConfig profile = new CustomDimConfig(
                "example:moon", CustomDimConfig.Mode.ATMOSPHERE,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.04, 100.0, null, 0.2, 0.1,
                List.of("0:1.0", "1000:0.0"));

        assertEquals(0.0, profile.getAtmosphereGravityOffset(100.0), EPS);
        assertEquals(0.02, profile.getAtmosphereGravityOffset(550.0), EPS);
        assertEquals(0.04, profile.getAtmosphereGravityOffset(1000.0), EPS);
        assertEquals(0.6, profile.getAtmosphereMuffleGain(500.0), EPS);
        assertEquals(0.55, profile.getAtmosphereMuffleGainHF(500.0), EPS);
    }

    @Test
    void fixedGravityProfileKeepsIndependentEntityOffsets() {
        CustomDimConfig profile = new CustomDimConfig(
                "example:moon", CustomDimConfig.Mode.FIXED_GRAVITY,
                0.04, -0.02, 0.01, 0.02, 0.015, 0.03,
                0.075, 319.0, null, 0.01, 0.005,
                AtmosphereLayers.serialize(AtmosphereLayers.defaults()));

        assertEquals(0.04, profile.getPlayerGravityOffset(), EPS);
        assertEquals(-0.02, profile.getMobGravityOffset(), EPS);
        assertEquals(0.01, profile.getItemGravityOffset(), EPS);
        assertEquals(0.02, profile.getArrowGravityOffset(), EPS);
        assertEquals(0.015, profile.getThrownGravityOffset(), EPS);
        assertEquals(0.03, profile.getBlockGravityOffset(), EPS);
        assertEquals(CustomDimConfig.Mode.FIXED_GRAVITY, profile.getMode());
    }
}
