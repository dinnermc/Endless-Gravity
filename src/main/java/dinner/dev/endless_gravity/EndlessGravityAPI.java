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

    /** Sea-level. Atmosphere effects start above this. */
    public static final double BASE = 64.0;

    private EndlessGravityAPI() {}

    public static boolean isGravityImmune(Entity entity) {
        return entity.getType().is(GRAVITY_IMMUNE);
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
     * Global Y for an entity. Inside a Sable sub-level the entity's own Y is
     * local to that level; this projects it back to Overworld coordinates.
     */
    public static double getRealY(Entity entity) {
        Level level = entity.level();
        ResourceKey<Level> dim = level.dimension();

        if (dim == Level.OVERWORLD || dim == Level.END || dim == Level.NETHER) {
            return entity.getY();
        }

        if (!dim.location().getNamespace().equals("sable")) {
            return entity.getY();
        }

        try {
            Vec3 realPos = SableCompanion.INSTANCE.projectOutOfSubLevel(level, entity.position());
            return realPos.y;
        } catch (Exception e) {
            // entity not fully inside the sub-level chunk grid yet; use the sub-level origin
            return getSubLevelOriginY(entity);
        }
    }

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

    // fallback when the entity is not inside the sub-level chunk grid yet
    private static double getSubLevelOriginY(Entity entity) {
        try {
            SubLevelAccess sub = SableCompanion.INSTANCE.getContaining(entity);
            if (sub == null) return entity.getY();
            return sub.logicalPose().position().y() + entity.getY();
        } catch (Exception e) {
            return entity.getY();
        }
    }

    private static Boolean sableLoaded;

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

    public static boolean isSableManaged(Level level) {
        if (!isSableLoaded()) return false;
        ResourceKey<Level> dim = level.dimension();
        // Only Sable-owned dimensions (sable:overworld, sable:the_end, sable:nether).
        // The vanilla End stays with Endless Gravity: Sable's base_gravity does not float there.
        return dim.location().getNamespace().equals("sable");
    }

    public static boolean isEnd(Level level) {
        return level.dimension() == Level.END;
    }

    public static boolean isOverworldOrSable(Level level) {
        ResourceKey<Level> dim = level.dimension();
        if (dim == Level.OVERWORLD) return true;
        return dim.location().getNamespace().equals("sable");
    }
}
