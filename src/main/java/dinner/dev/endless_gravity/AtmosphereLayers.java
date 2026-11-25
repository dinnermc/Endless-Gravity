package dinner.dev.endless_gravity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Configured pressure curve plus the shared smooth gravity ramp used by entities
 * and Sable sub-levels.
 */
public final class AtmosphereLayers {

    public static final int LAYER_COUNT = 8;
    public static final double GRAVITY_START_ALTITUDE = 319.0;
    public static final double AUDIO_MUFFLE_START_ALTITUDE = 319.0;
    private static final double VANILLA_GRAVITY = 0.08;
    private static final double MAX_GRAVITY_COMPENSATION = 0.99;
    private static final double MAX_SABLE_GRAVITY_COMPENSATION = 0.067;

    public record Layer(double altitude, double pressure) {}

    private static final List<Layer> DEFAULTS = List.of(
            new Layer(-64.0, 1.25),
            new Layer(64.0, 1.0),
            new Layer(400.0, 0.5),
            new Layer(1200.0, 0.4),
            new Layer(2000.0, 0.3),
            new Layer(3000.0, 0.2),
            new Layer(4500.0, 0.1),
            new Layer(6000.0, 0.0)
    );

    private record ParseCache(List<String> rawSnapshot, List<Layer> parsed) {}

    private static volatile ParseCache cache = null;

    private AtmosphereLayers() {}

    public static List<Layer> defaults() {
        return DEFAULTS;
    }

    /** "altitude:pressure" entries, sorted by altitude; defaults if invalid or too short. */
    public static List<Layer> parse(List<? extends String> raw) {
        if (raw == null) return DEFAULTS;
        ParseCache current = cache;
        if (current != null && raw.equals(current.rawSnapshot())) {
            return current.parsed();
        }
        List<String> snapshot = List.copyOf(raw);
        List<Layer> parsed = List.copyOf(parseUncached(snapshot));
        cache = new ParseCache(snapshot, parsed);
        return parsed;
    }

    public static List<Layer> parseUncached(List<? extends String> raw) {
        List<Layer> layers = new ArrayList<>();
        if (raw != null) {
            for (String entry : raw) {
                String[] parts = entry.split(":");
                if (parts.length != 2) continue;
                try {
                    double altitude = Double.parseDouble(parts[0].trim());
                    double pressure = Double.parseDouble(parts[1].trim());
                    if (!Double.isFinite(altitude) || !Double.isFinite(pressure)) continue;
                    layers.add(new Layer(altitude, clamp(pressure, 0.0, 10.0)));
                } catch (NumberFormatException ignored) {}
            }
        }
        layers.sort(Comparator.comparingDouble(Layer::altitude));
        if (layers.size() < 2) return new ArrayList<>(DEFAULTS);
        return layers;
    }

    public static List<String> serialize(List<Layer> layers) {
        List<String> out = new ArrayList<>();
        for (Layer layer : layers) {
            out.add(String.format(Locale.ROOT, "%.0f:%.4f", layer.altitude(), layer.pressure()));
        }
        return out;
    }

    /** Pressure at the given Y, piecewise linear between layers, clamped outside the curve. */
    public static double getPressure(double y) {
        return getPressure(parse(Config.COMMON.atmosphereLayers.get()), y);
    }

    /** Pure variant of {@link #getPressure(double)}: piecewise linear between the given layers. */
    public static double getPressure(List<Layer> layers, double y) {
        if (layers == null || layers.isEmpty()) return 1.0;
        Layer first = layers.get(0);
        if (y <= first.altitude()) return first.pressure();
        for (int i = 0; i < layers.size() - 1; i++) {
            Layer a = layers.get(i);
            Layer b = layers.get(i + 1);
            if (y <= b.altitude()) {
                double dAlt = b.altitude() - a.altitude();
                if (dAlt <= 0.0) return b.pressure();
                double fraction = (y - a.altitude()) / dAlt;
                return a.pressure() + fraction * (b.pressure() - a.pressure());
            }
        }
        return layers.get(layers.size() - 1).pressure();
    }

    /** Atmosphere progress from 0.0 (full pressure) to 1.0 (no pressure). */
    public static double getProgress(double y) {
        return getProgress(parse(Config.COMMON.atmosphereLayers.get()), y);
    }

    /** Pure variant of {@link #getProgress(double)}: progress from the given layers, clamped to [0, 1]. */
    public static double getProgress(List<Layer> layers, double y) {
        return clamp(1.0 - getPressure(layers, y), 0.0, 1.0);
    }

    /**
     * Audio-muffle progress in {@code [0, 1]} using the global curve and audio start altitude.
     */
    public static double getAudioMuffleProgress(double y) {
        return getAudioMuffleProgress(
                parse(Config.COMMON.atmosphereLayers.get()),
                Config.COMMON.atmosphereMuffleStartAltitude.get(),
                y);
    }

    /**
     * Audio-muffle progress in {@code [0, 1]} starting at {@code muffleStartAltitude}.
     * At or below {@code muffleStartAltitude} this returns {@code 0.0} so lower altitudes
     * (such as Overworld mountains below Y=319) keep vanilla audio, then ramps smoothly
     * from {@code 0.0} to {@code 1.0} as pressure drops to vacuum.
     */
    public static double getAudioMuffleProgress(List<Layer> layers, double muffleStartAltitude, double y) {
        if (y <= muffleStartAltitude) return 0.0;
        double pressure = getPressure(layers, y);
        double startPressure = Math.min(1.0, getPressure(layers, muffleStartAltitude));
        if (startPressure <= 0.0) {
            return pressure <= 0.0 ? 1.0 : 0.0;
        }
        return clamp((startPressure - pressure) / startPressure, 0.0, 1.0);
    }

    /**
     * Smooth gravity ramp from Y=319 to the final atmosphere layer. Pressure remains
     * independent so changing the gravity ramp does not alter audio or Sable drag.
     */
    public static double getGravityProgress(double y) {
        return getGravityProgress(parse(Config.COMMON.atmosphereLayers.get()), y);
    }

    /** Pure variant of {@link #getGravityProgress(double)} for the given atmosphere curve. */
    public static double getGravityProgress(List<Layer> layers, double y) {
        return getGravityProgress(layers, GRAVITY_START_ALTITUDE, y);
    }

    /** Pure variant with a dimension-specific gravity-ramp start altitude. */
    public static double getGravityProgress(List<Layer> layers, double startAltitude, double y) {
        if (layers == null || layers.isEmpty()) return 0.0;
        double endAltitude = layers.get(layers.size() - 1).altitude();
        if (endAltitude <= startAltitude) return 0.0;
        double t = clamp((y - startAltitude) / (endAltitude - startAltitude), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** Gravity compensation as a fraction of vanilla gravity, capped to leave a downward pull. */
    public static double getGravityCompensationFraction(double maxOffset) {
        return clamp(maxOffset / VANILLA_GRAVITY, 0.0, MAX_GRAVITY_COMPENSATION);
    }

    /** Sable uses a different physics scale, so its lift stays at the previously tuned safe limit. */
    public static double getSableGravityCompensationFraction(double maxOffset) {
        return Math.min(getGravityCompensationFraction(maxOffset), MAX_SABLE_GRAVITY_COMPENSATION);
    }

    /** Estimate the starting world altitude from a landing altitude and fall distance. */
    public static double getFallStartAltitude(double landingY, double fallDistance) {
        return landingY + Math.max(0.0, fallDistance);
    }

    /** Clamp helper matching {@code net.minecraft.util.Mth.clamp}. */
    public static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /** Slope for the Sable bezier at a layer, from the two neighbors. */
    public static double getSlope(List<Layer> layers, int index) {
        if (layers.size() < 2) return 0.0;
        Layer prev = layers.get(Math.max(0, index - 1));
        Layer next = layers.get(Math.min(layers.size() - 1, index + 1));
        double dAlt = next.altitude() - prev.altitude();
        if (dAlt == 0.0) return 0.0;
        return (next.pressure() - prev.pressure()) / dAlt;
    }
}
