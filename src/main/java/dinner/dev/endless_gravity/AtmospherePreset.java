package dinner.dev.endless_gravity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public record AtmospherePreset(
        String name,
        double gravityMax,
        double startAltitude,
        Double noFallDamageAbove,
        double muffleGain,
        double muffleGainHF,
        List<String> layers
) {
    public static final int MAX_PRESETS = 64;

    public AtmospherePreset {
        if (name == null || name.isBlank() || name.length() > 48) {
            throw new IllegalArgumentException("Preset name must contain 1-48 characters");
        }
        requireRange(gravityMax, 0.0, 0.5, "gravityMax");
        requireRange(startAltitude, -2048.0, 30000.0, "startAltitude");
        if (noFallDamageAbove != null) requireRange(noFallDamageAbove, 0.0, 30000.0, "noFallDamageAbove");
        requireRange(muffleGain, 0.0, 1.0, "muffleGain");
        requireRange(muffleGainHF, 0.0, 1.0, "muffleGainHF");
        if (layers == null || layers.size() < 2 || layers.size() > CustomDimConfig.MAX_ATMOSPHERE_LAYERS) {
            throw new IllegalArgumentException("Preset requires 2-"
                    + CustomDimConfig.MAX_ATMOSPHERE_LAYERS + " valid atmosphere layers");
        }
        List<AtmosphereLayers.Layer> parsed = new ArrayList<>();
        double previousAltitude = Double.NEGATIVE_INFINITY;
        for (String layer : layers) {
            String[] parts = layer.split(":", -1);
            if (parts.length != 2) throw new IllegalArgumentException("Invalid atmosphere layer");
            try {
                double altitude = Double.parseDouble(parts[0]);
                double pressure = Double.parseDouble(parts[1]);
                requireRange(altitude, -2048, 30000, "layer altitude");
                requireRange(pressure, 0, 10, "layer pressure");
                if (altitude <= previousAltitude) {
                    throw new IllegalArgumentException("Layer altitudes must be strictly increasing");
                }
                previousAltitude = altitude;
                parsed.add(new AtmosphereLayers.Layer(altitude, pressure));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid atmosphere layer", e);
            }
        }
        layers = List.copyOf(AtmosphereLayers.serialize(parsed));
    }

    private static void requireRange(double value, double min, double max, String field) {
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException("Invalid " + field + " value");
        }
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("gravity_max", gravityMax);
        obj.addProperty("start_altitude", startAltitude);
        if (noFallDamageAbove == null) obj.add("no_fall_damage_above", null);
        else obj.addProperty("no_fall_damage_above", noFallDamageAbove);
        obj.addProperty("muffle_gain", muffleGain);
        obj.addProperty("muffle_gain_hf", muffleGainHF);
        JsonArray layerArray = new JsonArray();
        layers.forEach(layerArray::add);
        obj.add("layers", layerArray);
        return obj;
    }

    public static AtmospherePreset fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) throw new IllegalArgumentException("Preset must be an object");
        JsonObject obj = element.getAsJsonObject();
        List<String> layerList = new ArrayList<>();
        JsonElement layersElement = obj.get("layers");
        if (layersElement == null || !layersElement.isJsonArray()) {
            throw new IllegalArgumentException("Preset is missing its layers array");
        }
        for (JsonElement layer : layersElement.getAsJsonArray()) {
            if (!layer.isJsonPrimitive() || !layer.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Layer values must use altitude:pressure strings");
            }
            layerList.add(layer.getAsString());
        }
        JsonElement noFall = obj.get("no_fall_damage_above");
        return new AtmospherePreset(
                stringValue(obj, "name"),
                numberValue(obj, "gravity_max"),
                numberValue(obj, "start_altitude"),
                noFall == null || noFall.isJsonNull() ? null : finiteNumber(noFall),
                numberValue(obj, "muffle_gain"),
                numberValue(obj, "muffle_gain_hf"),
                layerList
        );
    }

    public static List<AtmospherePreset> parseImport(String json) {
        JsonElement root = JsonParser.parseString(json);
        JsonArray presets;
        if (root.isJsonArray()) {
            presets = root.getAsJsonArray();
        } else if (root.isJsonObject() && root.getAsJsonObject().has("presets")) {
            presets = root.getAsJsonObject().getAsJsonArray("presets");
        } else {
            throw new IllegalArgumentException("JSON must contain a 'presets' array");
        }
        if (presets.size() > MAX_PRESETS) {
            throw new IllegalArgumentException("Import must contain at most " + MAX_PRESETS + " presets");
        }
        List<AtmospherePreset> result = new ArrayList<>();
        for (JsonElement element : presets) result.add(fromJson(element));
        return List.copyOf(result);
    }

    public static String exportJson(List<AtmospherePreset> presets) {
        JsonObject root = new JsonObject();
        root.addProperty("format", "endless_gravity_atmosphere_presets");
        root.addProperty("version", 1);
        JsonArray array = new JsonArray();
        presets.forEach(preset -> array.add(preset.toJson()));
        root.add("presets", array);
        return root.toString();
    }

    private static String stringValue(JsonObject obj, String key) {
        JsonElement value = obj.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Missing preset field: " + key);
        }
        return value.getAsString();
    }

    private static double numberValue(JsonObject obj, String key) {
        JsonElement value = obj.get(key);
        if (value == null) throw new IllegalArgumentException("Missing preset field: " + key);
        return finiteNumber(value);
    }

    private static double finiteNumber(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Expected a numeric preset value");
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number)) throw new IllegalArgumentException("Preset values must be finite");
        return number;
    }
}
