package dinner.dev.endless_gravity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

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
}
