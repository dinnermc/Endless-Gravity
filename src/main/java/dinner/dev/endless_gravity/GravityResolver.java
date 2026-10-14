package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.api.GravityChannel;
import dinner.dev.endless_gravity.api.GravityContext;
import dinner.dev.endless_gravity.api.GravityModifierRegistry;
import dinner.dev.endless_gravity.api.GravitySource;
import dinner.dev.endless_gravity.event.GravityCalculationCallback;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;

/**
 * What offset does Endless Gravity apply to this entity?
 *
 * <p>Every application point (server player tick, client player tick and the entity
 * mixins) goes through {@link #compute}, so {@link dinner.dev.endless_gravity.api.GravityApi#getGravityOffset}
 * and the mod's own behaviour can never drift apart. {@link #compute} only calculates;
 * callers apply the offset with {@link net.minecraft.world.entity.Entity#setDeltaMovement}.
 *
 * <p>Fabric differences from the NeoForge resolver: no Sable (no sub-level projection or
 * Sable-owned dimensions) and custom-dimension gravity applies to mobs only, never to players.
 */
public final class GravityResolver {

    private GravityResolver() {}

    /** The channel an entity belongs to, or {@code null} if it has none. */
    public static GravityChannel channelFor(Entity entity) {
        if (entity instanceof Player) return GravityChannel.PLAYER;
        if (entity instanceof LivingEntity) return GravityChannel.MOB;
        if (entity instanceof ItemEntity) return GravityChannel.ITEM;
        if (entity instanceof AbstractArrow) return GravityChannel.ARROW;
        if (entity instanceof ThrowableProjectile) return GravityChannel.THROWN;
        if (entity instanceof FallingBlockEntity) return GravityChannel.BLOCK;
        return null;
    }

    /** Preconditions shared by all application points. */
    public static boolean canApply(Entity entity, GravityChannel channel) {
        if (entity.onGround() || entity.isInWater()) return false;
        if (EndlessGravityAPI.isGravityImmune(entity)) return false;

        if (channel == GravityChannel.PLAYER) {
            if (entity instanceof LivingEntity living && living.isFallFlying()) return false;
            if (entity instanceof Player player && player.getAbilities().flying) return false;
        } else if (channel == GravityChannel.MOB) {
            if (entity instanceof LivingEntity living) {
                if (living.isFallFlying()) return false;
                if (GravityHandler.isFlightOrImmobile(living)) return false;
            }
        }
        return true;
    }

    /** Resolve the base offset and run all modifiers. Never applies anything. */
    public static GravityContext compute(Entity entity, GravityChannel channel) {
        Level level = entity.level();
        GravityContext context = new GravityContext(entity, level, channel);

        if (!GravityGameRules.enabled(level)) return context;

        if (!resolveCustomDimension(level, channel, context) && !resolveAtmosphere(entity, level, context)) {
            resolveEnd(level, channel, context);
        }

        applyEntityGravity(entity, context);

        GravityCalculationCallback.EVENT.invoker().calculate(context);
        GravityModifierRegistry.apply(context);
        return context;
    }

    /** Status effects on living entities that change gravity. */
    private static void applyEntityGravity(Entity entity, GravityContext context) {
        if (!(entity instanceof LivingEntity living)) return;

        if (Config.COMMON.enablePotionGravity.get()) {
            for (MobEffectInstance instance : living.getActiveEffects()) {
                MobEffect effect = instance.getEffect().value();
                int level = instance.getAmplifier() + 1;
                if (effect == ModEffects.weightlessness()) {
                    context.addOffset(Config.COMMON.weightlessnessGravityOffset.get() * level);
                } else if (effect == ModEffects.heaviness()) {
                    context.addOffset(-Config.COMMON.heavinessGravityOffset.get() * level);
                }
            }
        }
    }

    private static boolean resolveCustomDimension(Level level, GravityChannel channel, GravityContext context) {
        // Custom-dimension gravity is mobs-only on Fabric.
        if (channel != GravityChannel.MOB) return false;

        String dimensionId = level.dimension().location().toString();
        Config.CustomDimConfig config = Config.getCustomDimConfig(dimensionId).orElse(null);
        if (config == null || !config.isEnableCustomGravity()) return false;

        context.setSource(GravitySource.CUSTOM_DIMENSION);
        context.setOffset(config.getMobGravityOffset());
        return true;
    }

    private static boolean resolveAtmosphere(Entity entity, Level level, GravityContext context) {
        double offset;

        // The built-in Overworld atmosphere always uses the configurable 8-layer
        // pressure curve (the same curve as the audio filter and the Sable datapack).
        // dimensions.json ships a legacy Overworld entry whose linear ramp is
        // intentionally ignored so entity gravity stays in step with the layers.
        if (level.dimension() == Level.OVERWORLD) {
            if (!Config.COMMON.overworldEntityGravity.get()) return false;
            if (!Config.COMMON.enableAtmosphere.get()) return false;

            double realY = EndlessGravityAPI.getRealY(entity);
            if (realY <= EndlessGravityAPI.BASE) return false;
            offset = EndlessGravityAPI.getAtmosphereOffset(realY);
        } else {
            // Other dimensions may define their own linear atmosphere ramp.
            String dimensionId = level.dimension().location().toString();
            Config.CustomDimConfig custom = Config.getCustomDimConfig(dimensionId).orElse(null);
            if (custom == null || !custom.isEnableAtmosphere()) return false;

            double base = custom.getAtmosphereBaseAltitude();
            double span = custom.getAtmosphereMaxAltitude() - base;
            if (span <= 0) return false;
            double realY = EndlessGravityAPI.getRealY(entity);
            if (realY <= base) return false;
            double progress = Math.max(0.0, Math.min(1.0, (realY - base) / span));
            offset = progress * custom.getAtmosphereGravityMax();
        }

        if (offset <= 0) return false;
        double gravity = entity.getGravity();
        if (offset > gravity) offset = gravity;
        if (offset <= 0) return false;

        context.setSource(GravitySource.ATMOSPHERE);
        context.setOffset(offset);
        return true;
    }

    private static boolean resolveEnd(Level level, GravityChannel channel, GravityContext context) {
        if (level.dimension() != Level.END) return false;
        if (!Config.COMMON.endEntityGravity.get()) return false;

        Double offset = endOffset(channel);
        if (offset == null) return false;

        context.setSource(GravitySource.END);
        context.setOffset(offset);
        return true;
    }

    private static Double endOffset(GravityChannel channel) {
        switch (channel) {
            case PLAYER: return Config.COMMON.enablePlayerGravity.get() ? Config.COMMON.playerGravityOffset.get() : null;
            case MOB: return Config.COMMON.enableMobGravity.get() ? Config.COMMON.mobGravityOffset.get() : null;
            case ITEM: return Config.COMMON.enableItemGravity.get() ? Config.COMMON.itemGravityOffset.get() : null;
            case ARROW: return Config.COMMON.enableArrowGravity.get() ? Config.COMMON.arrowGravityOffset.get() : null;
            case THROWN: return Config.COMMON.enableThrownGravity.get() ? Config.COMMON.thrownGravityOffset.get() : null;
            case BLOCK: return Config.COMMON.enableBlockGravity.get() ? Config.COMMON.blockGravityOffset.get() : null;
            default: return null;
        }
    }
}
