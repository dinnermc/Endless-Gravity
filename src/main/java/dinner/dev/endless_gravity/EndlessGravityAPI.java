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
 * Public API for Endless Gravity. Other mods can use this to check gravity state,
 * read atmosphere progress, and interact with the gravity immune tag.
 * <p>
 * The atmosphere is controlled by a single configurable pressure curve
 * (see {@link AtmosphereLayers}): pressure 1.0 at base = full atmosphere,
 * pressure 0.0 at deep space = vacuum. Every atmosphere system (gravity,
 * muffle, temperature, oxygen, Sable physics) derives from that curve.
 */
public final class EndlessGravityAPI {

    /**
     * Entity type tag. Add entity IDs to {@code data/endless_gravity/tags/entity/gravity_immune.json}
     * to make them ignore gravity in The End.
     */
    public static final TagKey<EntityType<?>> GRAVITY_IMMUNE =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(EndlessGravity.MODID, "gravity_immune"));

    /** Sea-level reference altitude. Atmosphere effects are gated to start above this. */
    public static final double BASE = 64.0;

    private EndlessGravityAPI() {}

    /**
     * Returns {@code true} if the entity's type is in the {@code endless_gravity:gravity_immune} tag.
     */
    public static boolean isGravityImmune(Entity entity) {
        return entity.getType().is(GRAVITY_IMMUNE);
    }

    /**
     * Returns the atmosphere pressure at the given Y level, from the configured atmosphere layers.
     * 1.0 at base (full atmosphere), 0.0 at vacuum.
     */
    public static double getPressure(double y) {
        return AtmosphereLayers.getPressure(y);
    }

    /**
     * Returns the atmosphere progress for a given Y level, from 0.0 at full atmosphere pressure
     * to 1.0 at vacuum, derived from the configured atmospheric layers (progress = 1 - pressure).
     */
    public static double getAtmosphereProgress(double y) {
        return AtmosphereLayers.getProgress(y);
    }

    /**
     * Returns the atmosphere gravity offset at the given Y level.
     * 0.0 at full pressure, max at vacuum, following the configured atmosphere layers.
     */
    public static double getAtmosphereOffset(double y) {
        if (!Config.COMMON.enableAtmosphere.get()) return 0.0;
        double progress = getAtmosphereProgress(y);
        return progress * Config.COMMON.atmosphereGravityMax.get();
    }

    /**
     * Returns the atmosphere muffle gain at the given Y level.
     * 1.0 at full pressure, Config value at vacuum.
     */
    public static double getAtmosphereMuffleGain(double y) {
        double progress = getAtmosphereProgress(y);
        return 1.0 - progress * (1.0 - Config.COMMON.atmosphereMuffleGain.get());
    }

    /**
     * Returns the atmosphere muffle gain HF at the given Y level.
     * 1.0 at full pressure, Config value at vacuum.
     */
    public static double getAtmosphereMuffleGainHF(double y) {
        double progress = getAtmosphereProgress(y);
        return 1.0 - progress * (1.0 - Config.COMMON.atmosphereMuffleGainHF.get());
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

    /**
     * Projects an arbitrary position out of a Sable sub-level into global (Overworld) space.
     * Returns the position's global Y coordinate, or {@code pos.y} unchanged when not in a sub-level.
     */
    public static double getRealY(Level level, Vec3 pos) {
        ResourceKey<Level> dim = level.dimension();

        if (dim == Level.OVERWORLD || dim == Level.END || dim == Level.NETHER) {
            return pos.y;
        }

        if (!dim.location().getNamespace().equals("sable")) {
            return pos.y;
        }

        try {
            return SableCompanion.INSTANCE.projectOutOfSubLevel(level, pos).y;
        } catch (Exception e) {
            return pos.y;
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

    private static Boolean sableLoaded;

    /**
     * Returns {@code true} if the Sable mod is installed.
     */
    public static boolean isSableLoaded() {
        if (sableLoaded == null) {
            try {
                sableLoaded = net.neoforged.fml.ModList.get().isLoaded("sable");
            } catch (Exception e) {
                sableLoaded = false;
            }
        }
        return sableLoaded;
    }

    /**
     * Returns {@code true} if Sable manages physics for this dimension.
     * When this returns true, Endless Gravity's gravity and drag handlers
     * should defer to Sable's physics engine.
     */
    public static boolean isSableManaged(Level level) {
        if (!isSableLoaded()) return false;
        ResourceKey<Level> dim = level.dimension();
        // Sable-owned dimensions (sable:overworld, sable:the_end, sable:nether)
        if (dim.location().getNamespace().equals("sable")) return true;
        // The End: we generate a Sable datapack for it, so Sable controls it
        return dim == Level.END;
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
     * Temperature progress for freezing effects, derived from the configured atmosphere layers.
     * 0.0 at full atmosphere pressure (comfortable), ramps to 1.0 at vacuum (max freeze).
     */
    public static double getTemperatureProgress(double y) {
        return AtmosphereLayers.getProgress(y);
    }

    /**
     * Oxygen depletion progress, derived from the configured atmosphere layers.
     * 0.0 at full atmosphere pressure (normal air), ramps to 1.0 at vacuum.
     */
    public static double getOxygenProgress(double y) {
        return AtmosphereLayers.getProgress(y);
    }
}
