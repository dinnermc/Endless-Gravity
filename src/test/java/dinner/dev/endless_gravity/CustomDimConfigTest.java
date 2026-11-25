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
                List.of("0:1.0", "1000:0.0"),
                0.0, false, CustomDimConfig.FallDamageMode.INHERIT,
                1.0, 1.0, 0.0);

        assertEquals(0.0, profile.getAtmosphereGravityOffset(100.0), EPS);
        assertEquals(0.02, profile.getAtmosphereGravityOffset(550.0), EPS);
        assertEquals(0.04, profile.getAtmosphereGravityOffset(1000.0), EPS);
        assertEquals(0.5, profile.getAtmospherePressure(500.0), EPS);
        assertEquals(1.0, profile.getAtmosphereMuffleGain(0.0), EPS);
        assertEquals(0.2, profile.getAtmosphereMuffleGain(1000.0), EPS);
        assertEquals(MuffleMath.perceptualGain(0.5, 0.2), profile.getAtmosphereMuffleGain(500.0), EPS);
        assertEquals(MuffleMath.perceptualGainHF(0.5, 0.1), profile.getAtmosphereMuffleGainHF(500.0), EPS);
    }

    @Test
    void atmosphereProfileRespectsDefault319AudioMuffleStartAltitude() {
        CustomDimConfig overworld = new CustomDimConfig(
                "minecraft:overworld", CustomDimConfig.Mode.ATMOSPHERE,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.075, 319.0, 4500.0, 0.01, 0.005,
                AtmosphereLayers.serialize(AtmosphereLayers.defaults()));

        assertEquals(319.0, overworld.getAtmosphereMuffleStartAltitude(), EPS);
        assertEquals(1.0, overworld.getAtmosphereMuffleGain(220.0), EPS);
        assertEquals(1.0, overworld.getAtmosphereMuffleGainHF(220.0), EPS);
        assertEquals(1.0, overworld.getAtmosphereMuffleGain(319.0), EPS);
        assertEquals(1.0, overworld.getAtmosphereMuffleGainHF(319.0), EPS);
        assertEquals(0.01, overworld.getAtmosphereMuffleGain(6000.0), EPS);
        assertEquals(0.005, overworld.getAtmosphereMuffleGainHF(6000.0), EPS);
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

    @Test
    void atmosphereProfileBlendsSurfaceGravityIntoSpaceReduction() {
        CustomDimConfig mars = new CustomDimConfig(
                "example:mars", CustomDimConfig.Mode.ATMOSPHERE,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.08, 200.0, 1000.0, 0.1, 0.05,
                List.of("0:1.0", "1000:0.0"),
                0.04, true, CustomDimConfig.FallDamageMode.PROPORTIONAL);

        assertEquals(0.04, mars.getAtmosphereGravityOffset(100.0), EPS);
        assertEquals(0.06, mars.getAtmosphereGravityOffset(600.0), EPS);
        assertEquals(0.08, mars.getAtmosphereGravityOffset(1000.0), EPS);
        assertEquals(true, mars.isAtmosphereAffectNonLiving());
        assertEquals(CustomDimConfig.FallDamageMode.PROPORTIONAL, mars.getFallDamageMode());
    }
}
