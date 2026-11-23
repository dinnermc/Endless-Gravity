package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.api.BiomeGravityRegistry;
import dinner.dev.endless_gravity.api.BiomeGravityRule;
import dinner.dev.endless_gravity.api.GravityChannel;
import dinner.dev.endless_gravity.api.GravityContext;
import dinner.dev.endless_gravity.api.GravityModifierRegistry;
import dinner.dev.endless_gravity.api.GravitySource;
import dinner.dev.endless_gravity.event.GravityCalculationEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Single home for "what offset does Endless Gravity apply to this entity?".
 *
 * <p>Every application point (the server tick, the client player tick and the entity
 * mixins) goes through {@link #compute}, so {@link dinner.dev.endless_gravity.api.GravityApi#getGravityOffset}
 * and the mod's own behaviour can never drift apart. {@link #compute} only calculates; callers
 * apply the offset with {@link Entity#setDeltaMovement}.
 */
public final class GravityResolver {

    private static final double VANILLA_LIVING_GRAVITY = 0.08;
    private static final double VANILLA_AIR_DRAG = 0.98;
    private static final double MAX_DRAG_SPEED = 3.5;

    private GravityResolver() {}

    /** The channel an entity belongs to, or {@code null} if it has none. */
    public static GravityChannel channelFor(Entity entity) {
        if (entity instanceof Player) return GravityChannel.PLAYER;
        if (entity instanceof LivingEntity) return GravityChannel.MOB;
        if (entity instanceof ItemEntity) return GravityChannel.ITEM;
        if (entity instanceof AbstractArrow) return GravityChannel.ARROW;
        if (entity instanceof ThrowableProjectile) return GravityChannel.THROWN;
        if (entity instanceof FallingBlockEntity) return GravityChannel.BLOCK;
        if (entity instanceof PrimedTnt) return GravityChannel.TNT;
        if (entity instanceof ExperienceOrb) return GravityChannel.XP_ORB;
        return null;
    }

    /** Shared preconditions: grounded, in water, immune, or self-propelled/anchored entities. */
    public static boolean canApply(Entity entity, GravityChannel channel) {
        if (entity.onGround() || entity.isInWater()) return false;
        if (EndlessGravityAPI.isGravityImmune(entity)) return false;

        if (channel == GravityChannel.PLAYER) {
            if (entity instanceof Player player) {
                if (player.getAbilities().flying || player.isFallFlying()) return false;
                if (player.isNoGravity() || player.isPassenger() || player.onClimbable()
                        || player.isInLava() || player.isSleeping()) return false;
            }
        } else if (channel == GravityChannel.MOB) {
            if (entity instanceof LivingEntity living) {
                if (living.isFallFlying()) return false;
                if (GravityHandler.isFlightOrImmobile(living)) return false;
            }
        }
        return true;
    }

    /** Resolve the base offset, biome override, zones, potions, then run every modifier. Never applies anything. */
    public static GravityContext compute(Entity entity, GravityChannel channel) {
        Level level = entity.level();
        GravityContext context = new GravityContext(entity, level, channel);

        if (!GravityGameRules.enabled(level)) {
            return context;
        }

        resolveDimensionProfile(entity, level, channel, context);
        resolveBiomeOverride(level, entity.position(), context.baseGravity(), context);
        applyEntityGravity(entity, context);

        NeoForge.EVENT_BUS.post(new GravityCalculationEvent(context));
        GravityModifierRegistry.apply(context);
        return context;
    }

    /**
     * Resolve the dimension/biome/zone gravity offset at a world position without
     * requiring an {@link Entity} instance (for shaders, vehicles, HUDs and addon queries).
     */
    public static double computeAtPosition(Level level, Vec3 pos, GravityChannel channel) {
        if (level == null || pos == null || channel == null) return 0.0;
        if (!GravityGameRules.enabled(level)) return 0.0;

        double defaultBase = defaultGravityForChannel(channel);
        double offset = 0.0;

        if (!(EndlessGravityAPI.isOverworldOrSable(level) && EndlessGravityAPI.isCosmonauticsInstalled())) {
            CustomDimConfig config = Config.getCustomDimConfig(level.dimension().location()).orElse(null);
            if (config != null) {
                if (config.getMode() == CustomDimConfig.Mode.FIXED_GRAVITY) {
                    offset = fixedOffsetForChannel(config, channel);
                } else {
                    double realY = EndlessGravityAPI.getRealY(level, pos);
                    double atmOffset = config.getAtmosphereGravityOffset(realY);
                    if (channel == GravityChannel.PLAYER || channel == GravityChannel.MOB) {
                        offset = Math.min(atmOffset, VANILLA_LIVING_GRAVITY);
                    } else if (config.isAtmosphereAffectNonLiving()) {
                        double fraction = Math.min(1.0, atmOffset / VANILLA_LIVING_GRAVITY);
                        offset = defaultBase * fraction;
                    }
                }
            }
        }

        BiomeGravityRule biomeRule = matchBiomeRule(level, pos);
        if (biomeRule != null) {
            offset = biomeRule.resolveOffset(defaultBase);
        }
        return offset;
    }

    /**
     * Optional pressure-based vertical air drag for living entities in atmosphere dimensions.
     * In thin air/vacuum ({@code pressure < 1}), counteracts part of vanilla's 2%/tick air drag;
     * in dense atmospheres ({@code pressure > 1}), adds gentle extra vertical drag.
     */
    public static void applyAtmosphericDrag(Entity entity) {
        if (!Config.COMMON.enableAtmosphericDrag.get()) return;
        Level level = entity.level();
        if (!GravityGameRules.enabled(level)) return;
        CustomDimConfig profile = Config.getCustomDimConfig(level.dimension().location()).orElse(null);
        if (profile == null || profile.getMode() != CustomDimConfig.Mode.ATMOSPHERE) return;

        double pressure = profile.getAtmospherePressure(EndlessGravityAPI.getRealY(entity));
        Vec3 vel = entity.getDeltaMovement();
        double vy = vel.y;
        if (Math.abs(vy) < 1e-5) return;

        if (pressure < 0.999) {
            if (Math.abs(vy) < MAX_DRAG_SPEED) {
                double thinness = 1.0 - Math.max(0.0, pressure);
                double counterFactor = ((1.0 / VANILLA_AIR_DRAG) - 1.0) * thinness;
                entity.setDeltaMovement(vel.x, vy * (1.0 + counterFactor), vel.z);
            }
        } else if (pressure > 1.001) {
            double extraDrag = Math.min(0.12, (pressure - 1.0) * 0.025);
            entity.setDeltaMovement(vel.x, vy * (1.0 - extraDrag), vel.z);
        }
    }

    /** Status effects on living entities that change gravity, scaled by ambient gravity. */
    private static void applyEntityGravity(Entity entity, GravityContext context) {
        if (!(entity instanceof LivingEntity living)) return;
        if (!Config.COMMON.enablePotionGravity.get()) return;

        double base = context.baseGravity();
        double currentEffective = Math.max(0.0, base - context.offset());

        MobEffectInstance weightlessness = living.getEffect(ModEffects.weightlessnessHolder());
        if (weightlessness != null) {
            double bonus = PotionGravityMath.weightlessnessOffset(
                    Config.COMMON.weightlessnessGravityOffset.get(),
                    weightlessness.getAmplifier(),
                    base,
                    currentEffective);
            context.addOffset(bonus);
            currentEffective = Math.max(0.0, base - context.offset());
        }

        MobEffectInstance heaviness = living.getEffect(ModEffects.heavinessHolder());
        if (heaviness != null) {
            double pull = PotionGravityMath.heavinessPull(
                    Config.COMMON.heavinessGravityOffset.get(),
                    heaviness.getAmplifier(),
                    base,
                    currentEffective);
            context.addOffset(-pull);
        }
    }

    private static void resolveDimensionProfile(Entity entity, Level level, GravityChannel channel, GravityContext context) {
        // Create: Cosmonautics owns Overworld/Sable gravity when installed.
        if (EndlessGravityAPI.isOverworldOrSable(level) && EndlessGravityAPI.isCosmonauticsInstalled()) {
            return;
        }

        CustomDimConfig config = Config.getCustomDimConfig(level.dimension().location()).orElse(null);
        if (config == null) return;

        if (config.getMode() == CustomDimConfig.Mode.FIXED_GRAVITY) {
            double offset = fixedOffsetForChannel(config, channel);
            context.setSource(level.dimension() == Level.END
                    ? GravitySource.END : GravitySource.CUSTOM_DIMENSION);
            context.setOffset(offset);
        } else if (channel == GravityChannel.PLAYER || channel == GravityChannel.MOB) {
            double realY = EndlessGravityAPI.getRealY(entity);
            double offset = Math.min(config.getAtmosphereGravityOffset(realY), entity.getGravity());
            context.setSource(GravitySource.ATMOSPHERE);
            context.setOffset(offset);
        } else if (config.isAtmosphereAffectNonLiving()) {
            double realY = EndlessGravityAPI.getRealY(entity);
            double atmOffset = config.getAtmosphereGravityOffset(realY);
            double entityBase = entity.getGravity();
            double fraction = Math.min(1.0, atmOffset / VANILLA_LIVING_GRAVITY);
            context.setSource(GravitySource.ATMOSPHERE);
            context.setOffset(entityBase * fraction);
        }
    }

    private static void resolveBiomeOverride(Level level, Vec3 pos, double baseGravity, GravityContext context) {
        BiomeGravityRule rule = matchBiomeRule(level, pos);
        if (rule != null) {
            context.setSource(GravitySource.BIOME);
            context.setOffset(rule.resolveOffset(baseGravity));
        }
    }

    private static BiomeGravityRule matchBiomeRule(Level level, Vec3 pos) {
        if (BiomeGravityRegistry.activeRules().isEmpty()) return null;
        String dimId = level.dimension().location().toString();
        Holder<Biome> biomeHolder = level.getBiome(BlockPos.containing(pos));
        String biomeId = biomeHolder.unwrapKey().map(k -> k.location().toString()).orElse(null);
        return BiomeGravityRegistry.match(dimId, biomeId, tagId -> {
            ResourceLocation loc = ResourceLocation.tryParse(tagId);
            return loc != null && biomeHolder.is(TagKey.create(Registries.BIOME, loc));
        });
    }

    private static double fixedOffsetForChannel(CustomDimConfig config, GravityChannel channel) {
        return switch (channel) {
            case PLAYER -> config.getPlayerGravityOffset();
            case MOB -> config.getMobGravityOffset();
            case ITEM -> config.getItemGravityOffset();
            case ARROW -> config.getArrowGravityOffset();
            case THROWN, XP_ORB -> config.getThrownGravityOffset();
            case BLOCK, TNT -> config.getBlockGravityOffset();
        };
    }

    private static double defaultGravityForChannel(GravityChannel channel) {
        return switch (channel) {
            case PLAYER, MOB -> 0.08;
            case ITEM, BLOCK, TNT -> 0.04;
            case ARROW -> 0.05;
            case THROWN, XP_ORB -> 0.03;
        };
    }
}
