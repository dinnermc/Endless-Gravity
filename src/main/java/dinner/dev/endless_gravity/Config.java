package dinner.dev.endless_gravity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class Config {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_CUSTOM_DIMENSIONS = 512;
    private static final Gson GSON = new Gson();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();
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
        public final ModConfigSpec.IntValue mobFallDamageMode;
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
        public final ModConfigSpec.BooleanValue enableAtmosphericDrag;
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

        // Potion gravity
        public final ModConfigSpec.BooleanValue enablePotionGravity;
        public final ModConfigSpec.DoubleValue weightlessnessGravityOffset;
        public final ModConfigSpec.DoubleValue heavinessGravityOffset;

        // HUD (client-side only)
        public final ModConfigSpec.BooleanValue enableGravityHud;
        public final ModConfigSpec.IntValue hudPosition;
        public final ModConfigSpec.BooleanValue hudShowPressure;
        public final ModConfigSpec.BooleanValue hudCompact;

        // Extra entity IDs excluded from reduced gravity, in addition to the gravity_immune tag
        public final ModConfigSpec.ConfigValue<List<? extends String>> gravityImmuneEntities;

        public Common(ModConfigSpec.Builder builder) {
            builder.push("gravity");

            endEntityGravity = builder
                    .comment("Legacy End-profile default used when creating/migrating dimensions.json; also gates End particle gravity.")
                    .define("endEntityGravity", true);

            enablePlayerGravity = builder
                    .comment("Legacy End-profile default. Edit the minecraft:the_end profile in dimensions.json.")
                    .define("enablePlayerGravity", true);
            playerGravityOffset = builder
                    .comment("Legacy/default End profile value; edit dimensions.json after the profiles have been created.")
                    .defineInRange("playerGravityOffset", 0.055, 0.0, 0.07);

            enableItemGravity = builder
                    .comment("Legacy End-profile default. Edit the minecraft:the_end profile in dimensions.json.")
                    .define("enableItemGravity", true);
            itemGravityOffset = builder
                    .comment("Legacy/default End profile value; edit dimensions.json after the profiles have been created.")
                    .defineInRange("itemGravityOffset", 0.025, 0.0, 0.035);

            enableArrowGravity = builder
                    .comment("Legacy End-profile default. Edit the minecraft:the_end profile in dimensions.json.")
                    .define("enableArrowGravity", true);
            arrowGravityOffset = builder
                    .comment("Legacy/default End profile value; edit dimensions.json after the profiles have been created.")
                    .defineInRange("arrowGravityOffset", 0.03, 0.0, 0.04);

            enableThrownGravity = builder
                    .comment("Legacy End-profile default. Edit the minecraft:the_end profile in dimensions.json.")
                    .define("enableThrownGravity", true);
            thrownGravityOffset = builder
                    .comment("Legacy/default End profile value; edit dimensions.json after the profiles have been created.")
                    .defineInRange("thrownGravityOffset", 0.018, 0.0, 0.025);

            // Mob gravity (new)
            enableMobGravity = builder
                    .comment("Legacy End-profile default. Edit the minecraft:the_end profile in dimensions.json.")
                    .define("enableMobGravity", true);
            mobGravityOffset = builder
                    .comment("Legacy/default End profile value; edit dimensions.json after the profiles have been created.")
                    .defineInRange("mobGravityOffset", 0.025, 0.0, 0.035);

            gravityImmuneEntities = builder
                    .comment("Entity IDs (e.g. minecraft:zombie) excluded from reduced gravity, in addition to the gravity_immune tag. The tag needs a datapack; this is the per-install alternative.")
                    .defineListAllowEmpty("gravityImmuneEntities", List.<String>of(), () -> "", o -> o instanceof String);

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
                    .comment("Fall damage in The End and Sable dimensions: 0 = vanilla, 1 = disabled, 2 = proportional to gravity.")
                    .defineInRange("fallDamageMode", 1, 0, 2);

            mobFallDamageMode = builder
                    .comment("Fall damage for mobs in The End and Sable dimensions: 0 = vanilla, 1 = disabled, 2 = proportional to gravity.")
                    .defineInRange("mobFallDamageMode", 1, 0, 2);

            enableBlockGravity = builder
                    .comment("Legacy End-profile default. Edit the minecraft:the_end profile in dimensions.json.")
                    .define("enableBlockGravity", true);
            blockGravityOffset = builder
                    .comment("Legacy/default End profile value; edit dimensions.json after the profiles have been created.")
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
                    .defineInRange("overworldSableDatapackPriority", 9999, 1, 9999);

            builder.pop();

            builder.push("atmosphere");

            enableAtmosphere = builder
                    .comment("Master switch for the global Sable sub-level atmosphere. Dimension profile modes control gravity in Minecraft dimensions.")
                    .define("enableAtmosphere", true);
            enableAtmosphericDrag = builder
                    .comment("Optional pressure-based vertical air drag in atmosphere dimensions: lower air resistance in vacuum and thicker drag in dense atmospheres.")
                    .define("enableAtmosphericDrag", false);
            overworldEntityGravity = builder
                    .comment("Legacy Overworld profile default used when dimensions.json is first created or migrated.")
                    .define("overworldEntityGravity", true);
            atmosphereGravityMax = builder
                    .comment("Maximum upward impulse per tick for Sable sub-level gravity reduction and the initial Overworld profile.")
                    .defineInRange("atmosphereGravityMax", 0.075, 0.0, 0.1);
            noFallDamageAltitude = builder
                    .comment("Global Sable sub-level fall-damage threshold and initial Overworld profile default.")
                    .defineInRange("noFallDamageAltitude", 4500.0, 64.0, 6000.0);
            atmosphereMuffleGain = builder
                    .comment("Global Sable audio fallback and initial Overworld profile default.")
                    .defineInRange("atmosphereMuffleGain", 0.01, 0.0, 1.0);
            atmosphereMuffleGainHF = builder
                    .comment("Global Sable audio fallback and initial Overworld profile default.")
                    .defineInRange("atmosphereMuffleGainHF", 0.005, 0.0, 1.0);
            particleGravityOffset = builder
                    .comment("Upward acceleration per tick for particles in The End. 0.08 = neutral gravity (float), <0.08 = slow fall, >0.08 = slow rise")
                    .defineInRange("particleGravityOffset", 0.1, 0.0, 0.5);
            atmosphereLayers = builder
                    .comment("Global Sable pressure curve and initial curve for new atmosphere profiles. Profile curves are stored in dimensions.json.")
                    .defineListAllowEmpty("atmosphereLayers",
                            List.of("-64:1.25", "64:1.0", "400:0.5", "1200:0.4", "2000:0.3", "3000:0.2", "4500:0.1", "6000:0.0"),
                            obj -> obj instanceof String);

            builder.pop();

            builder.push("potions");

            enablePotionGravity = builder
                    .comment("The Weightlessness (lighter) and Heaviness (heavier) status effects change entity gravity.")
                    .define("enablePotionGravity", true);
            weightlessnessGravityOffset = builder
                    .comment("Upward impulse per tick per amplifier level from the Weightlessness effect.")
                    .defineInRange("weightlessnessGravityOffset", 0.05, 0.0, 0.07);
            heavinessGravityOffset = builder
                    .comment("Extra downward pull per tick per amplifier level from the Heaviness effect.")
                    .defineInRange("heavinessGravityOffset", 0.02, 0.0, 0.07);

            builder.pop();

            builder.push("hud");

            enableGravityHud = builder
                    .comment("Draw a small on-screen readout of the player's current gravity.")
                    .define("enableGravityHud", false);
            hudPosition = builder
                    .comment("Corner of the screen for the Gravity HUD: 0 = top-left, 1 = top-right, 2 = bottom-left, 3 = bottom-right.")
                    .defineInRange("hudPosition", 0, 0, 3);
            hudShowPressure = builder
                    .comment("Show atmospheric pressure alongside gravity when in an atmosphere dimension.")
                    .define("hudShowPressure", true);
            hudCompact = builder
                    .comment("Compact Gravity HUD showing only g-force (and pressure if enabled), hiding raw offset and source tag.")
                    .define("hudCompact", false);

            builder.pop();
        }
    }

    // Thread-safe immutable snapshots for custom dimensions (lock-free reads on entity ticks).
    // userCustomDimensions holds profiles from config/endless_gravity/dimensions.json;
    // datapackDimensions holds lower-priority profiles from datapacks;
    // customDimensions / customDimensionsByLocation hold the effective merged view (or server-synced view).
    private static Map<String, CustomDimConfig> userCustomDimensions = Map.of();
    private static Map<String, CustomDimConfig> datapackDimensions = Map.of();
    private static volatile Map<String, CustomDimConfig> customDimensions = Map.of();
    private static volatile Map<ResourceLocation, CustomDimConfig> customDimensionsByLocation = Map.of();
    private static volatile boolean customDimensionsLoaded = false;

    private static void publishEffectiveDimensions(Map<String, CustomDimConfig> map) {
        Map<String, CustomDimConfig> byId = new LinkedHashMap<>(map);
        Map<ResourceLocation, CustomDimConfig> byLoc = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, CustomDimConfig> entry : byId.entrySet()) {
            ResourceLocation loc = ResourceLocation.tryParse(entry.getKey());
            if (loc != null) {
                byLoc.put(loc, entry.getValue());
            }
        }
        customDimensions = Map.copyOf(byId);
        customDimensionsByLocation = Map.copyOf(byLoc);
    }

    private static void rebuildMergedDimensions() {
        Map<String, CustomDimConfig> merged = new LinkedHashMap<>(datapackDimensions);
        merged.putAll(userCustomDimensions); // user config always takes precedence over datapacks
        publishEffectiveDimensions(merged);
    }

    /** Replaces the lower-priority datapack profiles (called on server data reload). */
    public static synchronized void setDatapackDimensions(Map<String, CustomDimConfig> profiles) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        datapackDimensions = profiles == null ? Map.of() : Map.copyOf(profiles);
        rebuildMergedDimensions();
    }

    /** Public JSON parser used by both dimensions.json and datapack profiles. */
    public static CustomDimConfig parseDimensionJson(JsonElement element) {
        return parseDimension(element);
    }

    public static synchronized void loadCustomDimensions() {
        if (customDimensionsLoaded) return;
        Map<String, CustomDimConfig> loaded = new LinkedHashMap<>();

        Path configPath = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("endless_gravity").resolve("dimensions.json");
        if (!Files.exists(configPath)) {
            createDefaultDimensionsConfig(configPath);
        }

        try {
            parseDimensionsFile(configPath, loaded);
        } catch (Exception e) {
            LOGGER.error("Could not read dimension profiles from {}. The original file will be backed up.", configPath, e);
            try {
                Files.move(configPath, configPath.resolveSibling(configPath.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException backupError) {
                LOGGER.error("Could not back up invalid dimension profiles at {}", configPath, backupError);
            }
            loaded.clear();
            addBuiltInDefaults(loaded);
            try {
                writeDimensionsFile(configPath, loaded.values());
            } catch (IOException writeError) {
                LOGGER.error("Could not recreate dimension profiles at {}", configPath, writeError);
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
        if (dimensions.size() > MAX_CUSTOM_DIMENSIONS) {
            LOGGER.warn("Only the first {} dimension profiles will be loaded", MAX_CUSTOM_DIMENSIONS);
        }
        for (JsonElement element : dimensions) {
            if (target.size() >= MAX_CUSTOM_DIMENSIONS) break;
            try {
                CustomDimConfig config = parseDimension(element);
                if (target.putIfAbsent(config.getDimensionId(), config) != null) {
                    LOGGER.warn("Ignoring duplicate dimension profile for {}", config.getDimensionId());
                }
            } catch (RuntimeException e) {
                LOGGER.warn("Ignoring invalid dimension profile: {}", e.getMessage());
            }
        }
    }

    private static CustomDimConfig parseDimension(JsonElement element) {
        JsonObject obj = element.getAsJsonObject();
        String id = stringValue(obj, "id", stringValue(obj, "dimension", ""));
        ResourceLocation parsedId = ResourceLocation.tryParse(id);
        if (parsedId == null) throw new IllegalArgumentException("invalid dimension ID '" + id + "'");
        id = parsedId.toString();

        JsonElement modeElement = obj.get("mode");
        CustomDimConfig.Mode mode;
        boolean legacyProfile = modeElement == null;
        boolean legacyDisabled = false;
        boolean legacyAtmosphereEnabled = false;
        if (!legacyProfile) {
            if (!modeElement.isJsonPrimitive() || !modeElement.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("invalid mode for " + id);
            }
            mode = switch (modeElement.getAsString().toLowerCase(Locale.ROOT)) {
                case "atmosphere" -> CustomDimConfig.Mode.ATMOSPHERE;
                case "fixed_gravity", "fixed-gravity", "gravity" -> CustomDimConfig.Mode.FIXED_GRAVITY;
                default -> throw new IllegalArgumentException("invalid mode for " + id);
            };
        } else {
            boolean hasLegacyMode = obj.has("enableAtmosphere") || obj.has("enableCustomGravity");
            boolean legacyAtmosphere = boolValue(obj, "enableAtmosphere", id.equals("minecraft:overworld"));
            boolean legacyGravity = boolValue(obj, "enableCustomGravity", id.equals("minecraft:the_end"));
            legacyAtmosphereEnabled = legacyAtmosphere;
            legacyDisabled = hasLegacyMode && !legacyAtmosphere && !legacyGravity;
            mode = legacyGravity ? CustomDimConfig.Mode.FIXED_GRAVITY
                    : legacyAtmosphere ? CustomDimConfig.Mode.ATMOSPHERE
                    : (hasLegacyMode || !id.equals("minecraft:overworld")
                    ? CustomDimConfig.Mode.FIXED_GRAVITY : CustomDimConfig.Mode.ATMOSPHERE);
        }

        JsonObject gravity = obj.has("gravity") && obj.get("gravity").isJsonObject()
                ? obj.getAsJsonObject("gravity") : obj;
        double player = numberValue(gravity, "player", numberValue(obj, "playerGravityOffset",
                id.equals("minecraft:the_end") && !COMMON.endEntityGravity.get() ? 0.0 : COMMON.playerGravityOffset.get()));
        double mob = numberValue(gravity, "mob", numberValue(obj, "mobGravityOffset",
                numberValue(obj, "mobGravityOffsetCustom", id.equals("minecraft:the_end")
                        ? COMMON.mobGravityOffset.get() : 0.0)));
        double item = numberValue(gravity, "item", numberValue(obj, "itemGravityOffset",
                id.equals("minecraft:the_end") ? COMMON.itemGravityOffset.get() : 0.0));
        double arrow = numberValue(gravity, "arrow", numberValue(obj, "arrowGravityOffset",
                id.equals("minecraft:the_end") ? COMMON.arrowGravityOffset.get() : 0.0));
        double thrown = numberValue(gravity, "thrown", numberValue(obj, "thrownGravityOffset",
                id.equals("minecraft:the_end") ? COMMON.thrownGravityOffset.get() : 0.0));
        double block = numberValue(gravity, "block", numberValue(obj, "blockGravityOffset",
                id.equals("minecraft:the_end") ? COMMON.blockGravityOffset.get() : 0.0));
        if (legacyDisabled) player = mob = item = arrow = thrown = block = 0.0;
        if (legacyProfile && id.equals("minecraft:the_end")) {
            if (!COMMON.endEntityGravity.get()) {
                player = mob = item = arrow = thrown = block = 0.0;
            } else {
                if (!boolValue(obj, "enablePlayerGravity", true)) player = 0.0;
                if (!boolValue(obj, "enableMobGravity", true)) mob = 0.0;
                if (!boolValue(obj, "enableItemGravity", true)) item = 0.0;
                if (!boolValue(obj, "enableArrowGravity", true)) arrow = 0.0;
                if (!boolValue(obj, "enableThrownGravity", true)) thrown = 0.0;
                if (!boolValue(obj, "enableBlockGravity", true)) block = 0.0;
            }
        }

        JsonObject atmosphere = obj.has("atmosphere") && obj.get("atmosphere").isJsonObject()
                ? obj.getAsJsonObject("atmosphere") : obj;
        double baseAltitude = numberValue(atmosphere, "start_altitude",
                numberValue(atmosphere, "atmosphereBaseAltitude", AtmosphereLayers.GRAVITY_START_ALTITUDE));
        if (legacyProfile && id.equals("minecraft:overworld")) {
            baseAltitude = AtmosphereLayers.GRAVITY_START_ALTITUDE;
        }
        double maxGravity = numberValue(atmosphere, "gravity_offset",
                numberValue(atmosphere, "atmosphereGravityMax", COMMON.atmosphereGravityMax.get()));
        if (legacyProfile && id.equals("minecraft:overworld") && !COMMON.overworldEntityGravity.get()) maxGravity = 0.0;
        if (legacyProfile && id.equals("minecraft:overworld") && !COMMON.enableAtmosphere.get()) {
            mode = CustomDimConfig.Mode.FIXED_GRAVITY;
            player = mob = item = arrow = thrown = block = maxGravity = 0.0;
        }
        Double noFallDamage = null;
        if (obj.has("no_fall_damage_above")) {
            JsonElement threshold = obj.get("no_fall_damage_above");
            if (threshold != null && !threshold.isJsonNull()) noFallDamage = numeric(threshold, "no_fall_damage_above");
        } else {
            boolean useLegacyAtmosphereThreshold = legacyProfile
                    ? legacyAtmosphereEnabled : mode == CustomDimConfig.Mode.ATMOSPHERE;
            double fallback = !useLegacyAtmosphereThreshold ? -1.0
                    : id.equals("minecraft:overworld") ? COMMON.noFallDamageAltitude.get()
                    : numberValue(obj, "atmosphereMaxAltitude", COMMON.noFallDamageAltitude.get());
            double legacyThreshold = numberValue(obj, "noFallDamageAltitude", fallback);
            if (legacyThreshold >= 0.0) noFallDamage = legacyThreshold;
        }
        double gain = numberValue(atmosphere, "muffle_gain",
                numberValue(atmosphere, "atmosphereMuffleGain", COMMON.atmosphereMuffleGain.get()));
        double gainHF = numberValue(atmosphere, "muffle_gain_hf",
                numberValue(atmosphere, "atmosphereMuffleGainHF", COMMON.atmosphereMuffleGainHF.get()));
        List<String> layers = stringList(atmosphere, "layers", "atmosphereLayers");
        if (layers.size() < 2) layers = defaultLayers();
        if (layers.size() > CustomDimConfig.MAX_ATMOSPHERE_LAYERS) {
            throw new IllegalArgumentException("too many atmosphere layers for " + id);
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

        return new CustomDimConfig(id, mode,
                clamp(player, -0.5, 0.5), clamp(mob, -0.5, 0.5),
                clamp(item, -0.5, 0.5), clamp(arrow, -0.5, 0.5),
                clamp(thrown, -0.5, 0.5), clamp(block, -0.5, 0.5),
                clamp(maxGravity, 0.0, 0.5), clamp(baseAltitude, -2048.0, 30000.0),
                noFallDamage != null && Double.isFinite(noFallDamage) && noFallDamage >= 0.0 ? noFallDamage : null,
                clamp(gain, 0.0, 1.0), clamp(gainHF, 0.0, 1.0), layers,
                clamp(surfaceGravity, -0.5, 0.5), affectNonLiving, dimFallDamage,
                clamp(fixedMuffleGain, 0.0, 1.0), clamp(fixedMuffleGainHF, 0.0, 1.0));
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

    private static double numberValue(JsonObject obj, String key, double fallback) {
        JsonElement value = obj.get(key);
        return value == null || value.isJsonNull() ? fallback : numeric(value, key);
    }

    private static double numeric(JsonElement element, String key) {
        double value = element.getAsDouble();
        if (!Double.isFinite(value)) throw new IllegalArgumentException("non-finite value for " + key);
        return value;
    }

    private static String stringValue(JsonObject obj, String key, String fallback) {
        JsonElement value = obj.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }

    private static boolean boolValue(JsonObject obj, String key, boolean fallback) {
        JsonElement value = obj.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsBoolean();
    }

    private static List<String> stringList(JsonObject obj, String newKey, String oldKey) {
        JsonElement value = obj.has(newKey) ? obj.get(newKey) : obj.get(oldKey);
        List<String> layers = new ArrayList<>();
        if (value != null && value.isJsonArray()) {
            for (JsonElement element : value.getAsJsonArray()) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    layers.add(element.getAsString());
                }
            }
        }
        return layers;
    }

    private static void addBuiltInDefaults(Map<String, CustomDimConfig> dimensions) {
        dimensions.putIfAbsent("minecraft:overworld", defaultOverworld());
        dimensions.putIfAbsent("minecraft:the_end", defaultEnd());
    }

    private static CustomDimConfig defaultOverworld() {
        boolean atmosphereEnabled = COMMON.enableAtmosphere.get();
        return new CustomDimConfig("minecraft:overworld",
                atmosphereEnabled ? CustomDimConfig.Mode.ATMOSPHERE : CustomDimConfig.Mode.FIXED_GRAVITY,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                atmosphereEnabled && COMMON.overworldEntityGravity.get() ? COMMON.atmosphereGravityMax.get() : 0.0,
                AtmosphereLayers.GRAVITY_START_ALTITUDE,
                atmosphereEnabled ? COMMON.noFallDamageAltitude.get() : null, COMMON.atmosphereMuffleGain.get(),
                COMMON.atmosphereMuffleGainHF.get(), defaultLayers());
    }

    private static CustomDimConfig defaultEnd() {
        boolean enabled = COMMON.endEntityGravity.get();
        return new CustomDimConfig("minecraft:the_end", CustomDimConfig.Mode.FIXED_GRAVITY,
                enabled && COMMON.enablePlayerGravity.get() ? COMMON.playerGravityOffset.get() : 0.0,
                enabled && COMMON.enableMobGravity.get() ? COMMON.mobGravityOffset.get() : 0.0,
                enabled && COMMON.enableItemGravity.get() ? COMMON.itemGravityOffset.get() : 0.0,
                enabled && COMMON.enableArrowGravity.get() ? COMMON.arrowGravityOffset.get() : 0.0,
                enabled && COMMON.enableThrownGravity.get() ? COMMON.thrownGravityOffset.get() : 0.0,
                enabled && COMMON.enableBlockGravity.get() ? COMMON.blockGravityOffset.get() : 0.0,
                0.075, AtmosphereLayers.GRAVITY_START_ALTITUDE, null,
                COMMON.atmosphereMuffleGain.get(), COMMON.atmosphereMuffleGainHF.get(),
                defaultLayers());
    }

    private static List<String> defaultLayers() {
        List<String> configured = new ArrayList<>(COMMON.atmosphereLayers.get());
        List<AtmosphereLayers.Layer> parsed = AtmosphereLayers.parseUncached(configured);
        if (parsed.size() <= CustomDimConfig.MAX_ATMOSPHERE_LAYERS) {
            return AtmosphereLayers.serialize(parsed);
        }
        LOGGER.warn("Global atmosphere curve has more than {} layers; retaining the first {} and the final layer",
                CustomDimConfig.MAX_ATMOSPHERE_LAYERS, CustomDimConfig.MAX_ATMOSPHERE_LAYERS - 1);
        List<AtmosphereLayers.Layer> limited = new ArrayList<>(
                parsed.subList(0, CustomDimConfig.MAX_ATMOSPHERE_LAYERS - 1));
        limited.add(parsed.get(parsed.size() - 1));
        return AtmosphereLayers.serialize(limited);
    }

    public static CustomDimConfig createDimensionProfile(String dimensionId, CustomDimConfig.Mode mode) {
        return new CustomDimConfig(dimensionId, mode,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                COMMON.atmosphereGravityMax.get(), AtmosphereLayers.GRAVITY_START_ALTITUDE,
                mode == CustomDimConfig.Mode.ATMOSPHERE ? COMMON.noFallDamageAltitude.get() : null,
                COMMON.atmosphereMuffleGain.get(), COMMON.atmosphereMuffleGainHF.get(),
                defaultLayers());
    }

    private static void createDefaultDimensionsConfig(Path configPath) {
        Map<String, CustomDimConfig> defaults = new LinkedHashMap<>();
        addBuiltInDefaults(defaults);
        try {
            writeDimensionsFile(configPath, defaults.values());
        } catch (IOException e) {
            LOGGER.error("Failed to create default dimension profiles at {}", configPath, e);
        }
    }

    private static void writeDimensionsFile(Path configPath, Collection<CustomDimConfig> dimensions) throws IOException {
        Files.createDirectories(configPath.getParent());
        JsonArray array = new JsonArray();
        dimensions.stream().sorted(Comparator.comparing(CustomDimConfig::getDimensionId))
                .forEach(config -> array.add(toJson(config)));
        JsonObject root = new JsonObject();
        root.add("dimensions", array);
        String json = GSON_PRETTY.toJson(root);
        Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");
        Files.writeString(tempPath, json);
        try {
            Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static JsonObject toJson(CustomDimConfig config) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", config.getDimensionId());
        obj.addProperty("mode", config.getMode().name().toLowerCase(Locale.ROOT));
        JsonObject gravity = new JsonObject();
        gravity.addProperty("player", config.getPlayerGravityOffset());
        gravity.addProperty("mob", config.getMobGravityOffset());
        gravity.addProperty("item", config.getItemGravityOffset());
        gravity.addProperty("arrow", config.getArrowGravityOffset());
        gravity.addProperty("thrown", config.getThrownGravityOffset());
        gravity.addProperty("block", config.getBlockGravityOffset());
        gravity.addProperty("muffle_gain", config.getFixedMuffleGain());
        gravity.addProperty("muffle_gain_hf", config.getFixedMuffleGainHF());
        obj.add("gravity", gravity);
        JsonObject atmosphere = new JsonObject();
        atmosphere.addProperty("start_altitude", config.getAtmosphereBaseAltitude());
        atmosphere.addProperty("gravity_offset", config.getAtmosphereGravityMax());
        atmosphere.addProperty("surface_gravity_offset", config.getSurfaceGravityOffset());
        atmosphere.addProperty("affect_non_living", config.isAtmosphereAffectNonLiving());
        atmosphere.addProperty("muffle_gain", config.getAtmosphereMuffleGain());
        atmosphere.addProperty("muffle_gain_hf", config.getAtmosphereMuffleGainHF());
        JsonArray layers = new JsonArray();
        config.getAtmosphereLayers().forEach(layers::add);
        atmosphere.add("layers", layers);
        obj.add("atmosphere", atmosphere);
        if (config.getNoFallDamageAltitude() == null) obj.add("no_fall_damage_above", null);
        else obj.addProperty("no_fall_damage_above", config.getNoFallDamageAltitude());
        obj.addProperty("fall_damage_mode", config.getFallDamageMode().name().toLowerCase(Locale.ROOT));
        return obj;
    }

    public static synchronized List<CustomDimConfig> getCustomDimensions() {
        if (!customDimensionsLoaded) loadCustomDimensions();
        return customDimensions.values().stream()
                .sorted(Comparator.comparing(CustomDimConfig::getDimensionId))
                .toList();
    }

    public static synchronized List<String> getCustomDimensionsJson() {
        return getCustomDimensions().stream().map(config -> GSON.toJson(toJson(config))).toList();
    }

    /** Pretty-printed JSON representation suitable for a datapack file under data/<ns>/endless_gravity/dimensions/. */
    public static String toPrettyDatapackJson(CustomDimConfig config) {
        return GSON_PRETTY.toJson(toJson(config));
    }

    public static CustomDimConfig defaultOverworldProfile() {
        return new CustomDimConfig("minecraft:overworld", CustomDimConfig.Mode.ATMOSPHERE,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.075, 64.0, 4500.0, 0.01, 0.005,
                AtmosphereLayers.serialize(AtmosphereLayers.defaults()), 0.0, false, CustomDimConfig.FallDamageMode.INHERIT);
    }

    public static CustomDimConfig defaultEndProfile() {
        return new CustomDimConfig("minecraft:the_end", CustomDimConfig.Mode.FIXED_GRAVITY,
                0.055, 0.025, 0.025, 0.03, 0.018, 0.035,
                0.075, 64.0, 4500.0, 0.01, 0.005,
                AtmosphereLayers.serialize(AtmosphereLayers.defaults()), 0.0, false, CustomDimConfig.FallDamageMode.INHERIT);
    }

    public static CustomDimConfig defaultGenericProfile(String id, CustomDimConfig.Mode mode) {
        if (mode == CustomDimConfig.Mode.ATMOSPHERE) {
            return new CustomDimConfig(id, CustomDimConfig.Mode.ATMOSPHERE,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    0.075, 64.0, 4500.0, 0.01, 0.005,
                    AtmosphereLayers.serialize(AtmosphereLayers.defaults()), 0.0, false, CustomDimConfig.FallDamageMode.INHERIT);
        }
        return new CustomDimConfig(id, CustomDimConfig.Mode.FIXED_GRAVITY,
                0.055, 0.025, 0.025, 0.03, 0.018, 0.035,
                0.075, 64.0, 4500.0, 0.01, 0.005,
                AtmosphereLayers.serialize(AtmosphereLayers.defaults()), 0.0, false, CustomDimConfig.FallDamageMode.INHERIT);
    }

    public static synchronized void replaceCustomDimensions(Collection<CustomDimConfig> dimensions) {
        replaceCustomDimensions(dimensions, true);
    }

    public static synchronized void replaceCustomDimensions(Collection<CustomDimConfig> dimensions, boolean persist) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        Map<String, CustomDimConfig> replacement = new LinkedHashMap<>();
        if (dimensions.size() > MAX_CUSTOM_DIMENSIONS) {
            throw new IllegalArgumentException("At most " + MAX_CUSTOM_DIMENSIONS + " dimension profiles are supported");
        }
        HashSet<String> seen = new HashSet<>();
        for (CustomDimConfig config : dimensions) {
            ResourceLocation id = ResourceLocation.tryParse(config.getDimensionId());
            if (id == null) throw new IllegalArgumentException("Invalid dimension ID: " + config.getDimensionId());
            if (!seen.add(id.toString())) throw new IllegalArgumentException("Duplicate dimension profile: " + id);
            List<String> layers = config.getAtmosphereLayers();
            if (layers.size() > CustomDimConfig.MAX_ATMOSPHERE_LAYERS) {
                throw new IllegalArgumentException("Too many atmosphere layers for " + id);
            }
            if (layers.size() < 2) {
                layers = defaultLayers();
            }
            Double noFall = config.getNoFallDamageAltitude();
            Double sanitizedNoFall = (noFall != null && Double.isFinite(noFall) && noFall >= 0.0)
                    ? Math.min(noFall, 30000.0) : null;
            replacement.put(id.toString(), new CustomDimConfig(id.toString(), config.getMode(),
                    clamp(config.getPlayerGravityOffset(), -0.5, 0.5), clamp(config.getMobGravityOffset(), -0.5, 0.5),
                    clamp(config.getItemGravityOffset(), -0.5, 0.5), clamp(config.getArrowGravityOffset(), -0.5, 0.5),
                    clamp(config.getThrownGravityOffset(), -0.5, 0.5), clamp(config.getBlockGravityOffset(), -0.5, 0.5),
                    clamp(config.getAtmosphereGravityMax(), 0.0, 0.5), clamp(config.getAtmosphereBaseAltitude(), -2048.0, 30000.0),
                    sanitizedNoFall, clamp(config.getAtmosphereMuffleGain(), 0.0, 1.0),
                    clamp(config.getAtmosphereMuffleGainHF(), 0.0, 1.0), layers,
                    clamp(config.getSurfaceGravityOffset(), -0.5, 0.5),
                    config.isAtmosphereAffectNonLiving(),
                    config.getFallDamageMode(),
                    clamp(config.getFixedMuffleGain(), 0.0, 1.0),
                    clamp(config.getFixedMuffleGainHF(), 0.0, 1.0)));
        }
        if (persist) {
            try {
                Path path = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("endless_gravity").resolve("dimensions.json");
                writeDimensionsFile(path, replacement.values());
            } catch (IOException e) {
                throw new UncheckedIOException("Could not save dimension profiles", e);
            }
            userCustomDimensions = Map.copyOf(replacement);
            rebuildMergedDimensions();
        } else {
            publishEffectiveDimensions(replacement);
        }
    }

    /** Updates or adds a single dimension profile and persists dimensions.json. */
    public static synchronized void putDimensionProfile(CustomDimConfig profile) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        Map<String, CustomDimConfig> updated = new LinkedHashMap<>(userCustomDimensions);
        updated.put(profile.getDimensionId(), profile);
        replaceCustomDimensions(updated.values(), true);
    }

    /** Removes a dimension profile from user config (restoring datapack default or vanilla) and persists. */
    public static synchronized boolean removeDimensionProfile(String dimensionId) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        if (!userCustomDimensions.containsKey(dimensionId)) return false;
        Map<String, CustomDimConfig> updated = new LinkedHashMap<>(userCustomDimensions);
        updated.remove(dimensionId);
        replaceCustomDimensions(updated.values(), true);
        return true;
    }

    /** Updates a single dimension profile if present and persists dimensions.json. */
    public static synchronized void updateDimensionProfile(
            String dimensionId, java.util.function.UnaryOperator<CustomDimConfig> updater) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        CustomDimConfig existing = customDimensions.get(dimensionId);
        if (existing == null) return;
        Map<String, CustomDimConfig> updated = new LinkedHashMap<>(userCustomDimensions);
        updated.put(dimensionId, updater.apply(existing));
        replaceCustomDimensions(updated.values(), true);
    }

    public static synchronized void applySyncedCustomDimensions(List<String> profiles) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        Map<String, CustomDimConfig> replacement = new LinkedHashMap<>();
        for (String profile : profiles) {
            if (replacement.size() >= MAX_CUSTOM_DIMENSIONS) break;
            try {
                CustomDimConfig config = parseDimension(JsonParser.parseString(profile));
                replacement.put(config.getDimensionId(), config);
            } catch (RuntimeException e) {
                LOGGER.warn("Ignoring invalid synced dimension profile: {}", e.getMessage());
            }
        }
        publishEffectiveDimensions(replacement);
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    public static synchronized void reloadCustomDimensions() {
        customDimensionsLoaded = false;
        loadCustomDimensions();
    }

    /** Persists runtime changes made via the config screen or commands to disk. */
    public static void persist() {
        COMMON_SPEC.save();
    }

    public static Optional<CustomDimConfig> getCustomDimConfig(String dimensionId) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        return Optional.ofNullable(customDimensions.get(dimensionId));
    }

    /** Zero-allocation lookup for hot entity tick paths. */
    public static Optional<CustomDimConfig> getCustomDimConfig(ResourceLocation dimensionId) {
        if (!customDimensionsLoaded) loadCustomDimensions();
        return Optional.ofNullable(customDimensionsByLocation.get(dimensionId));
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

    public static synchronized List<AtmospherePreset> getAtmospherePresets() {
        Path path = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get()
                .resolve("endless_gravity").resolve("atmosphere_presets.json");
        if (!Files.exists(path)) return List.of();
        try {
            List<AtmospherePreset> presets = AtmospherePreset.parseImport(Files.readString(path));
            validateUniquePresetNames(presets);
            return presets;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read atmosphere presets from " + path, e);
        } catch (RuntimeException e) {
            LOGGER.error("Invalid atmosphere presets in {}", path, e);
            throw new IllegalStateException("Could not load atmosphere presets from " + path, e);
        }
    }

    /** Safe variant for UI screens: logs and returns an empty list if presets JSON is corrupt. */
    public static synchronized List<AtmospherePreset> getAtmospherePresetsSafe() {
        try {
            return getAtmospherePresets();
        } catch (RuntimeException e) {
            LOGGER.warn("Falling back to empty atmosphere presets list: {}", e.getMessage());
            return List.of();
        }
    }

    public static synchronized void saveAtmospherePresets(List<AtmospherePreset> presets) {
        validateUniquePresetNames(presets);
        if (presets.size() > AtmospherePreset.MAX_PRESETS) {
            throw new IllegalArgumentException("At most " + AtmospherePreset.MAX_PRESETS + " presets are supported");
        }
        Path path = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get()
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
            throw new UncheckedIOException("Could not save atmosphere presets to " + path, e);
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
}