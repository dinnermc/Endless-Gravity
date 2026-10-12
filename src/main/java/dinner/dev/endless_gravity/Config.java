package dinner.dev.endless_gravity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JSON-backed config (Fabric has no ForgeConfigSpec). Mirrors the value-based
 * API of the NeoForge version so gameplay/screen code stays identical.
 */
public class Config {
    public static final Common COMMON = new Common();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("endless_gravity.json");

    private static boolean dirty;

    public static void load() {
        try {
            if (!Files.exists(PATH)) {
                save();
                return;
            }
            JsonObject obj = JsonParser.parseString(Files.readString(PATH)).getAsJsonObject();
            for (Value<?> value : COMMON.all) {
                value.read(obj);
            }
        } catch (Exception e) {
            // Malformed or unreadable file: keep defaults.
        }
    }

    public static void save() {
        try {
            JsonObject obj = new JsonObject();
            for (Value<?> value : COMMON.all) {
                value.write(obj);
            }
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(obj));
            dirty = false;
        } catch (Exception ignored) {
        }
    }

    /** Flushes pending changes (screen saves, synced values) once per tick. */
    public static void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public abstract static class Value<T> {
        protected final String key;
        protected final T defaultValue;
        protected T value;

        Value(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        public T get() {
            return value;
        }

        public void set(T newValue) {
            if (!newValue.equals(value)) {
                value = newValue;
                dirty = true;
            }
        }

        abstract void read(JsonObject obj);

        abstract void write(JsonObject obj);
    }

    public static class BoolValue extends Value<Boolean> {
        BoolValue(String key, boolean def) { super(key, def); }

        @Override
        void read(JsonObject obj) {
            JsonElement e = obj.get(key);
            if (e != null && e.isJsonPrimitive()) value = e.getAsBoolean();
        }

        @Override
        void write(JsonObject obj) {
            obj.addProperty(key, value);
        }
    }

    public static class DoubleValue extends Value<Double> {
        DoubleValue(String key, double def) { super(key, def); }

        @Override
        void read(JsonObject obj) {
            JsonElement e = obj.get(key);
            if (e != null && e.isJsonPrimitive()) value = e.getAsDouble();
        }

        @Override
        void write(JsonObject obj) {
            obj.addProperty(key, value);
        }
    }

    public static class IntValue extends Value<Integer> {
        IntValue(String key, int def) { super(key, def); }

        @Override
        void read(JsonObject obj) {
            JsonElement e = obj.get(key);
            if (e != null && e.isJsonPrimitive()) value = e.getAsInt();
        }

        @Override
        void write(JsonObject obj) {
            obj.addProperty(key, value);
        }
    }

    public static class StringListValue extends Value<List<String>> {
        StringListValue(String key, List<String> def) { super(key, def); }

        @Override
        void read(JsonObject obj) {
            JsonElement e = obj.get(key);
            if (e == null || !e.isJsonArray()) return;
            List<String> out = new ArrayList<>();
            e.getAsJsonArray().forEach(el -> {
                if (el.isJsonPrimitive()) out.add(el.getAsString());
            });
            if (!out.isEmpty()) value = out;
        }

        @Override
        void write(JsonObject obj) {
            JsonArray arr = new com.google.gson.JsonArray();
            value.forEach(arr::add);
            obj.add(key, arr);
        }
    }

    public static class Common {
        final List<Value<?>> all = new ArrayList<>();

        private <V extends Value<?>> V add(V value) {
            all.add(value);
            return value;
        }

        // Gravity
        public final BoolValue endEntityGravity = add(new BoolValue("endEntityGravity", true));
        public final BoolValue enablePlayerGravity = add(new BoolValue("enablePlayerGravity", true));
        public final DoubleValue playerGravityOffset = add(new DoubleValue("playerGravityOffset", 0.055));
        public final BoolValue enableItemGravity = add(new BoolValue("enableItemGravity", true));
        public final DoubleValue itemGravityOffset = add(new DoubleValue("itemGravityOffset", 0.025));
        public final BoolValue enableArrowGravity = add(new BoolValue("enableArrowGravity", true));
        public final DoubleValue arrowGravityOffset = add(new DoubleValue("arrowGravityOffset", 0.03));
        public final BoolValue enableThrownGravity = add(new BoolValue("enableThrownGravity", true));
        public final DoubleValue thrownGravityOffset = add(new DoubleValue("thrownGravityOffset", 0.018));

        // Effects
        public final BoolValue enableLowPassFilter = add(new BoolValue("enableLowPassFilter", true));
        public final DoubleValue lowPassGain = add(new DoubleValue("lowPassGain", 0.35));
        public final DoubleValue lowPassGainHF = add(new DoubleValue("lowPassGainHF", 0.25));

        // Particles
        // Upward acceleration per tick for particles in The End. 0.08 = neutral gravity (float), <0.08 = slow fall, >0.08 = slow rise
        public final DoubleValue particleGravityOffset = add(new DoubleValue("particleGravityOffset", 0.5));

        // Gameplay
        public final IntValue fallDamageMode = add(new IntValue("fallDamageMode", 1));
        public final BoolValue enableBlockGravity = add(new BoolValue("enableBlockGravity", true));
        public final DoubleValue blockGravityOffset = add(new DoubleValue("blockGravityOffset", 0.035));

// Atmosphere
        public final BoolValue enableAtmosphere = add(new BoolValue("enableAtmosphere", true));
        public final BoolValue overworldEntityGravity = add(new BoolValue("overworldEntityGravity", true));
        public final DoubleValue atmosphereGravityMax = add(new DoubleValue("atmosphereGravityMax", 0.075));
        public final DoubleValue noFallDamageAltitude = add(new DoubleValue("noFallDamageAltitude", 400.0));
        public final DoubleValue atmosphereMuffleGain = add(new DoubleValue("atmosphereMuffleGain", 0.01));
        public final DoubleValue atmosphereMuffleGainHF = add(new DoubleValue("atmosphereMuffleGainHF", 0.005));
        public final StringListValue atmosphereLayers = add(new StringListValue("atmosphereLayers",
                java.util.Arrays.asList("-64:1.25", "64:1.0", "400:0.5", "900:0.2", "1200:0.08", "1800:0.01", "2500:0.001", "3500:0.0")));

        // Mob gravity (new section)
        public final BoolValue enableMobGravity = add(new BoolValue("enableMobGravity", true));
        public final DoubleValue mobGravityOffset = add(new DoubleValue("mobGravityOffset", 0.025));

    }

    // Custom dimension configuration loaded from JSON file.
    // Allows modpack authors to define custom gravity/atmosphere per dimension.
    public static class CustomDimConfig {
        private final String dimensionId;
        private final boolean enableCustomGravity;
        private final double mobGravityOffset;
        private final boolean enableAtmosphere;
        private final double atmosphereGravityMax;
        private final double atmosphereBaseAltitude;
        private final double atmosphereMaxAltitude;
        private final double atmosphereMuffleGain;
        private final double atmosphereMuffleGainHF;
        private final boolean enableMobGravity;
        private final double mobGravityOffsetCustom;
        private final List<String> atmosphereLayers;

        public CustomDimConfig(
                String dimensionId,
                boolean enableCustomGravity,
                double mobGravityOffset,
                boolean enableAtmosphere,
                double atmosphereGravityMax,
                double atmosphereBaseAltitude,
                double atmosphereMaxAltitude,
                double atmosphereMuffleGain,
                double atmosphereMuffleGainHF,
                boolean enableMobGravity,
                double mobGravityOffsetCustom,
                List<String> atmosphereLayers) {
            this.dimensionId = dimensionId;
            this.enableCustomGravity = enableCustomGravity;
            this.mobGravityOffset = mobGravityOffset;
            this.enableAtmosphere = enableAtmosphere;
            this.atmosphereGravityMax = atmosphereGravityMax;
            this.atmosphereBaseAltitude = atmosphereBaseAltitude;
            this.atmosphereMaxAltitude = atmosphereMaxAltitude;
            this.atmosphereMuffleGain = atmosphereMuffleGain;
            this.atmosphereMuffleGainHF = atmosphereMuffleGainHF;
            this.enableMobGravity = enableMobGravity;
            this.mobGravityOffsetCustom = mobGravityOffsetCustom;
            this.atmosphereLayers = atmosphereLayers;
        }

        public String getDimensionId() { return dimensionId; }
        public boolean isEnableCustomGravity() { return enableCustomGravity; }
        public double getMobGravityOffset() { return mobGravityOffset; }
        public boolean isEnableAtmosphere() { return enableAtmosphere; }
        public double getAtmosphereGravityMax() { return atmosphereGravityMax; }
        public double getAtmosphereBaseAltitude() { return atmosphereBaseAltitude; }
        public double getAtmosphereMaxAltitude() { return atmosphereMaxAltitude; }
        public double getAtmosphereMuffleGain() { return atmosphereMuffleGain; }
        public double getAtmosphereMuffleGainHF() { return atmosphereMuffleGainHF; }
        public boolean isEnableMobGravity() { return enableMobGravity; }
        public double getMobGravityOffsetCustom() { return mobGravityOffsetCustom; }
        public List<String> getAtmosphereLayers() { return atmosphereLayers; }

        public static CustomDimConfig vanilla() {
            return new CustomDimConfig(
                    "minecraft:overworld",
                    false, 0.0, false, 0.075, 64.0, 3500.0, 0.01, 0.005,
                    false, 0.025, List.of()
            );
        }

        public static CustomDimConfig endDefault() {
            return new CustomDimConfig(
                    "minecraft:the_end",
                    true, 0.025, false, 0.0, 0.0, 0.0, 0.0, 0.0,
                    true, 0.025, List.of()
            );
        }
    }

    // Runtime cache for custom dimensions
    private static final Map<String, CustomDimConfig> CUSTOM_DIMENSIONS = new java.util.HashMap<>();
    private static boolean customDimensionsLoaded = false;

    public static void loadCustomDimensions() {
        if (customDimensionsLoaded) return;
        CUSTOM_DIMENSIONS.clear();

        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("endless_gravity").resolve("dimensions.json");
        if (!Files.exists(configPath)) {
            createDefaultDimensionsConfig(configPath);
        }

        try {
            String json = Files.readString(configPath);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray dimensions = root.getAsJsonArray("dimensions");
            if (dimensions != null) {
                Gson gson = new GsonBuilder().create();
                for (JsonElement element : dimensions) {
                    JsonObject obj = element.getAsJsonObject();
                    String dimId = obj.get("dimension").getAsString();

                    CustomDimConfig config = new CustomDimConfig(
                            dimId,
                            obj.get("enableCustomGravity").getAsBoolean(),
                            obj.get("mobGravityOffset").getAsDouble(),
                            obj.get("enableAtmosphere").getAsBoolean(),
                            obj.get("atmosphereGravityMax").getAsDouble(),
                            obj.get("atmosphereBaseAltitude").getAsDouble(),
                            obj.get("atmosphereMaxAltitude").getAsDouble(),
                            obj.get("atmosphereMuffleGain").getAsDouble(),
                            obj.get("atmosphereMuffleGainHF").getAsDouble(),
                            obj.get("enableMobGravity").getAsBoolean(),
                            obj.get("mobGravityOffsetCustom").getAsDouble(),
                            gson.fromJson(obj.get("atmosphereLayers"), new com.google.gson.reflect.TypeToken<List<String>>() {}.getType())
                    );
                    CUSTOM_DIMENSIONS.put(dimId, config);
                }
            }
            customDimensionsLoaded = true;
        } catch (IOException e) {
            // Log error but don't crash
            System.err.println("[Endless Gravity] Failed to load custom dimensions config: " + e.getMessage());
        }
    }

    private static void createDefaultDimensionsConfig(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            
            // Use Gson to properly serialize the JSON to avoid formatting issues
            JsonObject endDim = new JsonObject();
            endDim.addProperty("dimension", "minecraft:the_end");
            endDim.addProperty("enableCustomGravity", true);
            endDim.addProperty("mobGravityOffset", 0.025);
            endDim.addProperty("enableAtmosphere", false);
            endDim.addProperty("atmosphereGravityMax", 0.0);
            endDim.addProperty("atmosphereBaseAltitude", 0.0);
            endDim.addProperty("atmosphereMaxAltitude", 0.0);
            endDim.addProperty("atmosphereMuffleGain", 0.0);
            endDim.addProperty("atmosphereMuffleGainHF", 0.0);
            endDim.addProperty("enableMobGravity", true);
            endDim.addProperty("mobGravityOffsetCustom", 0.025);
            endDim.add("atmosphereLayers", new JsonArray());
            
            JsonObject overworldDim = new JsonObject();
            overworldDim.addProperty("dimension", "minecraft:overworld");
            overworldDim.addProperty("enableCustomGravity", false);
            overworldDim.addProperty("mobGravityOffset", 0.0);
            overworldDim.addProperty("enableAtmosphere", true);
            overworldDim.addProperty("atmosphereGravityMax", 0.075);
            overworldDim.addProperty("atmosphereBaseAltitude", 64.0);
            overworldDim.addProperty("atmosphereMaxAltitude", 3500.0);
            overworldDim.addProperty("atmosphereMuffleGain", 0.01);
            overworldDim.addProperty("atmosphereMuffleGainHF", 0.005);
            overworldDim.addProperty("enableMobGravity", false);
            overworldDim.addProperty("mobGravityOffsetCustom", 0.0);
            
            JsonArray layers = new JsonArray();
            layers.add("-64:1.25");
            layers.add("64:1.0");
            layers.add("400:0.5");
            layers.add("900:0.2");
            layers.add("1200:0.08");
            layers.add("1800:0.01");
            layers.add("2500:0.001");
            layers.add("3500:0.0");
            overworldDim.add("atmosphereLayers", layers);
            
            JsonArray dimensions = new JsonArray();
            dimensions.add(endDim);
            dimensions.add(overworldDim);
            
            JsonObject root = new JsonObject();
            root.add("dimensions", dimensions);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String defaultJson = gson.toJson(root);
            
            // Write to temp file first, then move to avoid corruption
            Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");
            Files.writeString(tempPath, defaultJson);
            Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("[Endless Gravity] Failed to create default dimensions config: " + e.getMessage());
        }
    }

    public static void reloadCustomDimensions() {
        customDimensionsLoaded = false;
        loadCustomDimensions();
    }

    public static Optional<CustomDimConfig> getCustomDimConfig(String dimensionId) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        return Optional.ofNullable(CUSTOM_DIMENSIONS.get(dimensionId));
    }

    public static boolean hasCustomGravity(String dimensionId) {
        return getCustomDimConfig(dimensionId).map(CustomDimConfig::isEnableCustomGravity).orElse(false);
    }

    public static boolean hasCustomAtmosphere(String dimensionId) {
        return getCustomDimConfig(dimensionId).map(CustomDimConfig::isEnableAtmosphere).orElse(false);
    }

    public static double getMobGravityOffsetForDimension(String dimensionId) {
        return getCustomDimConfig(dimensionId)
                .map(c -> c.isEnableCustomGravity() ? c.getMobGravityOffset() : 0.0)
                .orElse(0.0);
    }

    public static double getMobGravityOffsetCustom(String dimensionId) {
        return getCustomDimConfig(dimensionId)
                .map(c -> c.isEnableMobGravity() ? c.getMobGravityOffsetCustom() : 0.0)
                .orElse(0.0);
    }

    public static boolean hasMobGravity(String dimensionId) {
        return getCustomDimConfig(dimensionId).map(CustomDimConfig::isEnableMobGravity).orElse(false);
    }

    public static Optional<CustomDimConfig> getCustomDimConfigByKey(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
        return getCustomDimConfig(dimension.location().toString());
    }
}