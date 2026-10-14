package dinner.dev.endless_gravity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure tests for the pressure curve in {@link AtmosphereLayers}. These deliberately avoid
 * anything Minecraft or config related: they exercise {@link AtmosphereLayers#parse},
 * {@link AtmosphereLayers#getPressure(List, double)}, {@link AtmosphereLayers#getProgress(List, double)},
 * {@link AtmosphereLayers#clamp(double, double, double)} and {@link AtmosphereLayers#getSlope}.
 */
class AtmosphereLayersTest {

    private static final double EPS = 1.0e-9;

    private static List<AtmosphereLayers.Layer> defaults() {
        return AtmosphereLayers.defaults();
    }

    @Test
    void clampKeepsValueInsideRange() {
        assertEquals(5.0, AtmosphereLayers.clamp(5.0, 0.0, 10.0), EPS);
        assertEquals(0.0, AtmosphereLayers.clamp(-3.0, 0.0, 10.0), EPS);
        assertEquals(10.0, AtmosphereLayers.clamp(42.0, 0.0, 10.0), EPS);
    }

    @Test
    void pressureAtExactDefaultAltitudes() {
        List<AtmosphereLayers.Layer> layers = defaults();
        assertEquals(1.25, AtmosphereLayers.getPressure(layers, -64.0), EPS);
        assertEquals(1.0, AtmosphereLayers.getPressure(layers, 64.0), EPS);
        assertEquals(0.5, AtmosphereLayers.getPressure(layers, 400.0), EPS);
        assertEquals(0.08, AtmosphereLayers.getPressure(layers, 1200.0), EPS);
        assertEquals(0.0, AtmosphereLayers.getPressure(layers, 3500.0), EPS);
    }

    @Test
    void pressureIsClampedOutsideTheCurve() {
        List<AtmosphereLayers.Layer> layers = defaults();
        assertEquals(1.25, AtmosphereLayers.getPressure(layers, -5000.0), EPS);
        assertEquals(0.0, AtmosphereLayers.getPressure(layers, 100000.0), EPS);
    }

    @Test
    void pressureInterpolatesLinearlyBetweenLayers() {
        List<AtmosphereLayers.Layer> layers = defaults();
        // Halfway between -64 (1.25) and 64 (1.0).
        assertEquals(1.125, AtmosphereLayers.getPressure(layers, 0.0), EPS);
        // A quarter of the way from 64 (1.0) to 400 (0.5).
        assertEquals(1.0 - 0.25 * 0.5, AtmosphereLayers.getPressure(layers, 148.0), EPS);
    }

    @Test
    void progressIsClampedToZeroOne() {
        List<AtmosphereLayers.Layer> layers = defaults();
        assertEquals(0.0, AtmosphereLayers.getProgress(layers, -64.0), EPS);   // pressure 1.25 -> clamped
        assertEquals(0.5, AtmosphereLayers.getProgress(layers, 400.0), EPS);   // pressure 0.5
        assertEquals(1.0, AtmosphereLayers.getProgress(layers, 3500.0), EPS);  // pressure 0.0
    }

    @Test
    void parseSortsByAltitude() {
        List<AtmosphereLayers.Layer> layers =
                AtmosphereLayers.parse(List.of("400:0.5", "-64:1.25", "64:1.0"));
        assertEquals(3, layers.size());
        assertEquals(-64.0, layers.get(0).altitude(), EPS);
        assertEquals(64.0, layers.get(1).altitude(), EPS);
        assertEquals(400.0, layers.get(2).altitude(), EPS);
    }

    @Test
    void parseClampsPressureToZeroTen() {
        List<AtmosphereLayers.Layer> layers =
                AtmosphereLayers.parse(List.of("0:50", "100:-5", "200:1"));
        assertEquals(10.0, layers.get(0).pressure(), EPS);
        assertEquals(0.0, layers.get(1).pressure(), EPS);
        assertEquals(1.0, layers.get(2).pressure(), EPS);
    }

    @Test
    void parseSkipsMalformedEntries() {
        List<AtmosphereLayers.Layer> layers =
                AtmosphereLayers.parse(List.of("notanumber", "10:2:3", "1:x", "50:0.75", "0:1"));
        assertEquals(2, layers.size());
        assertEquals(0.0, layers.get(0).altitude(), EPS);
        assertEquals(50.0, layers.get(1).altitude(), EPS);
    }

    @Test
    void parseFallsBackToDefaultsWhenFewerThanTwoLayers() {
        assertEquals(defaults(), AtmosphereLayers.parse(List.of("100:0.5")));
        assertEquals(defaults(), AtmosphereLayers.parse(List.of()));
        assertEquals(defaults(), AtmosphereLayers.parse(null));
    }

    @Test
    void parsedLayersDriveTheSameCurve() {
        List<AtmosphereLayers.Layer> layers = AtmosphereLayers.parse(List.of("0:1.0", "1000:0.0"));
        assertEquals(1.0, AtmosphereLayers.getPressure(layers, 0.0), EPS);
        assertEquals(0.5, AtmosphereLayers.getPressure(layers, 500.0), EPS);
        assertEquals(0.0, AtmosphereLayers.getPressure(layers, 1000.0), EPS);
        assertTrue(AtmosphereLayers.getProgress(layers, 750.0) > 0.7);
    }

    @Test
    void slopeUsesNeighbours() {
        List<AtmosphereLayers.Layer> layers = List.of(
                new AtmosphereLayers.Layer(0.0, 1.0),
                new AtmosphereLayers.Layer(100.0, 0.5),
                new AtmosphereLayers.Layer(200.0, 0.0));
        assertEquals(-0.005, AtmosphereLayers.getSlope(layers, 0), EPS);
        assertEquals(-0.005, AtmosphereLayers.getSlope(layers, 1), EPS);
        assertEquals(-0.005, AtmosphereLayers.getSlope(layers, 2), EPS);
    }

    @Test
    void slopeIsZeroForSingleLayer() {
        assertEquals(0.0,
                AtmosphereLayers.getSlope(List.of(new AtmosphereLayers.Layer(0.0, 1.0)), 0), EPS);
    }
}
