package dinner.dev.endless_gravity;

import dev.ryanhcode.sable.companion.SableCompanion;
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
     * Returns the Overworld layer progress at the given Y level, from 0.0 (below start Y or disabled) to 1.0 (max layers reached).
     * Also applies to Sable sub-levels in the Overworld.
     */
    public static double getOverworldLayerProgress(Level level, double y) {
        if (!isOverworldOrSable(level)) return 0.0;
        if (!Config.COMMON.enableOverworldGravity.get()) return 0.0;

        int startY = Config.COMMON.overworldGravityStartY.get();
        if (y < startY) return 0.0;

        int layerHeight = Config.COMMON.overworldGravityLayerHeight.get();
        int maxLayers = Config.COMMON.overworldGravityMaxLayers.get();
        double layers = Math.min(maxLayers, (y - startY) / layerHeight);
        return layers / maxLayers;
    }

    public static boolean isOverworldOrSable(Level level) {
        ResourceKey<Level> dim = level.dimension();
        if (dim == Level.OVERWORLD) return true;
        return dim.location().getNamespace().equals("sable");
    }

    /**
     * Projects an entity's position out of a Sable sub-level into global (Overworld) space.
     * Returns the entity's actual Overworld Y coordinate, accounting for sub-level offsets.
     * If not in a sub-level, returns {@code entity.getY()} unchanged.
     */
    public static double getRealY(Entity entity) {
        try {
            Vec3 realPos = SableCompanion.INSTANCE.projectOutOfSubLevel(entity.level(), entity.position());
            return realPos.y;
        } catch (Exception e) {
            return entity.getY();
        }
    }
}
