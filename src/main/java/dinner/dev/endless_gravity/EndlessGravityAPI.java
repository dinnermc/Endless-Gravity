package dinner.dev.endless_gravity;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Public API for Endless Gravity. Other mods can use this to read config values,
 * check gravity state, and interact with the gravity immune tag.
 */
public final class EndlessGravityAPI {

    /**
     * Entity type tag. Add entity IDs to {@code data/endless_gravity/tags/entity/gravity_immune.json}
     * to make them ignore gravity in The End.
     */
    public static final TagKey<EntityType<?>> GRAVITY_IMMUNE =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "gravity_immune"));

    // Real atmospheric layer boundaries
    public static final double BASE = 64.0;
    public static final double TROPOPAUSE = 400.0;
    public static final double STRATOPAUSE = 900.0;
    public static final double MESOPAUSE = 1200.0;
    public static final double KARMAN_LINE = 1800.0;
    public static final double SPACE = 2500.0;
    public static final double SPACE_DEEP = 3500.0;

    private static final double[] LAYER_TOPS = {BASE, TROPOPAUSE, STRATOPAUSE, MESOPAUSE, KARMAN_LINE, SPACE, SPACE_DEEP};

    private EndlessGravityAPI() {}

    /**
     * Returns {@code true} if the entity's type is in the {@code endless_gravity:gravity_immune} tag.
     */
    public static boolean isGravityImmune(Entity entity) {
        return entity.getType().is(GRAVITY_IMMUNE);
    }

    /**
     * Returns {@code true} if the given level is The End (where gravity effects are active).
     */
    public static boolean isGravityEnabled(Level level) {
        return level.dimension() == Level.END;
    }

    /**
     * Returns the current player gravity offset from config.
     */
    public static double getPlayerGravityOffset() {
        return Config.COMMON.playerGravityOffset.get();
    }

    /**
     * Returns the current item gravity offset from config.
     */
    public static double getItemGravityOffset() {
        return Config.COMMON.itemGravityOffset.get();
    }

    /**
     * Returns the current arrow gravity offset from config.
     */
    public static double getArrowGravityOffset() {
        return Config.COMMON.arrowGravityOffset.get();
    }

    /**
     * Returns the current thrown projectile gravity offset from config.
     */
    public static double getThrownGravityOffset() {
        return Config.COMMON.thrownGravityOffset.get();
    }

    /**
     * Returns the current falling block gravity offset from config.
     */
    public static double getBlockGravityOffset() {
        return Config.COMMON.blockGravityOffset.get();
    }

    /**
     * Returns {@code true} if player gravity is enabled in config.
     */
    public static boolean isPlayerGravityEnabled() {
        return Config.COMMON.enablePlayerGravity.get();
    }

    /**
     * Returns {@code true} if item gravity is enabled in config.
     */
    public static boolean isItemGravityEnabled() {
        return Config.COMMON.enableItemGravity.get();
    }

    /**
     * Returns {@code true} if arrow gravity is enabled in config.
     */
    public static boolean isArrowGravityEnabled() {
        return Config.COMMON.enableArrowGravity.get();
    }

    /**
     * Returns {@code true} if thrown projectile gravity is enabled in config.
     */
    public static boolean isThrownGravityEnabled() {
        return Config.COMMON.enableThrownGravity.get();
    }

    /**
     * Returns {@code true} if falling block gravity is enabled in config.
     */
    public static boolean isBlockGravityEnabled() {
        return Config.COMMON.enableBlockGravity.get();
    }

    /**
     * Returns the fall damage mode: 0 = normal, 1 = disabled, 2 = velocity-based.
     */
    public static int getFallDamageMode() {
        return Config.COMMON.fallDamageMode.get();
    }

    /**
     * Returns the atmosphere progress for a given Y level, from 0.0 at BASE (Y=64) to 1.0 at SPACE_DEEP (Y=3500).
     * Uses piecewise linear interpolation across real atmospheric layers:
     * BASE(64) -> TROPOPAUSE(400) -> STRATOPAUSE(900) -> MESOPAUSE(1200) -> KARMAN_LINE(1800) -> SPACE(2500) -> SPACE_DEEP(3500).
     */
    public static double getAtmosphereProgress(double y) {
        if (y <= BASE) return 0.0;
        if (y >= SPACE_DEEP) return 1.0;
        for (int i = 0; i < LAYER_TOPS.length - 1; i++) {
            if (y <= LAYER_TOPS[i + 1]) {
                double layerStart = LAYER_TOPS[i];
                double layerEnd = LAYER_TOPS[i + 1];
                double layerFraction = (y - layerStart) / (layerEnd - layerStart);
                double layerIndex = (double) i / (LAYER_TOPS.length - 1);
                double nextLayerIndex = (double) (i + 1) / (LAYER_TOPS.length - 1);
                return layerIndex + layerFraction * (nextLayerIndex - layerIndex);
            }
        }
        return 1.0;
    }

    /**
     * Returns the atmosphere gravity offset at the given Y level.
     * 0.0 at BASE, max at SPACE_DEEP, using piecewise linear interpolation.
     */
    public static double getAtmosphereOffset(double y) {
        if (!Config.COMMON.enableAtmosphere.get()) return 0.0;
        double progress = getAtmosphereProgress(y);
        return progress * Config.COMMON.atmosphereGravityMax.get();
    }

    /**
     * Returns the atmosphere muffle gain at the given Y level.
     * 1.0 at BASE, Config value at SPACE_DEEP.
     */
    public static double getAtmosphereMuffleGain(double y) {
        double progress = getAtmosphereProgress(y);
        return 1.0 - progress * (1.0 - Config.COMMON.atmosphereMuffleGain.get());
    }

    /**
     * Returns the atmosphere muffle gain HF at the given Y level.
     * 1.0 at BASE, Config value at SPACE_DEEP.
     */
    public static double getAtmosphereMuffleGainHF(double y) {
        double progress = getAtmosphereProgress(y);
        return 1.0 - progress * (1.0 - Config.COMMON.atmosphereMuffleGainHF.get());
    }

    private static final double FULL_INERTIA_FACTOR = 1.0 / 0.91;

    /**
     * Drag progress: 0.0 at BASE, 1.0 at KARMAN_LINE (full inertia in space).
     * Above KARMAN_LINE stays at 1.0 — zero drag in vacuum.
     */
    public static double getDragProgress(double y) {
        if (y <= BASE) return 0.0;
        if (y >= KARMAN_LINE) return 1.0;
        return (y - BASE) / (KARMAN_LINE - BASE);
    }

    /**
     * Returns the drag compensation factor for the given Y level.
     * 1.0 at BASE (vanilla drag), scaling to FULL_INERTIA_FACTOR at KARMAN_LINE.
     * In space (above KARMAN_LINE), drag is fully canceled — true vacuum physics.
     */
    public static double getAtmosphereDrag(double y) {
        double dragProgress = getDragProgress(y);
        return 1.0 + dragProgress * (FULL_INERTIA_FACTOR - 1.0) * Config.COMMON.atmosphereDrag.get();
    }

    /**
     * Projects an entity's position out of a Sable sub-level into global (Overworld) space.
     * Returns the entity's actual Overworld Y coordinate, accounting for sub-level offsets.
     * If not in a sub-level, returns {@code entity.getY()} unchanged.
     */
    public static double getRealY(Entity entity) {
        Level level = entity.level();
        ResourceKey<Level> dim = level.dimension();

        // Fast path: if not a sub-level, entity Y is already global
        if (dim == Level.OVERWORLD || dim == Level.END || dim == Level.NETHER) {
            return entity.getY();
        }

        // Only attempt projection for Sable sub-level dimensions
        if (!dim.location().getNamespace().equals("sable")) {
            return entity.getY();
        }

        try {
            Vec3 realPos = SableCompanion.INSTANCE.projectOutOfSubLevel(level, entity.position());
            return realPos.y;
        } catch (Exception e) {
            return getSubLevelOriginY(entity);
        }
    }

    private static double getSubLevelOriginY(Entity entity) {
        try {
            SubLevelAccess sub = SableCompanion.INSTANCE.getContaining(entity);
            if (sub != null) {
                return sub.logicalPose().position().y() + entity.getY();
            }
        } catch (Exception ignored) {}
        return entity.getY();
    }

    /**
     * Returns {@code true} if the given level is the Overworld or a Sable sub-level.
     */
    public static boolean isOverworldOrSable(Level level) {
        ResourceKey<Level> dim = level.dimension();
        if (dim == Level.OVERWORLD) return true;
        return dim.location().getNamespace().equals("sable");
    }

    /**
     * Temperature progress for freezing effects.
     * 0.0 at BASE (comfortable), ramps to 1.0 at STRATOPAUSE (max freeze).
     * Above STRATOPAUSE stays at 1.0.
     */
    public static double getTemperatureProgress(double y) {
        if (y <= BASE) return 0.0;
        if (y >= STRATOPAUSE) return 1.0;
        return (y - BASE) / (STRATOPAUSE - BASE);
    }

    /**
     * Oxygen depletion progress.
     * 0.0 at BASE (normal air), ramps to 1.0 at KARMAN_LINE (vacuum).
     * Above KARMAN_LINE stays at 1.0.
     */
    public static double getOxygenProgress(double y) {
        if (y <= BASE) return 0.0;
        if (y >= KARMAN_LINE) return 1.0;
        return (y - BASE) / (KARMAN_LINE - BASE);
    }

    /**
     * @deprecated Use {@link #getAtmosphereProgress(double)} instead.
     */
    @Deprecated
    public static double getOverworldLayerProgress(Level level, double y) {
        return getAtmosphereProgress(y);
    }
}
