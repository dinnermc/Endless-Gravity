package dinner.dev.endless_gravity.api;

import dinner.dev.endless_gravity.AtmosphereLayers;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityResolver;
import dinner.dev.endless_gravity.MuffleState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Stable public API for Endless Gravity.
 *
 * <p>Everything in this {@code api} package is intended to stay source- and
 * binary-compatible within a major API version (see {@link #API_VERSION}). Anything
 * outside it - {@code Config}, the mixins, {@code GravityHandler}, the network layer -
 * is internal and may change without notice.
 *
 * <p>Addons can read the atmosphere/End values Endless Gravity uses, ask what offset it
 * would apply to an entity ({@link #getGravityOffset(Entity)}), and register
 * {@link GravityModifier}s to change or cancel that offset in a loader-agnostic way.
 */
public final class GravityApi {

    /**
     * Major version of this API. Bumped when a breaking change is made to the
     * {@code api} package. Addons can compare it, but a compile-time dependency on
     * the mod version is usually enough.
     */
    public static final int API_VERSION = 1;

    /**
     * Entity-type tag. Add entity IDs to
     * {@code data/endless_gravity/tags/entity_type/gravity_immune.json} (in a datapack
     * or your own mod) to make them ignore reduced gravity. This needs no dependency
     * on Endless Gravity's classes.
     */
    public static final TagKey<EntityType<?>> GRAVITY_IMMUNE =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("endless_gravity", "gravity_immune"));

    /** Sea level. Atmosphere effects start above this Y. */
    public static final double BASE = 64.0;

    private GravityApi() {}

    /** True if the entity is excluded from reduced gravity (tag or config list). */
    public static boolean isGravityImmune(Entity entity) {
        return EndlessGravityAPI.isGravityImmune(entity);
    }

    /** Atmospheric pressure at a world Y, from the Overworld/global pressure curve (1.0 = full atmosphere). */
    public static double getPressure(double y) {
        return EndlessGravityAPI.getPressure(y);
    }

    /** Atmospheric pressure at a world Y for a specific level's configured atmosphere profile. */
    public static double getPressure(Level level, double y) {
        return EndlessGravityAPI.getPressure(level, y);
    }

    /** How far into the atmosphere a world Y is, 0.0 (base) to 1.0 (vacuum), using the Overworld/global curve. */
    public static double getAtmosphereProgress(double y) {
        return EndlessGravityAPI.getAtmosphereProgress(y);
    }

    /** How far into the atmosphere a world Y is for a specific level (0.0 if not in atmosphere mode). */
    public static double getAtmosphereProgress(Level level, double y) {
        return EndlessGravityAPI.getAtmosphereProgress(level, y);
    }

    /** Upward atmosphere offset Endless Gravity would apply at a world Y in the Overworld. */
    public static double getAtmosphereOffset(double y) {
        return EndlessGravityAPI.getAtmosphereOffset(y);
    }

    /** Upward atmosphere offset Endless Gravity would apply at a world Y in the given level. */
    public static double getAtmosphereOffset(Level level, double y) {
        return EndlessGravityAPI.getAtmosphereOffset(level, y);
    }

    /**
     * Global Y of an entity. Inside a Sable sub-level the entity's own Y is local to
     * that level; on NeoForge this projects it back to Overworld coordinates.
     */
    public static double getRealY(Entity entity) {
        return EndlessGravityAPI.getRealY(entity);
    }

    /**
     * The offset Endless Gravity would apply to this entity right now, {@code 0.0} if
     * none. This runs the exact same resolution (and modifiers) the mod itself uses, so
     * a value of {@code 0.0} means the mod is leaving vanilla gravity alone.
     */
    public static double getGravityOffset(Entity entity) {
        GravityChannel channel = channelFor(entity);
        if (channel == null) return 0.0;
        GravityContext context = GravityResolver.compute(entity, channel);
        return context.applicable() ? context.offset() : 0.0;
    }

    /**
     * Resolve the configured dimension gravity offset at a world position without needing
     * an {@link Entity} instance. Useful for shaders, vehicles, custom particles and HUDs.
     */
    public static double getGravityOffset(Level level, Vec3 pos, GravityChannel channel) {
        return GravityResolver.computeAtPosition(level, pos, channel);
    }

    /** Convenience overload of {@link #getGravityOffset(Level, Vec3, GravityChannel)}. */
    public static double getGravityOffset(Level level, double x, double y, double z, GravityChannel channel) {
        return GravityResolver.computeAtPosition(level, new Vec3(x, y, z), channel);
    }

    /**
     * The full calculation context for an entity (base source, offset after modifiers,
     * cancellation). Runs the same resolution as {@link #getGravityOffset(Entity)}.
     *
     * @return the context, or {@code null} if the entity has no gravity channel
     */
    public static GravityContext getGravityContext(Entity entity) {
        GravityChannel channel = channelFor(entity);
        if (channel == null) return null;
        return GravityResolver.compute(entity, channel);
    }

    /**
     * The gravity channel an entity resolves to, or {@code null} if Endless Gravity
     * never applies reduced gravity to it.
     */
    public static GravityChannel channelFor(Entity entity) {
        return GravityResolver.channelFor(entity);
    }

    /**
     * Register a global modifier. The same id can be re-registered to replace it.
     *
     * @param id       unique id, namespaced to your mod (e.g. {@code mymod:low_gravity})
     * @param priority higher runs first
     * @param modifier called every time an offset is resolved for a matching entity
     */
    public static void registerModifier(ResourceLocation id, int priority, GravityModifier modifier) {
        GravityModifierRegistry.register(id, priority, modifier);
    }

    /** Remove a previously registered global modifier. Safe to call if it isn't registered. */
    public static void unregisterModifier(ResourceLocation id) {
        GravityModifierRegistry.unregister(id);
    }

    /**
     * Attach a modifier to a single entity instance. It runs after all global modifiers.
     * Pass {@code null} to remove it. The entity is held weakly.
     */
    public static void setEntityModifier(Entity entity, GravityModifier modifier) {
        GravityModifierRegistry.setEntityModifier(entity, modifier);
    }

    /** Remove an entity's modifier, if any. */
    public static void clearEntityModifier(Entity entity) {
        GravityModifierRegistry.clearEntityModifier(entity);
    }

    /** Immutable snapshot of all registered global modifiers in execution order. */
    public static List<GravityModifierRegistry.Entry> getRegisteredModifiers() {
        return GravityModifierRegistry.getRegisteredModifiers();
    }

    // ==================== BIOME GRAVITY OVERRIDES ====================

    /** Register (or replace) a programmatic biome gravity override rule. */
    public static void registerBiomeRule(BiomeGravityRule rule) {
        BiomeGravityRegistry.registerApiRule(rule);
    }

    /** Remove a programmatic biome gravity override rule by id. */
    public static boolean unregisterBiomeRule(ResourceLocation id) {
        return BiomeGravityRegistry.unregisterApiRule(id.toString());
    }

    /** Immutable snapshot of active biome gravity rules in descending priority order. */
    public static List<BiomeGravityRule> getBiomeRules() {
        return BiomeGravityRegistry.activeRules();
    }

    // ==================== CLIENT AUDIO MUFFLE HOOK ====================

    /**
     * Register a client-side modifier for Endless Gravity's OpenAL low-pass audio filter.
     * Higher priority runs first; calling {@link AudioMuffleContext#cancel()} restores clear
     * audio (e.g. inside pressurized cabins or sealed spacesuits).
     */
    public static void registerAudioMuffleModifier(ResourceLocation id, int priority, AudioMuffleModifier modifier) {
        AudioMuffleRegistry.register(id, priority, modifier);
    }

    /** Remove a previously registered audio muffle modifier. Safe to call if absent. */
    public static void unregisterAudioMuffleModifier(ResourceLocation id) {
        AudioMuffleRegistry.unregister(id);
    }

    /** True on the client when the OpenAL low-pass audio filter is currently attenuating world sound. */
    public static boolean isAudioMuffled() {
        return MuffleState.isActive();
    }

    /** Current applied overall audio gain ({@code 1.0} = clear, lower = quieter). */
    public static float getAudioMuffleGain() {
        return MuffleState.getCurrentGain();
    }

    /** Current applied high-frequency audio gain ({@code 1.0} = clear, lower = more muffled). */
    public static float getAudioMuffleGainHF() {
        return MuffleState.getCurrentGainHF();
    }
}
