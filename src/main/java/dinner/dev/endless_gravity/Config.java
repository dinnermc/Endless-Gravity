package dinner.dev.endless_gravity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class Config {
    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    public static class Common {
        // Gravity toggles
        public final ModConfigSpec.BooleanValue enablePlayerGravity;
        public final ModConfigSpec.BooleanValue enableItemGravity;
        public final ModConfigSpec.BooleanValue enableArrowGravity;
        public final ModConfigSpec.BooleanValue enableThrownGravity;
        public final ModConfigSpec.BooleanValue endEntityGravity;

        // Gravity values
        public final ModConfigSpec.DoubleValue playerGravityOffset;
        public final ModConfigSpec.DoubleValue itemGravityOffset;
        public final ModConfigSpec.DoubleValue arrowGravityOffset;
        public final ModConfigSpec.DoubleValue thrownGravityOffset;

        // Effects toggles
        public final ModConfigSpec.BooleanValue enableLowPassFilter;

        // Effects values
        public final ModConfigSpec.DoubleValue lowPassGain;
        public final ModConfigSpec.DoubleValue lowPassGainHF;

        // Gameplay
        public final ModConfigSpec.IntValue fallDamageMode;
        public final ModConfigSpec.BooleanValue enableBlockGravity;
        public final ModConfigSpec.DoubleValue blockGravityOffset;

        // Sable
        public final ModConfigSpec.DoubleValue sableGravityY;
        public final ModConfigSpec.DoubleValue sablePressure;
        public final ModConfigSpec.DoubleValue sableDrag;
        public final ModConfigSpec.IntValue sableDatapackPriority;
        public final ModConfigSpec.BooleanValue endSableGravity;

        // Sable Overworld
        public final ModConfigSpec.IntValue overworldSableDatapackPriority;
        public final ModConfigSpec.BooleanValue overworldSableGravity;

        // Atmosphere
        public final ModConfigSpec.BooleanValue enableAtmosphere;
        public final ModConfigSpec.BooleanValue overworldEntityGravity;
        public final ModConfigSpec.DoubleValue atmosphereGravityMax;
        public final ModConfigSpec.DoubleValue noFallDamageAltitude;
        public final ModConfigSpec.DoubleValue atmosphereMuffleGain;
        public final ModConfigSpec.DoubleValue atmosphereMuffleGainHF;
        public final ModConfigSpec.DoubleValue particleGravityOffset;
        public final ModConfigSpec.ConfigValue<List<? extends String>> atmosphereLayers;

        // Mob gravity
        public final ModConfigSpec.BooleanValue enableMobGravity;
        public final ModConfigSpec.DoubleValue mobGravityOffset;

        public Common(ModConfigSpec.Builder builder) {
            builder.push("gravity");

            endEntityGravity = builder
                    .comment("Master switch for End gravity. Off = vanilla gravity for everything, ignoring the toggles below.")
                    .define("endEntityGravity", true);

            enablePlayerGravity = builder
                    .comment("Reduced gravity for players in The End.")
                    .define("enablePlayerGravity", true);
            playerGravityOffset = builder
                    .comment("Upward impulse per tick for players. 0.055 leaves a gentle float; crank it up and you barely fall at all.")
                    .defineInRange("playerGravityOffset", 0.055, 0.0, 0.07);

            enableItemGravity = builder
                    .comment("Reduced gravity for dropped items in The End.")
                    .define("enableItemGravity", true);
            itemGravityOffset = builder
                    .comment("Same impulse as the player offset, but for items. Lower because items have no way to steer.")
                    .defineInRange("itemGravityOffset", 0.025, 0.0, 0.035);

            enableArrowGravity = builder
                    .comment("Reduced gravity for arrows and tridents in The End.")
                    .define("enableArrowGravity", true);
            arrowGravityOffset = builder
                    .comment("Upward impulse per tick for arrows and tridents.")
                    .defineInRange("arrowGravityOffset", 0.03, 0.0, 0.04);

            enableThrownGravity = builder
                    .comment("Reduced gravity for thrown projectiles (snowballs, pearls, bottles...) in The End.")
                    .define("enableThrownGravity", true);
            thrownGravityOffset = builder
                    .comment("Upward impulse per tick for thrown projectiles.")
                    .defineInRange("thrownGravityOffset", 0.018, 0.0, 0.025);

            // Mob gravity (new)
            enableMobGravity = builder
                    .comment("Reduced gravity for all mobs in The End.")
                    .define("enableMobGravity", true);
            mobGravityOffset = builder
                    .comment("Upward impulse per tick for mobs in The End. Lower because mobs have no way to steer.")
                    .defineInRange("mobGravityOffset", 0.025, 0.0, 0.035);

            builder.pop();

            builder.push("effects");

            enableLowPassFilter = builder
                    .comment("Muffles The End's audio with a low-pass filter, like being underwater.")
                    .define("enableLowPassFilter", true);
            lowPassGain = builder
                    .comment("Filter volume. 0.35 is a subtle dampening; go lower for a heavier blanket.")
                    .defineInRange("lowPassGain", 0.35, 0.0, 1.0);
            lowPassGainHF = builder
                    .comment("How much high-frequency sound survives the filter. Lower = duller, boomier audio.")
                    .defineInRange("lowPassGainHF", 0.25, 0.0, 1.0);

            builder.pop();

            builder.push("gameplay");

            fallDamageMode = builder
                    .comment("Fall damage in The End and Sable dimensions: 0 = vanilla, 1 = disabled.")
                    .defineInRange("fallDamageMode", 1, 0, 1);

            enableBlockGravity = builder
                    .comment("Falling blocks (sand, gravel, anvils, dragon eggs) fall slower in The End.")
                    .define("enableBlockGravity", true);
            blockGravityOffset = builder
                    .comment("Upward impulse per tick for falling blocks.")
                    .defineInRange("blockGravityOffset", 0.035, 0.0, 0.1);

            builder.pop();

            builder.push("sable");

            endSableGravity = builder
                    .comment("Master switch for the End Sable physics pack. Off = Sable falls back to its own defaults.")
                    .define("endSableGravity", true);

            sableGravityY = builder
                    .comment("Downward pull in The End. Negative = down; the more negative, the stronger.")
                    .defineInRange("sableGravityY", -4.0, -20.0, 0.0);
            sablePressure = builder
                    .comment("Air pressure in The End. 0 = vacuum.")
                    .defineInRange("sablePressure", 0.0, 0.0, 10.0);
            sableDrag = builder
                    .comment("Air drag in The End. 0 = no resistance.")
                    .defineInRange("sableDrag", 0.05, 0.0, 10.0);
            sableDatapackPriority = builder
                    .comment("Datapack priority for the End pack. Sable's built-ins sit at 1000, so anything above that wins.")
                    .defineInRange("sableDatapackPriority", 9999, 1, 9999);

            builder.pop();

            builder.push("sable_overworld");

            overworldSableGravity = builder
                    .comment("Master switch for the Overworld Sable physics pack. Off = Sable reuses its own defaults.")
                    .define("overworldSableGravity", true);

            overworldSableDatapackPriority = builder
                    .comment("Datapack priority for the Overworld pack. Must beat Sable's built-in 1000.")
                    .defineInRange("overworldSableDatapackPriority", 2000, 1, 9999);

            builder.pop();

            builder.push("atmosphere");

            enableAtmosphere = builder
                    .comment("Turns on the Overworld's altitude physics: gravity eases off above Y=64 and fades to vacuum by Y=3500. Also applies inside Sable sub-levels.")
                    .define("enableAtmosphere", true);
            overworldEntityGravity = builder
                    .comment("Master switch for the atmosphere's effect on entities. Off = full vanilla gravity at every altitude.")
                    .define("overworldEntityGravity", true);
            atmosphereGravityMax = builder
                    .comment("Upward impulse per tick at deep space, ramped from 0 at Y=64. Keep under 0.08 so a residual pull always remains - never perfect zero-G.")
                    .defineInRange("atmosphereGravityMax", 0.075, 0.0, 0.1);
            noFallDamageAltitude = builder
                    .comment("Above this Y, fall damage is cancelled in the Overworld (and Sable sub-levels). 400 = the air is already thin enough that falls stop hurting; 3500 = only at the top of the atmosphere.")
                    .defineInRange("noFallDamageAltitude", 400.0, 64.0, 3500.0);
            atmosphereMuffleGain = builder
                    .comment("Low-pass gain at deep space, interpolated from 1.0 at the base. 0.01 is effectively silence.")
                    .defineInRange("atmosphereMuffleGain", 0.01, 0.0, 1.0);
            atmosphereMuffleGainHF = builder
                    .comment("High-frequency gain at deep space, same interpolation as the gain above.")
                    .defineInRange("atmosphereMuffleGainHF", 0.005, 0.0, 1.0);
            particleGravityOffset = builder
                    .comment("Upward acceleration per tick for particles in The End. 0.08 = neutral gravity (float), <0.08 = slow fall, >0.08 = slow rise")
                    .defineInRange("particleGravityOffset", 0.5, 0.0, 0.5);
            atmosphereLayers = builder
                    .comment("\"altitude:pressure\" pairs defining the pressure curve - 1.0 = full atmosphere, 0.0 = vacuum. Gravity, muffling and the Sable datapack all read this curve. Defaults: -64:1.25, 64:1.0, 400:0.5, 900:0.2, 1200:0.08, 1800:0.01, 2500:0.001, 3500:0.0")
                    .defineListAllowEmpty("atmosphereLayers",
                            List.of("-64:1.25", "64:1.0", "400:0.5", "900:0.2", "1200:0.08", "1800:0.01", "2500:0.001", "3500:0.0"),
                            obj -> obj instanceof String);

            builder.pop();
        }
    }

    // Runtime cache for custom dimensions
    private static final Map<String, CustomDimConfig> CUSTOM_DIMENSIONS = new HashMap<>();
    private static boolean customDimensionsLoaded = false;

    public static void loadCustomDimensions() {
        if (customDimensionsLoaded) return;
        CUSTOM_DIMENSIONS.clear();

        Path configPath = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("endless_gravity").resolve("dimensions.json");
        if (!Files.exists(configPath)) {
            createDefaultDimensionsConfig(configPath);
        }

        try {
            parseDimensionsFile(configPath);
            customDimensionsLoaded = true;
        } catch (Exception e) {
            // Corrupt or truncated file: back it up, regenerate the default, retry once.
            System.err.println("[Endless Gravity] Failed to load custom dimensions config (" + e.getMessage() + "); regenerating default.");
            try {
                Files.move(configPath, configPath.resolveSibling(configPath.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
            createDefaultDimensionsConfig(configPath);
            try {
                parseDimensionsFile(configPath);
                customDimensionsLoaded = true;
            } catch (Exception e2) {
                System.err.println("[Endless Gravity] Failed to load regenerated custom dimensions config: " + e2.getMessage());
                customDimensionsLoaded = true;
            }
        }
    }

    private static void parseDimensionsFile(Path configPath) throws IOException {
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
    }

    private static void createDefaultDimensionsConfig(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());

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
        return getCustomDimConfig(dimensionId)
                .map(CustomDimConfig::isEnableMobGravity)
                .orElse(false);
    }

    public static Optional<CustomDimConfig> getCustomDimConfigByKey(ResourceKey<Level> dimension) {
        return getCustomDimConfig(dimension.location().toString());
    }
}