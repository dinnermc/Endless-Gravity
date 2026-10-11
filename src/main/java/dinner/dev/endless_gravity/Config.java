package dinner.dev.endless_gravity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
                List.of("-64:1.25", "64:1.0", "400:0.5", "900:0.2", "1200:0.08", "1800:0.01", "2500:0.001", "3500:0.0")));
    }
}