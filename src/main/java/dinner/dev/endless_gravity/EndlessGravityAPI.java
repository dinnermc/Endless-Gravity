package dinner.dev.endless_gravity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Public API for Endless Gravity: atmosphere queries, dimension helpers,
 * and the gravity-immune tag. The pressure curve lives in {@link AtmosphereLayers}.
 */
public final class EndlessGravityAPI {

    /**
     * Entity type tag. Add entity IDs to {@code data/endless_gravity/tags/entity_type/gravity_immune.json}
     * to make them ignore reduced gravity.
     */
    public static final TagKey<EntityType<?>> GRAVITY_IMMUNE =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "gravity_immune"));

    /** Sea-level. Atmosphere effects start above this. */
    public static final double BASE = 64.0;

    private EndlessGravityAPI() {}

    public static boolean isGravityImmune(Entity entity) {
        if (entity.getType().is(GRAVITY_IMMUNE)) return true;
        if (!(entity instanceof LivingEntity)) return false;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && Config.COMMON.gravityImmuneEntities.get().contains(id.toString());
    }

    public static double getPressure(double y) {
        Config.CustomDimConfig config = Config.getCustomDimConfig(Level.OVERWORLD.location()).orElse(null);
        if (config != null && config.isEnableAtmosphere()) {
            return config.getAtmospherePressure(y);
        }
        return AtmosphereLayers.getPressure(y);
    }

    public static double getPressure(Level level, double y) {
        if (level == null) return getPressure(y);
        Config.CustomDimConfig config = Config.getCustomDimConfig(level.dimension().location()).orElse(null);
        if (config != null) {
            return config.isEnableAtmosphere() ? config.getAtmospherePressure(y) : 1.0;
        }
        return 1.0;
    }

    public static double getAtmosphereProgress(double y) {
        Config.CustomDimConfig config = Config.getCustomDimConfig(Level.OVERWORLD.location()).orElse(null);
        if (config == null) return AtmosphereLayers.getProgress(y);
        if (!config.isEnableAtmosphere()) return 0.0;
        return config.getAtmosphereProgress(y);
    }

    public static double getAtmosphereProgress(Level level, double y) {
        if (level == null) return getAtmosphereProgress(y);
        Config.CustomDimConfig config = Config.getCustomDimConfig(level.dimension().location()).orElse(null);
        if (config == null || !config.isEnableAtmosphere()) return 0.0;
        return config.getAtmosphereProgress(y);
    }

    public static double getAtmosphereOffset(double y) {
        Config.CustomDimConfig config = Config.getCustomDimConfig(Level.OVERWORLD.location()).orElse(null);
        if (config == null) {
            if (!Config.COMMON.enableAtmosphere.get()) return 0.0;
            return AtmosphereLayers.getGravityProgress(y) * Config.COMMON.atmosphereGravityMax.get();
        }
        if (!config.isEnableAtmosphere() || !Config.COMMON.enableAtmosphere.get()
                || !Config.COMMON.overworldEntityGravity.get()) return 0.0;
        return config.getAtmosphereGravityOffset(y);
    }

    public static double getAtmosphereOffset(Level level, double y) {
        if (level == null) return getAtmosphereOffset(y);
        Config.CustomDimConfig config = Config.getCustomDimConfig(level.dimension().location()).orElse(null);
        if (config == null || !config.isEnableAtmosphere()) return 0.0;
        if (level.dimension() == Level.OVERWORLD
                && (!Config.COMMON.enableAtmosphere.get() || !Config.COMMON.overworldEntityGravity.get())) {
            return 0.0;
        }
        return config.getAtmosphereGravityOffset(y);
    }

    public static double getAtmosphereMuffleGain(double y) {
        Config.CustomDimConfig config = Config.getCustomDimConfig(Level.OVERWORLD.location()).orElse(null);
        if (config != null) {
            return config.isEnableAtmosphere() ? config.getAtmosphereMuffleGain(y) : 1.0;
        }
        return MuffleMath.perceptualGain(AtmosphereLayers.getAudioMuffleProgress(y), Config.COMMON.atmosphereMuffleGain.get());
    }

    public static double getAtmosphereMuffleGain(Level level, double y) {
        if (level == null) return getAtmosphereMuffleGain(y);
        Config.CustomDimConfig config = Config.getCustomDimConfig(level.dimension().location()).orElse(null);
        if (config == null || !config.isEnableAtmosphere()) return 1.0;
        return config.getAtmosphereMuffleGain(y);
    }

    public static double getAtmosphereMuffleGainHF(double y) {
        Config.CustomDimConfig config = Config.getCustomDimConfig(Level.OVERWORLD.location()).orElse(null);
        if (config != null) {
            return config.isEnableAtmosphere() ? config.getAtmosphereMuffleGainHF(y) : 1.0;
        }
        return MuffleMath.perceptualGainHF(AtmosphereLayers.getAudioMuffleProgress(y), Config.COMMON.atmosphereMuffleGainHF.get());
    }

    public static double getAtmosphereMuffleGainHF(Level level, double y) {
        if (level == null) return getAtmosphereMuffleGainHF(y);
        Config.CustomDimConfig config = Config.getCustomDimConfig(level.dimension().location()).orElse(null);
        if (config == null || !config.isEnableAtmosphere()) return 1.0;
        return config.getAtmosphereMuffleGainHF(y);
    }

    public static double getRealY(Entity entity) {
        return entity.getY();
    }

    public static boolean isEnd(Level level) {
        return level.dimension() == Level.END;
    }

    public static boolean isOverworld(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }

    public static boolean hasCustomGravity(ResourceKey<Level> dimension) {
        return Config.hasCustomGravity(dimension.location().toString());
    }

    public static boolean hasCustomAtmosphere(ResourceKey<Level> dimension) {
        return Config.hasCustomAtmosphere(dimension.location().toString());
    }

    public static boolean hasCustomGravity(String dimensionId) {
        return Config.hasCustomGravity(dimensionId);
    }

    public static Optional<Config.CustomDimConfig> getCustomDimConfig(ResourceKey<Level> dimension) {
        return Config.getCustomDimConfig(dimension.location());
    }

    public static Optional<Config.CustomDimConfig> getCustomDimConfig(String dimensionId) {
        return Config.getCustomDimConfig(dimensionId);
    }

    public static boolean hasCustomGravityEnabled(ResourceKey<Level> dimension) {
        return Config.hasCustomGravity(dimension.location().toString());
    }

    public static boolean hasCustomAtmosphereEnabled(ResourceKey<Level> dimension) {
        return Config.hasCustomAtmosphere(dimension.location().toString());
    }

    public static double getMobGravityOffsetForDimension(ResourceKey<Level> dimension) {
        return Config.getMobGravityOffsetForDimension(dimension.location().toString());
    }

    public static double getMobGravityOffsetCustom(ResourceKey<Level> dimension) {
        return Config.getMobGravityOffsetCustom(dimension.location().toString());
    }

    public static boolean hasMobGravityEnabled(ResourceKey<Level> dimension) {
        return Config.hasMobGravity(dimension.location().toString());
    }

    public static Optional<Config.CustomDimConfig> getCustomDimConfigByKey(ResourceKey<Level> dimension) {
        return Config.getCustomDimConfig(dimension.location());
    }
}
