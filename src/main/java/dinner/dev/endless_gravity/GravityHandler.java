package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.api.GravityChannel;
import dinner.dev.endless_gravity.api.GravityContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class GravityHandler {

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(GravityHandler::onLevelTick);
    }

    private static void onLevelTick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            applyPlayerGravity(player);
        }
    }

    private static void applyPlayerGravity(Player player) {
        if (!GravityResolver.canApply(player, GravityChannel.PLAYER)) return;

        GravityContext context = GravityResolver.compute(player, GravityChannel.PLAYER);
        if (context.applicable()) {
            applyGravity(player, context.offset());
        }
        GravityResolver.applyAtmosphericDrag(player);
    }

    /**
     * Adjusts fall distance before vanilla calculates fall damage (invoked from
     * {@code LivingEntityMixin#causeFallDamage}). Returns {@code 0.0F} to cancel fall
     * damage cleanly, or a scaled fall distance in proportional mode.
     */
    public static float adjustFallDistance(LivingEntity entity, float fallDistance) {
        if (fallDistance <= 0.0F) return fallDistance;

        Level level = entity.level();
        if (level == null || !GravityGameRules.enabled(level)) return fallDistance;

        boolean isPlayer = entity instanceof Player;
        double fallStartY = AtmosphereLayers.getFallStartAltitude(
                EndlessGravityAPI.getRealY(entity), fallDistance);

        Config.CustomDimConfig profile = Config.getCustomDimConfig(level.dimension().location()).orElse(null);

        // Altitude-based immunity in atmosphere profiles (always checked first).
        if (profile != null && profile.isEnableAtmosphere()) {
            Double noFallAltitude = profile.getNoFallDamageAltitude();
            if (noFallAltitude != null && fallStartY > noFallAltitude) {
                return 0.0F;
            }
        } else if (profile == null && Config.COMMON.enableAtmosphere.get()
                && EndlessGravityAPI.isOverworld(level)
                && fallStartY > Config.COMMON.noFallDamageAltitude.get()) {
            return 0.0F;
        }

        // Per-dimension explicit fall-damage mode overrides global End behavior.
        if (profile != null && profile.getFallDamageMode() != Config.CustomDimConfig.FallDamageMode.INHERIT) {
            return switch (profile.getFallDamageMode()) {
                case NORMAL -> fallDistance;
                case DISABLED -> 0.0F;
                case PROPORTIONAL -> scaleProportionalFallDistance(entity, isPlayer, fallDistance);
                default -> fallDistance;
            };
        }

        if (isEnd(level)) {
            int mode = isPlayer ? Config.COMMON.fallDamageMode.get() : Config.COMMON.mobFallDamageMode.get();
            if (mode == 1) return 0.0F;
            if (mode == 2) return scaleProportionalFallDistance(entity, isPlayer, fallDistance);
        }
        return fallDistance;
    }

    private static float scaleProportionalFallDistance(LivingEntity entity, boolean isPlayer, float fallDistance) {
        if (EndlessGravityAPI.isGravityImmune(entity)) return fallDistance;
        GravityChannel channel = isPlayer ? GravityChannel.PLAYER : GravityChannel.MOB;
        GravityContext ctx = GravityResolver.compute(entity, channel);
        if (!ctx.applicable()) return fallDistance;
        double base = ctx.baseGravity();
        if (base <= 0.0) return fallDistance;
        double ratio = AtmosphereLayers.clamp(ctx.effectiveGravity() / base, 0.0, 4.0);
        return ratio <= 0.01 ? 0.0F : (float) (fallDistance * ratio);
    }

    /**
     * Flying or immobile living entities must not receive reduced-gravity impulses: they hover on
     * their own, or are anchored (riding, climbing, in lava, asleep). Mobs with bespoke move
     * controls (blaze, vex, wither) are not covered here, so the gravity_immune tag stays as the
     * safety net.
     */
    public static boolean isFlightOrImmobile(LivingEntity entity) {
        if (entity.isNoGravity()) return true;
        if (entity.isPassenger()) return true;
        if (entity.onClimbable()) return true;
        if (entity.isInLava()) return true;
        if (entity.isSleeping()) return true;
        if (entity instanceof FlyingAnimal) return true;
        if (entity instanceof FlyingMob) return true;
        if (entity instanceof Mob mob && mob.getMoveControl() instanceof FlyingMoveControl) return true;
        return false;
    }

    public static boolean isEnd(Level level) {
        return level.dimension() == Level.END;
    }

    public static boolean isOverworld(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }

    static void applyGravity(Entity entity, double offset) {
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, offset, 0));
    }

    // Public getters retained for compatibility
    public static boolean isEndEntityGravity() { return Config.COMMON.endEntityGravity.get(); }
    public static boolean isOverworldEntityGravity() { return Config.COMMON.overworldEntityGravity.get(); }
    public static boolean isEnableAtmosphere() { return Config.COMMON.enableAtmosphere.get(); }
    public static double getAtmosphereGravityMax() { return Config.COMMON.atmosphereGravityMax.get(); }
    public static double getNoFallDamageAltitude() { return Config.COMMON.noFallDamageAltitude.get(); }
    public static double getAtmosphereMuffleGain() { return Config.COMMON.atmosphereMuffleGain.get(); }
    public static double getAtmosphereMuffleGainHF() { return Config.COMMON.atmosphereMuffleGainHF.get(); }
    public static boolean isEnableItemGravity() { return Config.COMMON.enableItemGravity.get(); }
    public static double getItemGravityOffset() { return Config.COMMON.itemGravityOffset.get(); }
    public static boolean isEnableArrowGravity() { return Config.COMMON.enableArrowGravity.get(); }
    public static double getArrowGravityOffset() { return Config.COMMON.arrowGravityOffset.get(); }
    public static boolean isEnableThrownGravity() { return Config.COMMON.enableThrownGravity.get(); }
    public static double getThrownGravityOffset() { return Config.COMMON.thrownGravityOffset.get(); }
    public static boolean isEnableBlockGravity() { return Config.COMMON.enableBlockGravity.get(); }
    public static double getBlockGravityOffset() { return Config.COMMON.blockGravityOffset.get(); }
    public static int getFallDamageMode() { return Config.COMMON.fallDamageMode.get(); }
    public static double getParticleGravityOffset() { return Config.COMMON.particleGravityOffset.get(); }
    public static boolean isEnableMobGravity() { return Config.COMMON.enableMobGravity.get(); }
    public static double getMobGravityOffset() { return Config.COMMON.mobGravityOffset.get(); }
}
