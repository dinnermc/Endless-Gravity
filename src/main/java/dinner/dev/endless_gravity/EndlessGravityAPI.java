package dinner.dev.endless_gravity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;

/**
 * Public API for Endless Gravity: atmosphere queries, dimension helpers,
 * and the gravity-immune tag. The pressure curve lives in {@link AtmosphereLayers}.
 */
public final class EndlessGravityAPI {

    /**
     * Entity type tag. Add entity IDs to {@code data/endless_gravity/tags/entity/gravity_immune.json}
     * to make them ignore gravity in The End.
     */
    public static final TagKey<EntityType<?>> GRAVITY_IMMUNE =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "gravity_immune"));

    /** Sea-level. Atmosphere effects start above this. */
    public static final double BASE = 64.0;

    private EndlessGravityAPI() {}

    public static boolean isGravityImmune(Entity entity) {
        if (entity.getType().is(GRAVITY_IMMUNE)) return true;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && Config.COMMON.gravityImmuneEntities.get().contains(id.toString());
    }

    public static double getPressure(double y) {
        return AtmosphereLayers.getPressure(y);
    }

    public static double getAtmosphereProgress(double y) {
        return AtmosphereLayers.getProgress(y);
    }

    public static double getAtmosphereOffset(double y) {
        if (!Config.COMMON.enableAtmosphere.get()) return 0.0;
        double progress = getAtmosphereProgress(y);
        return progress * Config.COMMON.atmosphereGravityMax.get();
    }

    public static double getAtmosphereMuffleGain(double y) {
        double progress = getAtmosphereProgress(y);
        return 1.0 - progress * (1.0 - Config.COMMON.atmosphereMuffleGain.get());
    }

    public static double getAtmosphereMuffleGainHF(double y) {
        double progress = getAtmosphereProgress(y);
        return 1.0 - progress * (1.0 - Config.COMMON.atmosphereMuffleGainHF.get());
    }

    /**
     * Global Y for an entity.
     */
    public static double getRealY(Entity entity) {
        return entity.getY();
    }

    public static double getRealY(Level level, double y) {
        return y;
    }

    public static boolean isEnd(Level level) {
        return level.dimension() == Level.END;
    }

    public static boolean isOverworld(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }

    // ========== Custom Dimension Support ==========

    /**
     * Checks if a dimension has custom gravity configuration.
     */
    public static boolean hasCustomGravity(ResourceKey<Level> dimension) {
        return Config.hasCustomGravity(dimension.location().toString());
    }

    /**
     * Checks if a dimension has custom atmosphere configuration.
     */
    public static boolean hasCustomAtmosphere(ResourceKey<Level> dimension) {
        return Config.hasCustomAtmosphere(dimension.location().toString());
    }

    /**
     * Checks if a dimension has custom gravity configuration (by ID string).
     */
    public static boolean hasCustomGravity(String dimensionId) {
        return Config.hasCustomGravity(dimensionId);
    }

    /**
     * Gets the custom dimension configuration for a dimension.
     */
    public static Optional<Config.CustomDimConfig> getCustomDimConfig(ResourceKey<Level> dimension) {
        return Config.getCustomDimConfig(dimension.location().toString());
    }

    /**
     * Gets the custom dimension configuration for a dimension by ID string.
     */
    public static Optional<Config.CustomDimConfig> getCustomDimConfig(String dimensionId) {
        return Config.getCustomDimConfig(dimensionId);
    }

    /**
     * Checks if a dimension has custom gravity enabled.
     */
    public static boolean hasCustomGravityEnabled(ResourceKey<Level> dimension) {
        return Config.hasCustomGravity(dimension.location().toString());
    }

    /**
     * Checks if a dimension has custom atmosphere enabled.
     */
    public static boolean hasCustomAtmosphereEnabled(ResourceKey<Level> dimension) {
        return Config.hasCustomAtmosphere(dimension.location().toString());
    }

    /**
     * Gets the mob gravity offset for a custom dimension.
     */
    public static double getMobGravityOffsetForDimension(ResourceKey<Level> dimension) {
        return Config.getMobGravityOffsetForDimension(dimension.location().toString());
    }

    /**
     * Gets the custom mob gravity offset for a dimension.
     */
    public static double getMobGravityOffsetCustom(ResourceKey<Level> dimension) {
        return Config.getMobGravityOffsetCustom(dimension.location().toString());
    }

    /**
     * Checks if a dimension has mob gravity enabled.
     */
    public static boolean hasMobGravityEnabled(ResourceKey<Level> dimension) {
        return Config.hasMobGravity(dimension.location().toString());
    }

    /**
     * Gets the custom dimension configuration for a dimension by ResourceKey.
     */
    public static Optional<Config.CustomDimConfig> getCustomDimConfigByKey(ResourceKey<Level> dimension) {
        return Config.getCustomDimConfig(dimension.location().toString());
    }
}