package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.api.GravityChannel;
import dinner.dev.endless_gravity.api.GravityContext;
import dinner.dev.endless_gravity.api.GravityModifierRegistry;
import dinner.dev.endless_gravity.api.GravitySource;
import dinner.dev.endless_gravity.event.GravityCalculationEvent;
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
import net.neoforged.neoforge.common.NeoForge;

/**
 * Single home for "what offset does Endless Gravity apply to this entity?".
 *
 * <p>Every application point (the server tick, the client player tick and the entity
 * mixins) goes through {@link #compute}, so {@link GravityApi#getGravityOffset} and the
 * mod's own behaviour can never drift apart. {@link #compute} only calculates; callers
 * apply the offset with {@link net.minecraft.world.entity.Entity#setDeltaMovement}.
 */
public final class GravityResolver {

    private GravityResolver() {}

    /** The channel an entity belongs to, or {@code null} if it has none. */
    public static GravityChannel channelFor(Entity entity) {
        if (entity instanceof Player) return GravityChannel.PLAYER;
        if (entity instanceof LivingEntity) return GravityChannel.MOB;
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity) return GravityChannel.ITEM;
        if (entity instanceof net.minecraft.world.entity.projectile.AbstractArrow) return GravityChannel.ARROW;
        if (entity instanceof net.minecraft.world.entity.projectile.ThrowableProjectile) return GravityChannel.THROWN;
        if (entity instanceof net.minecraft.world.entity.item.FallingBlockEntity) return GravityChannel.BLOCK;
        return null;
    }

    /** Shared preconditions: grounded, in water, immune, or self-propelled entities. */
    public static boolean canApply(Entity entity, GravityChannel channel) {
        if (entity.onGround() || entity.isInWater()) return false;
        if (EndlessGravityAPI.isGravityImmune(entity)) return false;

        if (channel == GravityChannel.PLAYER) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living && living.isFallFlying()) return false;
            if (entity instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().flying) return false;
        } else if (channel == GravityChannel.MOB) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                if (living.isFallFlying()) return false;
                if (GravityHandler.isFlightOrImmobile(living)) return false;
            }
        }
        return true;
    }

    /** Resolve the base offset, then run every modifier. Never applies anything. */
    public static GravityContext compute(Entity entity, GravityChannel channel) {
        Level level = entity.level();
        GravityContext context = new GravityContext(entity, level, channel);

        if (!GravityGameRules.enabled(level)) {
            return context;
        }

        resolveDimensionProfile(entity, level, channel, context);

        applyEntityGravity(entity, context);

        NeoForge.EVENT_BUS.post(new GravityCalculationEvent(context));
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

    private static void resolveDimensionProfile(Entity entity, Level level, GravityChannel channel, GravityContext context) {
        String dimensionId = level.dimension().location().toString();
        CustomDimConfig config = Config.getCustomDimConfig(dimensionId).orElse(null);
        if (config == null) return;

        double offset = 0.0;
        if (config.getMode() == CustomDimConfig.Mode.FIXED_GRAVITY) {
            offset = switch (channel) {
                case PLAYER -> config.getPlayerGravityOffset();
                case MOB -> config.getMobGravityOffset();
                case ITEM -> config.getItemGravityOffset();
                case ARROW -> config.getArrowGravityOffset();
                case THROWN -> config.getThrownGravityOffset();
                case BLOCK -> config.getBlockGravityOffset();
            };
        } else if (channel == GravityChannel.PLAYER || channel == GravityChannel.MOB) {
            double realY = EndlessGravityAPI.getRealY(entity);
            offset = config.getAtmosphereGravityOffset(realY);
            offset = Math.min(offset, entity.getGravity());
        }
        context.setSource(config.getMode() == CustomDimConfig.Mode.ATMOSPHERE
                ? GravitySource.ATMOSPHERE : GravitySource.CUSTOM_DIMENSION);
        context.setOffset(offset);
    }

}
