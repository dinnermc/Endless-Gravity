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
 * Public API for Endless Gravity: atmosphere queries, dimension/Sable helpers,
 * and the gravity-immune tag. The pressure curve lives in {@link AtmosphereLayers}.
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
     * {@code true} if the entity type is in {@code endless_gravity:gravity_immune}.
     */
    public static boolean isGravityImmune(Entity entity) {
        return entity.getType().is(GRAVITY_IMMUNE);
    }

    /**
     * Atmosphere pressure at Y, from the configured layers: 1.0 at base, 0.0 at vacuum.
     */
    public static double getPressure(double y) {
        return AtmosphereLayers.getPressure(y);
    }

    /**
     * 0.0 at full atmosphere, 1.0 at vacuum ({@code 1 - pressure}).
     */
    public static double getAtmosphereProgress(double y) {
        return AtmosphereLayers.getProgress(y);
    }

    /**
     * Gravity offset at Y: 0.0 at full pressure, ramping to max at vacuum. 0.0 when the atmosphere is disabled.
     */
    public static double getAtmosphereOffset(double y) {
        if (!Config.COMMON.enableAtmosphere.get()) return 0.0;
        double progress = getAtmosphereProgress(y);
        return progress * Config.COMMON.atmosphereGravityMax.get();
    }

    /**
     * Muffle gain at Y: 1.0 at full pressure, the configured value at vacuum.
     */
    public static double getAtmosphereMuffleGain(double y) {
        double progress = getAtmosphereProgress(y);
        return 1.0 - progress * (1.0 - Config.COMMON.atmosphereMuffleGain.get());
    }

    /**
     * Muffle gain HF at Y: 1.0 at full pressure, the configured value at vacuum.
     */
    public static double getAtmosphereMuffleGainHF(double y) {
        double progress = getAtmosphereProgress(y);
        return 1.0 - progress * (1.0 - Config.COMMON.atmosphereMuffleGainHF.get());
    }

    /**
     * Projects an entity out of a Sable sub-level into global (Overworld) Y.
     * Unchanged {@code entity.getY()} when not in a sub-level.
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
     * Same as {@link #getRealY(Entity)} but for an arbitrary position.
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
     * {@code true} if the Sable mod is installed.
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

    private static Boolean cosmonauticsLoaded;

    /**
     * {@code true} if Create: Cosmonautics ({@code rocketnautics}) is installed.
     * When it is, the Overworld atmosphere gravity system backs off so Cosmonautics
     * owns gravity for the Overworld, its entities and sub-levels.
     */
    public static boolean isCosmonauticsInstalled() {
        if (cosmonauticsLoaded == null) {
            try {
                cosmonauticsLoaded = net.neoforged.fml.ModList.get().isLoaded("rocketnautics");
            } catch (Exception e) {
                cosmonauticsLoaded = false;
            }
        }
        return cosmonauticsLoaded;
    }

    /**
     * {@code true} if Sable manages physics for this dimension. When true, the
     * gravity/drag handlers defer to Sable's physics engine.
     */
    public static boolean isSableManaged(Level level) {
        if (!isSableLoaded()) return false;
        ResourceKey<Level> dim = level.dimension();
        // Only Sable-owned dimensions (sable:overworld, sable:the_end, sable:nether).
        // The vanilla End stays managed by Endless Gravity itself: Sable's
        // dimension_physics base_gravity does not apply float behavior there.
        return dim.location().getNamespace().equals("sable");
    }

    /**
     * {@code true} if the given level is The End.
     */
    public static boolean isEnd(Level level) {
        return level.dimension() == Level.END;
    }

    /**
     * {@code true} if the level is the Overworld or a Sable sub-level.
     */
    public static boolean isOverworldOrSable(Level level) {
        ResourceKey<Level> dim = level.dimension();
        if (dim == Level.OVERWORLD) return true;
        return dim.location().getNamespace().equals("sable");
    }
}
