package dinner.dev.endless_gravity;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Configurable pressure curve. Each layer is an altitude -> pressure point;
 * the same curve drives the mod's gravity/muffle progress and the Sable
 * Overworld datapack pressure_function.
 */
public final class AtmosphereLayers {

    public static final int LAYER_COUNT = 8;

    public record Layer(double altitude, double pressure) {}

    private static final List<Layer> DEFAULTS = List.of(
            new Layer(-64.0, 1.25),
            new Layer(64.0, 1.0),
            new Layer(400.0, 0.5),
            new Layer(900.0, 0.2),
            new Layer(1200.0, 0.08),
            new Layer(1800.0, 0.01),
            new Layer(2500.0, 0.001),
            new Layer(3500.0, 0.0)
    );

    private static volatile List<Layer> cached = null;
    private static volatile List<? extends String> cachedRaw = null;

    private AtmosphereLayers() {}

    public static List<Layer> defaults() {
        return DEFAULTS;
    }

    /**
     * Parses the config list ("altitude:pressure" entries), sorted by altitude.
     * Falls back to defaults when the list is invalid, empty or too short.
     */
    public static List<Layer> parse(List<? extends String> raw) {
        if (raw == cachedRaw) return cached;
        cachedRaw = raw;
        cached = doParse(raw);
        return cached;
    }

    private static List<Layer> doParse(List<? extends String> raw) {
        List<Layer> layers = new ArrayList<>();
        if (raw != null) {
            for (String entry : raw) {
                String[] parts = entry.split(":");
                if (parts.length != 2) continue;
                try {
                    double altitude = Double.parseDouble(parts[0].trim());
                    double pressure = Double.parseDouble(parts[1].trim());
                    layers.add(new Layer(altitude, Mth.clamp(pressure, 0.0, 10.0)));
                } catch (NumberFormatException ignored) {}
            }
        }
        layers.sort(Comparator.comparingDouble(Layer::altitude));
        if (layers.size() < 2) return new ArrayList<>(DEFAULTS);
        return layers;
    }

    public static List<? extends String> serialize(List<Layer> layers) {
        List<String> out = new ArrayList<>();
        for (Layer layer : layers) {
            out.add(String.format(Locale.ROOT, "%.0f:%.4f", layer.altitude(), layer.pressure()));
        }
        return out;
    }

    /** Pressure at the given Y, piecewise linear between layers, clamped outside the curve. */
    public static double getPressure(double y) {
        List<Layer> layers = parse(Config.COMMON.atmosphereLayers.get());
        Layer first = layers.get(0);
        if (y <= first.altitude()) return first.pressure();
        for (int i = 0; i < layers.size() - 1; i++) {
            Layer a = layers.get(i);
            Layer b = layers.get(i + 1);
            if (y <= b.altitude()) {
                double fraction = (y - a.altitude()) / (b.altitude() - a.altitude());
                return a.pressure() + fraction * (b.pressure() - a.pressure());
            }
        }
        return layers.get(layers.size() - 1).pressure();
    }

    /** Atmosphere progress from 0.0 (full pressure) to 1.0 (no pressure). */
    public static double getProgress(double y) {
        return Mth.clamp(1.0 - getPressure(y), 0.0, 1.0);
    }

    /** Slope for the Sable bezier at the given layer index, via central difference. */
    public static double getSlope(List<Layer> layers, int index) {
        if (layers.size() < 2) return 0.0;
        Layer prev = layers.get(Math.max(0, index - 1));
        Layer next = layers.get(Math.min(layers.size() - 1, index + 1));
        double dAlt = next.altitude() - prev.altitude();
        if (dAlt == 0.0) return 0.0;
        return (next.pressure() - prev.pressure()) / dAlt;
    }
}
