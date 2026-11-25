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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;

/**
 * JSON-backed config (Fabric has no ForgeConfigSpec). Mirrors the value-based
 * API of the NeoForge version so gameplay/screen code stays identical.
 */
public class Config {
    public static final Common COMMON = new Common();

    public static final int MAX_CUSTOM_DIMENSIONS = 512;
    private static final Gson GSON_COMPACT = new Gson();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("endless_gravity.json");

    private static volatile boolean dirty;
    private static volatile boolean serverSyncActive;

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        String json;
        try {
            json = Files.readString(PATH);
        } catch (IOException e) {
            System.err.println("[Endless Gravity] Could not read config (" + e.getMessage() + "); using defaults.");
            return;
        }
        JsonObject obj;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                throw new IllegalStateException("root is not a JSON object");
            }
            obj = root.getAsJsonObject();
        } catch (RuntimeException e) {
            // Only when the whole file is unreadable: back it up and regenerate defaults.
            // A single bad field is handled per-field in read(), not here.
            System.err.println("[Endless Gravity] Config is corrupt (" + e.getMessage() + "); backing up to .bak and regenerating defaults.");
            try {
                Files.move(PATH, PATH.resolveSibling(PATH.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
            for (Value<?> value : COMMON.all) {
                value.reset();
            }
            save();
            return;
        }
        for (Value<?> value : COMMON.all) {
            value.read(obj);
        }
    }

    public static void save() {
        // Never persist a remote server's synced values over the player's own config.
        if (serverSyncActive) {
            dirty = false;
            return;
        }
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

    /**
     * Marks whether a remote server's config is currently applied over the local one.
     * While active, {@link #save()} refuses to write so synced values are never persisted;
     * the player's own values are restored (and this cleared) on disconnect.
     */
    public static void setServerSyncActive(boolean active) {
        serverSyncActive = active;
    }

    public static boolean isServerSyncActive() {
        return serverSyncActive;
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
            T fixed = sanitize(newValue);
            if (!(fixed == null ? value == null : fixed.equals(value))) {
                value = fixed;
                dirty = true;
            }
        }

        /** Sets the value without touching the dirty flag (used for server-synced values). */
        public void setSynced(T newValue) {
            this.value = sanitize(newValue);
        }

        /** Restores the field default (used when the whole config file is corrupt). */
        public void reset() {
            value = defaultValue;
        }

        /** Clamps/validates a value before it is stored (set/setSynced/read). Default is identity. */
        protected T sanitize(T newValue) {
            return newValue;
        }

        abstract void read(JsonObject obj);

        abstract void write(JsonObject obj);
    }

    public static class BoolValue extends Value<Boolean> {
        BoolValue(String key, boolean def) { super(key, def); }

        @Override
        void read(JsonObject obj) {
            JsonElement e = obj.get(key);
            if (e == null) return;
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) {
                value = e.getAsBoolean();
            } else {
                System.err.println("[Endless Gravity] config '" + key + "' is not a boolean; using default " + defaultValue);
                value = defaultValue;
            }
        }

        @Override
        void write(JsonObject obj) {
            obj.addProperty(key, value);
        }
    }

    public static class DoubleValue extends Value<Double> {
        private final double min;
        private final double max;

        DoubleValue(String key, double def) {
            this(key, def, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        DoubleValue(String key, double def, double min, double max) {
            super(key, def);
            this.min = min;
            this.max = max;
        }

        @Override
        protected Double sanitize(Double newValue) {
            if (newValue == null || newValue.isNaN() || newValue.isInfinite()) return defaultValue;
            return Math.max(min, Math.min(max, newValue));
        }

        @Override
        void read(JsonObject obj) {
            JsonElement e = obj.get(key);
            if (e == null) return;
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
                System.err.println("[Endless Gravity] config '" + key + "' is not a number; using default " + defaultValue);
                value = defaultValue;
                return;
            }
            double raw = e.getAsDouble();
            Double fixed = sanitize(raw);
            if (fixed != raw) {
                System.err.println("[Endless Gravity] config '" + key + "'=" + raw + " is not finite or outside [" + min + ", " + max + "]; using " + fixed);
            }
            value = fixed;
        }

        @Override
        void write(JsonObject obj) {
            obj.addProperty(key, value);
        }
    }

    public static class IntValue extends Value<Integer> {
        private final int min;
        private final int max;

        IntValue(String key, int def) {
            this(key, def, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }

        IntValue(String key, int def, int min, int max) {
            super(key, def);
            this.min = min;
            this.max = max;
        }

        @Override
        protected Integer sanitize(Integer newValue) {
            if (newValue == null) return defaultValue;
            return Math.max(min, Math.min(max, newValue));
        }

        @Override
        void read(JsonObject obj) {
            JsonElement e = obj.get(key);
            if (e == null) return;
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
                System.err.println("[Endless Gravity] config '" + key + "' is not a number; using default " + defaultValue);
                value = defaultValue;
                return;
            }
            int raw = e.getAsInt();
            int fixed = sanitize(raw);
            if (fixed != raw) {
                System.err.println("[Endless Gravity] config '" + key + "' value " + raw + " is out of range [" + min + ", " + max + "]; using " + fixed);
            }
            value = fixed;
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
        public final DoubleValue playerGravityOffset = add(new DoubleValue("playerGravityOffset", 0.055, 0.0, 0.07));
        public final BoolValue enableItemGravity = add(new BoolValue("enableItemGravity", true));
        public final DoubleValue itemGravityOffset = add(new DoubleValue("itemGravityOffset", 0.025, 0.0, 0.035));
        public final BoolValue enableArrowGravity = add(new BoolValue("enableArrowGravity", true));
        public final DoubleValue arrowGravityOffset = add(new DoubleValue("arrowGravityOffset", 0.03, 0.0, 0.04));
        public final BoolValue enableThrownGravity = add(new BoolValue("enableThrownGravity", true));
        public final DoubleValue thrownGravityOffset = add(new DoubleValue("thrownGravityOffset", 0.018, 0.0, 0.025));

        // Effects
        public final BoolValue enableLowPassFilter = add(new BoolValue("enableLowPassFilter", true));
        public final DoubleValue lowPassGain = add(new DoubleValue("lowPassGain", 0.35, 0.0, 1.0));
        public final DoubleValue lowPassGainHF = add(new DoubleValue("lowPassGainHF", 0.25, 0.0, 1.0));

        // Particles
        // Upward acceleration per tick for particles in The End. 0.08 = neutral gravity (float), <0.08 = slow fall, >0.08 = slow rise
        public final DoubleValue particleGravityOffset = add(new DoubleValue("particleGravityOffset", 0.1, 0.0, 0.5));

        // Gameplay
        public final IntValue fallDamageMode = add(new IntValue("fallDamageMode", 1, 0, 2));
        public final IntValue mobFallDamageMode = add(new IntValue("mobFallDamageMode", 1, 0, 2));
        public final BoolValue enableBlockGravity = add(new BoolValue("enableBlockGravity", true));
        public final DoubleValue blockGravityOffset = add(new DoubleValue("blockGravityOffset", 0.035, 0.0, 0.1));

// Atmosphere
        public final BoolValue enableAtmosphere = add(new BoolValue("enableAtmosphere", true));
        public final BoolValue enableAtmosphericDrag = add(new BoolValue("enableAtmosphericDrag", false));
        public final BoolValue overworldEntityGravity = add(new BoolValue("overworldEntityGravity", true));
        public final DoubleValue atmosphereGravityMax = add(new DoubleValue("atmosphereGravityMax", 0.075, 0.0, 0.1));
        public final DoubleValue noFallDamageAltitude = add(new DoubleValue("noFallDamageAltitude", 4500.0, 64.0, 6000.0));
        public final DoubleValue atmosphereMuffleGain = add(new DoubleValue("atmosphereMuffleGain", 0.01, 0.0, 1.0));
        public final DoubleValue atmosphereMuffleGainHF = add(new DoubleValue("atmosphereMuffleGainHF", 0.005, 0.0, 1.0));
        public final DoubleValue atmosphereMuffleStartAltitude = add(new DoubleValue(
                "atmosphereMuffleStartAltitude", AtmosphereLayers.AUDIO_MUFFLE_START_ALTITUDE, -2048.0, 30000.0));
        public final StringListValue atmosphereLayers = add(new StringListValue("atmosphereLayers",
                java.util.Arrays.asList("-64:1.25", "64:1.0", "400:0.5", "1200:0.4", "2000:0.3", "3000:0.2", "4500:0.1", "6000:0.0")));

        // Mob gravity (new section)
        public final BoolValue enableMobGravity = add(new BoolValue("enableMobGravity", true));
        public final DoubleValue mobGravityOffset = add(new DoubleValue("mobGravityOffset", 0.025, 0.0, 0.035));

        // Potions
        public final BoolValue enablePotionGravity = add(new BoolValue("enablePotionGravity", true));
        public final DoubleValue weightlessnessGravityOffset = add(new DoubleValue("weightlessnessGravityOffset", 0.05, 0.0, 0.07));
        public final DoubleValue heavinessGravityOffset = add(new DoubleValue("heavinessGravityOffset", 0.02, 0.0, 0.07));

        // HUD (client-side only)
        public final BoolValue enableGravityHud = add(new BoolValue("enableGravityHud", false));
        public final IntValue hudPosition = add(new IntValue("hudPosition", 0, 0, 3));
        public final BoolValue hudShowPressure = add(new BoolValue("hudShowPressure", true));
        public final BoolValue hudCompact = add(new BoolValue("hudCompact", false));

        // Extra entity IDs excluded from reduced gravity, in addition to the gravity_immune tag.
        // The tag needs a datapack; this is the convenient per-player alternative.
        public final StringListValue gravityImmuneEntities = add(new StringListValue("gravityImmuneEntities", java.util.List.of()));

    }

    // Custom dimension configuration loaded from JSON file.
    // Allows modpack authors to define custom gravity/atmosphere per dimension.
    public static class CustomDimConfig {
        public static final int MAX_ATMOSPHERE_LAYERS = 64;

        public enum Mode { ATMOSPHERE, FIXED_GRAVITY }
        public enum FallDamageMode { INHERIT, NORMAL, PROPORTIONAL, DISABLED }

        private final String dimensionId;
        private final Mode mode;
        private final double playerGravityOffset;
        private final double mobGravityOffset;
        private final double itemGravityOffset;
        private final double arrowGravityOffset;
        private final double thrownGravityOffset;
        private final double blockGravityOffset;
        private final double atmosphereGravityMax;
        private final double atmosphereBaseAltitude;
        private final Double noFallDamageAltitude;
        private final double atmosphereMuffleGain;
        private final double atmosphereMuffleGainHF;
        private final List<String> atmosphereLayers;
        private final List<AtmosphereLayers.Layer> parsedAtmosphereLayers;
        private final double surfaceGravityOffset;
        private final boolean atmosphereAffectNonLiving;
        private final FallDamageMode fallDamageMode;
        private final double fixedMuffleGain;
        private final double fixedMuffleGainHF;
        private final double atmosphereMuffleStartAltitude;

        public CustomDimConfig(
                String dimensionId,
                Mode mode,
                double playerGravityOffset,
                double mobGravityOffset,
                double itemGravityOffset,
                double arrowGravityOffset,
                double thrownGravityOffset,
                double blockGravityOffset,
                double atmosphereGravityMax,
                double atmosphereBaseAltitude,
                Double noFallDamageAltitude,
                double atmosphereMuffleGain,
                double atmosphereMuffleGainHF,
                List<String> atmosphereLayers) {
            this(dimensionId, mode,
                    playerGravityOffset, mobGravityOffset, itemGravityOffset,
                    arrowGravityOffset, thrownGravityOffset, blockGravityOffset,
                    atmosphereGravityMax, atmosphereBaseAltitude, noFallDamageAltitude,
                    atmosphereMuffleGain, atmosphereMuffleGainHF, atmosphereLayers,
                    0.0, false, FallDamageMode.INHERIT);
        }

        public CustomDimConfig(
                String dimensionId,
                Mode mode,
                double playerGravityOffset,
                double mobGravityOffset,
                double itemGravityOffset,
                double arrowGravityOffset,
                double thrownGravityOffset,
                double blockGravityOffset,
                double atmosphereGravityMax,
                double atmosphereBaseAltitude,
                Double noFallDamageAltitude,
                double atmosphereMuffleGain,
                double atmosphereMuffleGainHF,
                List<String> atmosphereLayers,
                double surfaceGravityOffset,
                boolean atmosphereAffectNonLiving,
                FallDamageMode fallDamageMode) {
            this(dimensionId, mode,
                    playerGravityOffset, mobGravityOffset, itemGravityOffset,
                    arrowGravityOffset, thrownGravityOffset, blockGravityOffset,
                    atmosphereGravityMax, atmosphereBaseAltitude, noFallDamageAltitude,
                    atmosphereMuffleGain, atmosphereMuffleGainHF, atmosphereLayers,
                    surfaceGravityOffset, atmosphereAffectNonLiving, fallDamageMode,
                    1.0, 1.0);
        }

        public CustomDimConfig(
                String dimensionId,
                Mode mode,
                double playerGravityOffset,
                double mobGravityOffset,
                double itemGravityOffset,
                double arrowGravityOffset,
                double thrownGravityOffset,
                double blockGravityOffset,
                double atmosphereGravityMax,
                double atmosphereBaseAltitude,
                Double noFallDamageAltitude,
                double atmosphereMuffleGain,
                double atmosphereMuffleGainHF,
                List<String> atmosphereLayers,
                double surfaceGravityOffset,
                boolean atmosphereAffectNonLiving,
                FallDamageMode fallDamageMode,
                double fixedMuffleGain,
                double fixedMuffleGainHF) {
            this(dimensionId, mode,
                    playerGravityOffset, mobGravityOffset, itemGravityOffset,
                    arrowGravityOffset, thrownGravityOffset, blockGravityOffset,
                    atmosphereGravityMax, atmosphereBaseAltitude, noFallDamageAltitude,
                    atmosphereMuffleGain, atmosphereMuffleGainHF, atmosphereLayers,
                    surfaceGravityOffset, atmosphereAffectNonLiving, fallDamageMode,
                    fixedMuffleGain, fixedMuffleGainHF, AtmosphereLayers.AUDIO_MUFFLE_START_ALTITUDE);
        }

        public CustomDimConfig(
                String dimensionId,
                Mode mode,
                double playerGravityOffset,
                double mobGravityOffset,
                double itemGravityOffset,
                double arrowGravityOffset,
                double thrownGravityOffset,
                double blockGravityOffset,
                double atmosphereGravityMax,
                double atmosphereBaseAltitude,
                Double noFallDamageAltitude,
                double atmosphereMuffleGain,
                double atmosphereMuffleGainHF,
                List<String> atmosphereLayers,
                double surfaceGravityOffset,
                boolean atmosphereAffectNonLiving,
                FallDamageMode fallDamageMode,
                double fixedMuffleGain,
                double fixedMuffleGainHF,
                double atmosphereMuffleStartAltitude) {
            this.dimensionId = dimensionId;
            this.mode = mode;
            this.playerGravityOffset = playerGravityOffset;
            this.mobGravityOffset = mobGravityOffset;
            this.itemGravityOffset = itemGravityOffset;
            this.arrowGravityOffset = arrowGravityOffset;
            this.thrownGravityOffset = thrownGravityOffset;
            this.blockGravityOffset = blockGravityOffset;
            this.atmosphereGravityMax = atmosphereGravityMax;
            this.atmosphereBaseAltitude = atmosphereBaseAltitude;
            this.noFallDamageAltitude = noFallDamageAltitude;
            this.atmosphereMuffleGain = atmosphereMuffleGain;
            this.atmosphereMuffleGainHF = atmosphereMuffleGainHF;
            this.atmosphereLayers = List.copyOf(atmosphereLayers);
            this.parsedAtmosphereLayers = List.copyOf(AtmosphereLayers.parseUncached(this.atmosphereLayers));
            this.surfaceGravityOffset = surfaceGravityOffset;
            this.atmosphereAffectNonLiving = atmosphereAffectNonLiving;
            this.fallDamageMode = fallDamageMode == null ? FallDamageMode.INHERIT : fallDamageMode;
            this.fixedMuffleGain = MuffleMath.clamp01(fixedMuffleGain);
            this.fixedMuffleGainHF = MuffleMath.clamp01(fixedMuffleGainHF);
            this.atmosphereMuffleStartAltitude = Double.isFinite(atmosphereMuffleStartAltitude)
                    ? AtmosphereLayers.clamp(atmosphereMuffleStartAltitude, -2048.0, 30000.0)
                    : AtmosphereLayers.AUDIO_MUFFLE_START_ALTITUDE;
        }

        /** Backwards-compatible constructor for the legacy dimensions.json schema. */
        public CustomDimConfig(String dimensionId, boolean enableCustomGravity, double mobGravityOffset,
                               Double playerGravityOffset, boolean enableAtmosphere, double atmosphereGravityMax,
                               double atmosphereBaseAltitude, double atmosphereMaxAltitude,
                               double atmosphereMuffleGain, double atmosphereMuffleGainHF,
                               boolean enableMobGravity, double mobGravityOffsetCustom,
                               List<String> atmosphereLayers) {
            this(dimensionId,
                    enableAtmosphere && !enableCustomGravity ? Mode.ATMOSPHERE : Mode.FIXED_GRAVITY,
                    playerGravityOffset == null ? 0.0 : playerGravityOffset,
                    enableCustomGravity ? (enableMobGravity ? mobGravityOffsetCustom : mobGravityOffset) : 0.0,
                    0.0, 0.0, 0.0, 0.0,
                    atmosphereGravityMax, atmosphereBaseAltitude,
                    enableAtmosphere && atmosphereMaxAltitude >= 0 ? atmosphereMaxAltitude : null,
                    atmosphereMuffleGain, atmosphereMuffleGainHF, atmosphereLayers == null ? List.of() : atmosphereLayers);
        }

        public String getDimensionId() { return dimensionId; }
        public Mode getMode() { return mode; }
        public boolean isEnableCustomGravity() { return mode == Mode.FIXED_GRAVITY; }
        public double getMobGravityOffset() { return mobGravityOffset; }
        public double getPlayerGravityOffset() { return playerGravityOffset; }
        public double getItemGravityOffset() { return itemGravityOffset; }
        public double getArrowGravityOffset() { return arrowGravityOffset; }
        public double getThrownGravityOffset() { return thrownGravityOffset; }
        public double getBlockGravityOffset() { return blockGravityOffset; }
        public boolean isEnableAtmosphere() { return mode == Mode.ATMOSPHERE; }
        public double getAtmosphereGravityMax() { return atmosphereGravityMax; }
        public double getAtmosphereBaseAltitude() { return atmosphereBaseAltitude; }
        public double getAtmosphereMaxAltitude() {
            return parsedAtmosphereLayers.get(parsedAtmosphereLayers.size() - 1).altitude();
        }
        public Double getNoFallDamageAltitude() { return noFallDamageAltitude; }
        public double getAtmosphereMuffleGain() { return atmosphereMuffleGain; }
        public double getAtmosphereMuffleGainHF() { return atmosphereMuffleGainHF; }
        public boolean isEnableMobGravity() { return mode == Mode.FIXED_GRAVITY; }
        public double getMobGravityOffsetCustom() { return mobGravityOffset; }
        public List<String> getAtmosphereLayers() { return atmosphereLayers; }
        public List<AtmosphereLayers.Layer> getParsedAtmosphereLayers() { return parsedAtmosphereLayers; }
        public double getSurfaceGravityOffset() { return surfaceGravityOffset; }
        public boolean isAtmosphereAffectNonLiving() { return atmosphereAffectNonLiving; }
        public FallDamageMode getFallDamageMode() { return fallDamageMode; }
        public double getFixedMuffleGain() { return fixedMuffleGain; }
        public double getFixedMuffleGainHF() { return fixedMuffleGainHF; }
        public boolean hasFixedAudioMuffle() { return fixedMuffleGain < 0.999 || fixedMuffleGainHF < 0.999; }
        public double getAtmosphereMuffleStartAltitude() { return atmosphereMuffleStartAltitude; }

        public double getAtmospherePressure(double y) {
            return AtmosphereLayers.getPressure(parsedAtmosphereLayers, y);
        }

        public double getAtmosphereProgress(double y) {
            return AtmosphereLayers.getProgress(parsedAtmosphereLayers, y);
        }

        public double getAtmosphereGravityProgress(double y) {
            return AtmosphereLayers.getGravityProgress(parsedAtmosphereLayers, atmosphereBaseAltitude, y);
        }

        public double getAtmosphereGravityOffset(double y) {
            double progress = getAtmosphereGravityProgress(y);
            if (surfaceGravityOffset == 0.0) {
                return progress * atmosphereGravityMax;
            }
            double target = Math.max(surfaceGravityOffset, atmosphereGravityMax);
            return surfaceGravityOffset + progress * (target - surfaceGravityOffset);
        }

        public double getAtmosphereMuffleProgress(double y) {
            return AtmosphereLayers.getAudioMuffleProgress(
                    parsedAtmosphereLayers, atmosphereMuffleStartAltitude, y);
        }

        public double getAtmosphereMuffleGain(double y) {
            return MuffleMath.perceptualGain(getAtmosphereMuffleProgress(y), atmosphereMuffleGain);
        }

        public double getAtmosphereMuffleGainHF(double y) {
            return MuffleMath.perceptualGainHF(getAtmosphereMuffleProgress(y), atmosphereMuffleGainHF);
        }

        public static CustomDimConfig vanilla() {
            return new CustomDimConfig(
                    "minecraft:overworld", Mode.ATMOSPHERE,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    COMMON.atmosphereGravityMax.get(), AtmosphereLayers.GRAVITY_START_ALTITUDE,
                    COMMON.noFallDamageAltitude.get(), COMMON.atmosphereMuffleGain.get(),
                    COMMON.atmosphereMuffleGainHF.get(), List.copyOf(COMMON.atmosphereLayers.get()),
                    0.0, false, FallDamageMode.INHERIT,
                    1.0, 1.0, COMMON.atmosphereMuffleStartAltitude.get()
            );
        }

        public static CustomDimConfig endDefault() {
            return new CustomDimConfig(
                    "minecraft:the_end", Mode.FIXED_GRAVITY,
                    COMMON.enablePlayerGravity.get() ? COMMON.playerGravityOffset.get() : 0.0,
                    COMMON.enableMobGravity.get() ? COMMON.mobGravityOffset.get() : 0.0,
                    COMMON.enableItemGravity.get() ? COMMON.itemGravityOffset.get() : 0.0,
                    COMMON.enableArrowGravity.get() ? COMMON.arrowGravityOffset.get() : 0.0,
                    COMMON.enableThrownGravity.get() ? COMMON.thrownGravityOffset.get() : 0.0,
                    COMMON.enableBlockGravity.get() ? COMMON.blockGravityOffset.get() : 0.0,
                    0.0, 319.0, null, 0.0, 0.0,
                    AtmosphereLayers.serialize(AtmosphereLayers.defaults())
            );
        }
    }

    // Thread-safe immutable snapshots for custom dimensions (lock-free reads on entity ticks).
    private static Map<String, CustomDimConfig> userCustomDimensions = Map.of();
    private static Map<String, CustomDimConfig> datapackDimensions = Map.of();
    private static volatile Map<String, CustomDimConfig> customDimensions = Map.of();
    private static volatile Map<net.minecraft.resources.ResourceLocation, CustomDimConfig> customDimensionsByLocation = Map.of();
    private static volatile boolean customDimensionsLoaded = false;

    private static void publishEffectiveDimensions(Map<String, CustomDimConfig> map) {
        Map<String, CustomDimConfig> byId = new java.util.LinkedHashMap<>(map);
        Map<net.minecraft.resources.ResourceLocation, CustomDimConfig> byLoc = new java.util.LinkedHashMap<>(map.size());
        for (Map.Entry<String, CustomDimConfig> entry : byId.entrySet()) {
            net.minecraft.resources.ResourceLocation loc = net.minecraft.resources.ResourceLocation.tryParse(entry.getKey());
            if (loc != null) {
                byLoc.put(loc, entry.getValue());
            }
        }
        customDimensions = Map.copyOf(byId);
        customDimensionsByLocation = Map.copyOf(byLoc);
    }

    private static void rebuildMergedDimensions() {
        Map<String, CustomDimConfig> merged = new java.util.LinkedHashMap<>(datapackDimensions);
        merged.putAll(userCustomDimensions);
        publishEffectiveDimensions(merged);
    }

    public static synchronized void setDatapackDimensions(Map<String, CustomDimConfig> profiles) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        datapackDimensions = profiles == null ? Map.of() : Map.copyOf(profiles);
        rebuildMergedDimensions();
    }

    public static CustomDimConfig parseDimensionJson(JsonObject obj) {
        return parseDimension(obj);
    }

    public static synchronized void loadCustomDimensions() {
        if (customDimensionsLoaded) return;
        Map<String, CustomDimConfig> loaded = new java.util.LinkedHashMap<>();

        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("endless_gravity").resolve("dimensions.json");
        if (!Files.exists(configPath)) {
            createDefaultDimensionsConfig(configPath);
        }

        try {
            parseDimensionsFile(configPath, loaded);
        } catch (Exception e) {
            // Corrupt or truncated file: back it up, regenerate the default, retry once.
            System.err.println("[Endless Gravity] Failed to load custom dimensions config (" + e.getMessage() + "); regenerating default.");
            try {
                Files.move(configPath, configPath.resolveSibling(configPath.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
            createDefaultDimensionsConfig(configPath);
            loaded.clear();
            try {
                parseDimensionsFile(configPath, loaded);
            } catch (Exception e2) {
                System.err.println("[Endless Gravity] Failed to load regenerated custom dimensions config: " + e2.getMessage());
                loaded.put("minecraft:overworld", CustomDimConfig.vanilla());
                loaded.put("minecraft:the_end", CustomDimConfig.endDefault());
            }
        }
        userCustomDimensions = Map.copyOf(loaded);
        rebuildMergedDimensions();
        customDimensionsLoaded = true;
    }

    private static void parseDimensionsFile(Path configPath, Map<String, CustomDimConfig> target) throws IOException {
        String json = Files.readString(configPath);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray dimensions = root.getAsJsonArray("dimensions");
        if (dimensions == null) throw new IllegalArgumentException("missing 'dimensions' array");
        for (JsonElement element : dimensions) {
            if (target.size() >= MAX_CUSTOM_DIMENSIONS) break;
            try {
                CustomDimConfig config = parseDimension(element.getAsJsonObject());
                target.putIfAbsent(config.getDimensionId(), config);
            } catch (RuntimeException e) {
                System.err.println("[Endless Gravity] Ignoring invalid dimension profile: " + e.getMessage());
            }
        }
    }

    private static CustomDimConfig parseDimension(JsonObject obj) {
        String id = stringValue(obj, "id", stringValue(obj, "dimension", ""));
        if (net.minecraft.resources.ResourceLocation.tryParse(id) == null) {
            throw new IllegalArgumentException("invalid dimension ID: " + id);
        }
        JsonObject gravity = obj.has("gravity") && obj.get("gravity").isJsonObject()
                ? obj.getAsJsonObject("gravity") : obj;
        JsonObject atmosphere = obj.has("atmosphere") && obj.get("atmosphere").isJsonObject()
                ? obj.getAsJsonObject("atmosphere") : obj;

        JsonElement modeValue = obj.get("mode");
        boolean legacySchema = modeValue == null;
        boolean legacyGravityEnabled = boolValue(obj, "enableCustomGravity", id.equals("minecraft:the_end"));
        CustomDimConfig.Mode mode;
        if (modeValue != null && modeValue.isJsonPrimitive()) {
            mode = switch (modeValue.getAsString().toLowerCase(java.util.Locale.ROOT)) {
                case "atmosphere" -> CustomDimConfig.Mode.ATMOSPHERE;
                case "fixed_gravity", "fixed-gravity", "gravity" -> CustomDimConfig.Mode.FIXED_GRAVITY;
                default -> throw new IllegalArgumentException("invalid mode for " + id);
            };
        } else {
            boolean legacyAtmosphere = boolValue(obj, "enableAtmosphere", id.equals("minecraft:overworld"));
            mode = legacyAtmosphere && !legacyGravityEnabled
                    ? CustomDimConfig.Mode.ATMOSPHERE : CustomDimConfig.Mode.FIXED_GRAVITY;
        }

        JsonElement noFallElement = obj.get("no_fall_damage_above");
        if (noFallElement == null) noFallElement = obj.get("atmosphereMaxAltitude");
        Double noFallAltitude = noFallElement == null || noFallElement.isJsonNull()
                ? null : numberValue(noFallElement, "no fall damage altitude");
        if (legacySchema && id.equals("minecraft:overworld")) {
            noFallAltitude = COMMON.noFallDamageAltitude.get();
        }
        List<String> layers = stringList(atmosphere, "layers", "atmosphereLayers");
        if (layers.size() < 2) layers = AtmosphereLayers.serialize(AtmosphereLayers.defaults());
        double playerOffset = numberValue(gravity, "player", numberValue(obj, "playerGravityOffset",
                legacySchema && COMMON.enablePlayerGravity.get() ? COMMON.playerGravityOffset.get() : 0.0));
        double mobOffset = numberValue(gravity, "mob", numberValue(obj, "mobGravityOffset",
                numberValue(obj, "mobGravityOffsetCustom", 0.025)));
        double itemOffset = numberValue(gravity, "item", legacySchema && COMMON.enableItemGravity.get()
                ? COMMON.itemGravityOffset.get() : 0.0);
        double arrowOffset = numberValue(gravity, "arrow", legacySchema && COMMON.enableArrowGravity.get()
                ? COMMON.arrowGravityOffset.get() : 0.0);
        double thrownOffset = numberValue(gravity, "thrown", legacySchema && COMMON.enableThrownGravity.get()
                ? COMMON.thrownGravityOffset.get() : 0.0);
        double blockOffset = numberValue(gravity, "block", legacySchema && COMMON.enableBlockGravity.get()
                ? COMMON.blockGravityOffset.get() : 0.0);
        if (legacySchema && !legacyGravityEnabled) {
            playerOffset = mobOffset = itemOffset = arrowOffset = thrownOffset = blockOffset = 0.0;
        }
        double baseAltitude = numberValue(atmosphere, "start_altitude",
                numberValue(obj, "atmosphereBaseAltitude", AtmosphereLayers.GRAVITY_START_ALTITUDE));
        if (legacySchema && id.equals("minecraft:overworld")) {
            baseAltitude = AtmosphereLayers.GRAVITY_START_ALTITUDE;
        }
        if (noFallAltitude != null && (!Double.isFinite(noFallAltitude) || noFallAltitude < 0)) {
            noFallAltitude = null;
        } else if (noFallAltitude != null) {
            noFallAltitude = clamp(noFallAltitude, 0, 30000);
        }

        double surfaceGravity = numberValue(atmosphere, "surface_gravity_offset", 0.0);
        boolean affectNonLiving = boolValue(atmosphere, "affect_non_living", false);
        CustomDimConfig.FallDamageMode dimFallDamage = parseFallDamageMode(obj.get("fall_damage_mode"));
        double fixedMuffleGain = gravity != obj && gravity.has("muffle_gain")
                ? numberValue(gravity, "muffle_gain", 1.0)
                : numberValue(obj, "fixed_muffle_gain", 1.0);
        double fixedMuffleGainHF = gravity != obj && gravity.has("muffle_gain_hf")
                ? numberValue(gravity, "muffle_gain_hf", 1.0)
                : numberValue(obj, "fixed_muffle_gain_hf", 1.0);
        double muffleStartAltitude = numberValue(atmosphere, "muffle_start_altitude",
                COMMON.atmosphereMuffleStartAltitude.get());

        return new CustomDimConfig(
                id, mode,
                clamp(playerOffset, -0.5, 0.5), clamp(mobOffset, -0.5, 0.5),
                clamp(itemOffset, -0.5, 0.5), clamp(arrowOffset, -0.5, 0.5),
                clamp(thrownOffset, -0.5, 0.5), clamp(blockOffset, -0.5, 0.5),
                clamp(numberValue(atmosphere, "gravity_offset", numberValue(obj, "atmosphereGravityMax", 0.075)),
                        0.0, 0.5),
                clamp(baseAltitude, -2048, 30000),
                noFallAltitude,
                clamp(numberValue(atmosphere, "muffle_gain", numberValue(obj, "atmosphereMuffleGain", 0.01)), 0, 1),
                clamp(numberValue(atmosphere, "muffle_gain_hf",
                        numberValue(obj, "atmosphereMuffleGainHF", 0.005)), 0, 1),
                layers,
                clamp(surfaceGravity, -0.5, 0.5), affectNonLiving, dimFallDamage,
                clamp(fixedMuffleGain, 0, 1), clamp(fixedMuffleGainHF, 0, 1),
                clamp(muffleStartAltitude, -2048, 30000)
        );
    }

    private static CustomDimConfig.FallDamageMode parseFallDamageMode(JsonElement element) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return CustomDimConfig.FallDamageMode.INHERIT;
        }
        return switch (element.getAsString().toLowerCase(Locale.ROOT)) {
            case "normal", "vanilla" -> CustomDimConfig.FallDamageMode.NORMAL;
            case "proportional", "scaled" -> CustomDimConfig.FallDamageMode.PROPORTIONAL;
            case "disabled", "none", "off" -> CustomDimConfig.FallDamageMode.DISABLED;
            default -> CustomDimConfig.FallDamageMode.INHERIT;
        };
    }

    private static String stringValue(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }

    private static boolean boolValue(JsonObject object, String key, boolean fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsBoolean();
    }

    private static double numberValue(JsonObject object, String key, double fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : numberValue(value, key);
    }

    private static double numberValue(JsonElement value, String name) {
        double result = value.getAsDouble();
        if (!Double.isFinite(result)) throw new IllegalArgumentException("invalid " + name);
        return result;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static List<String> stringList(JsonObject object, String preferred, String legacy) {
        JsonElement value = object.has(preferred) ? object.get(preferred) : object.get(legacy);
        if (value == null || !value.isJsonArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonElement entry : value.getAsJsonArray()) result.add(entry.getAsString());
        return result;
    }

    private static void createDefaultDimensionsConfig(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());
            Map<String, CustomDimConfig> defaults = new java.util.LinkedHashMap<>();
            defaults.put("minecraft:overworld", CustomDimConfig.vanilla());
            defaults.put("minecraft:the_end", CustomDimConfig.endDefault());
            writeCustomDimensions(configPath, defaults.values());
        } catch (IOException e) {
            System.err.println("[Endless Gravity] Failed to create default dimensions config: " + e.getMessage());
        }
    }

    private static JsonObject toDimensionJson(CustomDimConfig config) {
        JsonObject dimension = new JsonObject();
        dimension.addProperty("id", config.getDimensionId());
        dimension.addProperty("mode", config.getMode() == CustomDimConfig.Mode.ATMOSPHERE
                ? "atmosphere" : "fixed_gravity");
        JsonObject gravity = new JsonObject();
        gravity.addProperty("player", config.getPlayerGravityOffset());
        gravity.addProperty("mob", config.getMobGravityOffset());
        gravity.addProperty("item", config.getItemGravityOffset());
        gravity.addProperty("arrow", config.getArrowGravityOffset());
        gravity.addProperty("thrown", config.getThrownGravityOffset());
        gravity.addProperty("block", config.getBlockGravityOffset());
        gravity.addProperty("muffle_gain", config.getFixedMuffleGain());
        gravity.addProperty("muffle_gain_hf", config.getFixedMuffleGainHF());
        dimension.add("gravity", gravity);
        JsonObject atmosphere = new JsonObject();
        atmosphere.addProperty("gravity_offset", config.getAtmosphereGravityMax());
        atmosphere.addProperty("start_altitude", config.getAtmosphereBaseAltitude());
        atmosphere.addProperty("surface_gravity_offset", config.getSurfaceGravityOffset());
        atmosphere.addProperty("affect_non_living", config.isAtmosphereAffectNonLiving());
        atmosphere.addProperty("muffle_gain", config.getAtmosphereMuffleGain());
        atmosphere.addProperty("muffle_gain_hf", config.getAtmosphereMuffleGainHF());
        atmosphere.addProperty("muffle_start_altitude", config.getAtmosphereMuffleStartAltitude());
        atmosphere.add("layers", GSON.toJsonTree(config.getAtmosphereLayers()));
        dimension.add("atmosphere", atmosphere);
        if (config.getNoFallDamageAltitude() == null) dimension.add("no_fall_damage_above", null);
        else dimension.addProperty("no_fall_damage_above", config.getNoFallDamageAltitude());
        dimension.addProperty("fall_damage_mode", config.getFallDamageMode().name().toLowerCase(Locale.ROOT));
        return dimension;
    }

    private static void writeCustomDimensions(Path configPath, java.util.Collection<CustomDimConfig> profiles) throws IOException {
        JsonArray dimensions = new JsonArray();
        profiles.stream()
                .sorted(java.util.Comparator.comparing(CustomDimConfig::getDimensionId))
                .forEach(config -> dimensions.add(toDimensionJson(config)));
        JsonObject root = new JsonObject();
        root.add("dimensions", dimensions);
        Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");
        Files.writeString(tempPath, GSON.toJson(root));
        try {
            Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static synchronized void reloadCustomDimensions() {
        customDimensionsLoaded = false;
        loadCustomDimensions();
    }

    /** Persists runtime changes made via the config screen or commands to disk. */
    public static void persist() {
        save();
    }

    public static Optional<CustomDimConfig> getCustomDimConfig(String dimensionId) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        return Optional.ofNullable(customDimensions.get(dimensionId));
    }

    /** Zero-allocation lookup for hot entity tick paths. */
    public static Optional<CustomDimConfig> getCustomDimConfig(net.minecraft.resources.ResourceLocation dimensionId) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        return Optional.ofNullable(customDimensionsByLocation.get(dimensionId));
    }

    public static synchronized List<CustomDimConfig> getCustomDimensions() {
        if (!customDimensionsLoaded) loadCustomDimensions();
        return customDimensions.values().stream()
                .sorted(java.util.Comparator.comparing(CustomDimConfig::getDimensionId))
                .toList();
    }

    public static synchronized List<String> getCustomDimensionsJson() {
        return getCustomDimensions().stream().map(config -> GSON_COMPACT.toJson(toDimensionJson(config))).toList();
    }

    /** Pretty-printed JSON representation suitable for a datapack file under data/<ns>/endless_gravity/dimensions/. */
    public static String toPrettyDatapackJson(CustomDimConfig config) {
        return GSON.toJson(toDimensionJson(config));
    }

    public static CustomDimConfig defaultOverworldProfile() {
        return new CustomDimConfig("minecraft:overworld", CustomDimConfig.Mode.ATMOSPHERE,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.075, AtmosphereLayers.GRAVITY_START_ALTITUDE, 4500.0, 0.01, 0.005,
                AtmosphereLayers.serialize(AtmosphereLayers.defaults()), 0.0, false, CustomDimConfig.FallDamageMode.INHERIT,
                1.0, 1.0, AtmosphereLayers.AUDIO_MUFFLE_START_ALTITUDE);
    }

    public static CustomDimConfig defaultEndProfile() {
        return new CustomDimConfig("minecraft:the_end", CustomDimConfig.Mode.FIXED_GRAVITY,
                0.055, 0.025, 0.025, 0.03, 0.018, 0.035,
                0.075, AtmosphereLayers.GRAVITY_START_ALTITUDE, 4500.0, 0.01, 0.005,
                AtmosphereLayers.serialize(AtmosphereLayers.defaults()), 0.0, false, CustomDimConfig.FallDamageMode.INHERIT,
                1.0, 1.0, AtmosphereLayers.AUDIO_MUFFLE_START_ALTITUDE);
    }

    public static CustomDimConfig defaultGenericProfile(String id, CustomDimConfig.Mode mode) {
        if (mode == CustomDimConfig.Mode.ATMOSPHERE) {
            return new CustomDimConfig(id, CustomDimConfig.Mode.ATMOSPHERE,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    0.075, AtmosphereLayers.GRAVITY_START_ALTITUDE, 4500.0, 0.01, 0.005,
                    AtmosphereLayers.serialize(AtmosphereLayers.defaults()), 0.0, false, CustomDimConfig.FallDamageMode.INHERIT,
                    1.0, 1.0, AtmosphereLayers.AUDIO_MUFFLE_START_ALTITUDE);
        }
        return new CustomDimConfig(id, CustomDimConfig.Mode.FIXED_GRAVITY,
                0.055, 0.025, 0.025, 0.03, 0.018, 0.035,
                0.075, AtmosphereLayers.GRAVITY_START_ALTITUDE, 4500.0, 0.01, 0.005,
                AtmosphereLayers.serialize(AtmosphereLayers.defaults()), 0.0, false, CustomDimConfig.FallDamageMode.INHERIT,
                1.0, 1.0, AtmosphereLayers.AUDIO_MUFFLE_START_ALTITUDE);
    }

    public static synchronized void applySyncedCustomDimensions(List<String> profiles) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        Map<String, CustomDimConfig> replacement = new java.util.LinkedHashMap<>();
        for (String profile : profiles) {
            if (replacement.size() >= MAX_CUSTOM_DIMENSIONS) break;
            try {
                CustomDimConfig config = parseDimension(JsonParser.parseString(profile).getAsJsonObject());
                replacement.put(config.getDimensionId(), config);
            } catch (RuntimeException e) {
                System.err.println("[Endless Gravity] Ignoring invalid synced dimension profile: " + e.getMessage());
            }
        }
        publishEffectiveDimensions(replacement);
    }

    public static synchronized void putDimensionProfile(CustomDimConfig profile) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        Map<String, CustomDimConfig> updated = new java.util.LinkedHashMap<>(userCustomDimensions);
        updated.put(profile.getDimensionId(), profile);
        saveCustomDimensions(List.copyOf(updated.values()));
    }

    public static synchronized boolean removeDimensionProfile(String dimensionId) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        if (!userCustomDimensions.containsKey(dimensionId)) return false;
        Map<String, CustomDimConfig> updated = new java.util.LinkedHashMap<>(userCustomDimensions);
        updated.remove(dimensionId);
        saveCustomDimensions(List.copyOf(updated.values()));
        return true;
    }

    public static synchronized void updateDimensionProfile(
            String dimensionId, java.util.function.UnaryOperator<CustomDimConfig> updater) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        CustomDimConfig existing = customDimensions.get(dimensionId);
        if (existing == null) return;
        Map<String, CustomDimConfig> updated = new java.util.LinkedHashMap<>(userCustomDimensions);
        updated.put(dimensionId, updater.apply(existing));
        saveCustomDimensions(List.copyOf(updated.values()));
    }

    public static synchronized void saveCustomDimensions(List<CustomDimConfig> dimensions) {
        if (serverSyncActive) return;
        if (dimensions.size() > MAX_CUSTOM_DIMENSIONS) {
            throw new IllegalArgumentException("At most " + MAX_CUSTOM_DIMENSIONS + " dimension profiles are supported");
        }
        Map<String, CustomDimConfig> replacement = new java.util.LinkedHashMap<>();
        for (CustomDimConfig dimension : dimensions) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(dimension.getDimensionId());
            if (id == null) throw new IllegalArgumentException("Invalid dimension ID: " + dimension.getDimensionId());
            List<String> layers = dimension.getAtmosphereLayers();
            if (layers.size() > CustomDimConfig.MAX_ATMOSPHERE_LAYERS) {
                throw new IllegalArgumentException("Too many atmosphere layers for " + id);
            }
            if (layers.size() < 2) {
                layers = AtmosphereLayers.serialize(AtmosphereLayers.defaults());
            }
            Double noFall = dimension.getNoFallDamageAltitude();
            Double sanitizedNoFall = (noFall != null && Double.isFinite(noFall) && noFall >= 0.0)
                    ? clamp(noFall, 0.0, 30000.0) : null;
            replacement.put(id.toString(), new CustomDimConfig(id.toString(), dimension.getMode(),
                    clamp(dimension.getPlayerGravityOffset(), -0.5, 0.5),
                    clamp(dimension.getMobGravityOffset(), -0.5, 0.5),
                    clamp(dimension.getItemGravityOffset(), -0.5, 0.5),
                    clamp(dimension.getArrowGravityOffset(), -0.5, 0.5),
                    clamp(dimension.getThrownGravityOffset(), -0.5, 0.5),
                    clamp(dimension.getBlockGravityOffset(), -0.5, 0.5),
                    clamp(dimension.getAtmosphereGravityMax(), 0.0, 0.5),
                    clamp(dimension.getAtmosphereBaseAltitude(), -2048.0, 30000.0),
                    sanitizedNoFall,
                    clamp(dimension.getAtmosphereMuffleGain(), 0.0, 1.0),
                    clamp(dimension.getAtmosphereMuffleGainHF(), 0.0, 1.0),
                    layers,
                    clamp(dimension.getSurfaceGravityOffset(), -0.5, 0.5),
                    dimension.isAtmosphereAffectNonLiving(),
                    dimension.getFallDamageMode(),
                    clamp(dimension.getFixedMuffleGain(), 0.0, 1.0),
                    clamp(dimension.getFixedMuffleGainHF(), 0.0, 1.0),
                    clamp(dimension.getAtmosphereMuffleStartAltitude(), -2048.0, 30000.0)));
        }
        userCustomDimensions = Map.copyOf(replacement);
        rebuildMergedDimensions();
        customDimensionsLoaded = true;
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("endless_gravity").resolve("dimensions.json");
        try {
            Files.createDirectories(configPath.getParent());
            writeCustomDimensions(configPath, replacement.values());
        } catch (IOException e) {
            throw new IllegalStateException("Could not save dimension profiles to " + configPath, e);
        }
    }

    public static synchronized List<AtmospherePreset> getAtmospherePresetsSafe() {
        try {
            return getAtmospherePresets();
        } catch (RuntimeException e) {
            System.err.println("[Endless Gravity] Falling back to empty atmosphere presets: " + e.getMessage());
            return List.of();
        }
    }

    public static synchronized List<AtmospherePreset> getAtmospherePresets() {
        Path path = FabricLoader.getInstance().getConfigDir()
                .resolve("endless_gravity").resolve("atmosphere_presets.json");
        if (!Files.exists(path)) return List.of();
        try {
            List<AtmospherePreset> presets = AtmospherePreset.parseImport(Files.readString(path));
            validateUniquePresetNames(presets);
            return presets;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read atmosphere presets from " + path, e);
        } catch (RuntimeException e) {
            System.err.println("[Endless Gravity] Invalid atmosphere presets in " + path + ": " + e.getMessage());
            throw new IllegalStateException("Could not load atmosphere presets from " + path, e);
        }
    }

    public static synchronized void saveAtmospherePresets(List<AtmospherePreset> presets) {
        validateUniquePresetNames(presets);
        Path path = FabricLoader.getInstance().getConfigDir()
                .resolve("endless_gravity").resolve("atmosphere_presets.json");
        Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(tempPath, AtmospherePreset.exportJson(presets));
            try {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save atmosphere presets to " + path, e);
        }
    }

    private static void validateUniquePresetNames(List<AtmospherePreset> presets) {
        if (presets.size() > AtmospherePreset.MAX_PRESETS) {
            throw new IllegalArgumentException("At most " + AtmospherePreset.MAX_PRESETS + " presets are supported");
        }
        HashSet<String> names = new HashSet<>();
        for (AtmospherePreset preset : presets) {
            if (!names.add(preset.name().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate atmosphere preset name: " + preset.name());
            }
        }
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