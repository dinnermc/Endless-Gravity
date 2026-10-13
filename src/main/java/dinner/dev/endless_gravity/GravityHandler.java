package dinner.dev.endless_gravity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class GravityHandler {

    private static volatile MinecraftServer server;

    /** Valores de config cacheados por tick (actualizados en onServerTick). */
    private static boolean cacheEndEntityGravity;
    private static boolean cacheEnablePlayerGravity;
    private static double cachePlayerGravityOffset;
    private static boolean cacheOverworldEntityGravity;
    private static boolean cacheEnableAtmosphere;
    private static double cacheAtmosphereGravityMax;
    private static double cacheNoFallDamageAltitude;
    private static double cacheAtmosphereMuffleGain;
    private static double cacheAtmosphereMuffleGainHF;
    private static boolean cacheEnableItemGravity;
    private static double cacheItemGravityOffset;
    private static boolean cacheEnableArrowGravity;
    private static double cacheArrowGravityOffset;
    private static boolean cacheEnableThrownGravity;
    private static double cacheThrownGravityOffset;
    private static boolean cacheEnableBlockGravity;
    private static double cacheBlockGravityOffset;
    private static int cacheFallDamageMode;
    private static double cacheParticleGravityOffset;
    private static boolean cacheEnableMobGravity;
    private static double cacheMobGravityOffset;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> GravityHandler.server = server);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> GravityHandler.server = null);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server != null) {
                for (ServerLevel level : server.getAllLevels()) {
                    onLevelTick(level);
                }
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(GravityHandler::onAllowDamage);
    }

    private static void onLevelTick(ServerLevel level) {
        // Skip gravity when paused (prevents velocity accumulation during pause)
        MinecraftServer server = level.getServer();
        if (server != null && server.isPaused()) return;

        refreshConfigCache();

        for (ServerPlayer player : level.players()) {
            applyPlayerGravity(player, level);
        }
    }

    private static void refreshConfigCache() {
        cacheEndEntityGravity = Config.COMMON.endEntityGravity.get();
        cacheEnablePlayerGravity = Config.COMMON.enablePlayerGravity.get();
        cachePlayerGravityOffset = Config.COMMON.playerGravityOffset.get();
        cacheOverworldEntityGravity = Config.COMMON.overworldEntityGravity.get();
        cacheEnableAtmosphere = Config.COMMON.enableAtmosphere.get();
        cacheAtmosphereGravityMax = Config.COMMON.atmosphereGravityMax.get();
        cacheNoFallDamageAltitude = Config.COMMON.noFallDamageAltitude.get();
        cacheAtmosphereMuffleGain = Config.COMMON.atmosphereMuffleGain.get();
        cacheAtmosphereMuffleGainHF = Config.COMMON.atmosphereMuffleGainHF.get();
        cacheEnableItemGravity = Config.COMMON.enableItemGravity.get();
        cacheItemGravityOffset = Config.COMMON.itemGravityOffset.get();
        cacheEnableArrowGravity = Config.COMMON.enableArrowGravity.get();
        cacheArrowGravityOffset = Config.COMMON.arrowGravityOffset.get();
        cacheEnableThrownGravity = Config.COMMON.enableThrownGravity.get();
        cacheThrownGravityOffset = Config.COMMON.thrownGravityOffset.get();
        cacheEnableBlockGravity = Config.COMMON.enableBlockGravity.get();
        cacheBlockGravityOffset = Config.COMMON.blockGravityOffset.get();
        cacheFallDamageMode = Config.COMMON.fallDamageMode.get();
        cacheParticleGravityOffset = Config.COMMON.particleGravityOffset.get();
        cacheEnableMobGravity = Config.COMMON.enableMobGravity.get();
        cacheMobGravityOffset = Config.COMMON.mobGravityOffset.get();
    }

    private static void applyPlayerGravity(Player player, Level level) {
        if (player.onGround() || player.isInWater() || player.isFallFlying()) return;
        if (player.getAbilities().flying) return;

        if (EndlessGravityAPI.isGravityImmune(player)) return;

        if (applyAtmosphereEffects(level, player)) return;

        if (!isEnd(level)) return;
        if (!cacheEndEntityGravity) return;
        if (!cacheEnablePlayerGravity) return;

        applyGravity(player, cachePlayerGravityOffset);
    }

    /**
     * Applies gravity for custom dimensions with custom config (mirrors the NeoForge handler).
     */
    public static void applyCustomDimensionGravity(Entity entity, Level level) {
        String dimId = level.dimension().location().toString();
        Config.getCustomDimConfig(dimId).ifPresent(config -> {
            if (config.isEnableCustomGravity()) {
                if (!(entity instanceof LivingEntity living)) return;
                if (!living.onGround() && !living.isInWater() && !living.isFallFlying()) {
                    if (living instanceof Player player && player.getAbilities().flying) return;
                    applyGravity(living, config.getMobGravityOffset());
                }
            }
        });
    }

    public static boolean applyAtmosphereEffects(Level level, Entity entity) {
        // Check custom dimension atmosphere first
        String dimId = level.dimension().location().toString();
        if (Config.hasCustomAtmosphere(dimId)) {
            Config.CustomDimConfig config = Config.getCustomDimConfig(dimId).orElse(null);
            if (config == null || !config.isEnableAtmosphere()) return false;

            double realY = EndlessGravityAPI.getRealY(entity);
            if (realY <= config.getAtmosphereBaseAltitude()) return false;

            double progress = (realY - config.getAtmosphereBaseAltitude()) / (config.getAtmosphereMaxAltitude() - config.getAtmosphereBaseAltitude());
            progress = Math.max(0.0, Math.min(1.0, progress));

            double offset = progress * config.getAtmosphereGravityMax();
            if (offset <= 0) return false;

            double entityGravity = entity.getGravity();
            if (offset > entityGravity) offset = entityGravity;
            if (offset <= 0) return false;

            applyGravity(entity, offset);
            return true;
        }

        // Vanilla atmosphere (Overworld)
        if (!isOverworld(level)) return false;
        if (!Config.COMMON.overworldEntityGravity.get()) return false;
        if (!Config.COMMON.enableAtmosphere.get()) return false;

        double realY = EndlessGravityAPI.getRealY(entity);
        if (realY <= EndlessGravityAPI.BASE) return false;

        double offset = EndlessGravityAPI.getAtmosphereOffset(realY);
        if (offset <= 0) return false;

        double entityGravity = entity.getGravity();
        if (offset > entityGravity) offset = entityGravity;
        if (offset <= 0) return false;

        applyGravity(entity, offset);
        return true;
    }
    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.IS_FALL)) return true;

        boolean isPlayer = entity instanceof Player;
        Level level = entity.level();

        // Custom dimension fall damage
        String dimId = level.dimension().location().toString();
        if (Config.hasCustomAtmosphere(dimId)) {
            Config.CustomDimConfig config = Config.getCustomDimConfig(dimId).orElse(null);
            if (config != null && config.isEnableAtmosphere() && EndlessGravityAPI.getRealY(entity) > config.getAtmosphereMaxAltitude()) {
                return false;
            }
        }

        // Vanilla fall damage
        if (Config.COMMON.enableAtmosphere.get()
                && EndlessGravityAPI.isOverworld(level)
                && EndlessGravityAPI.getRealY(entity) > Config.COMMON.noFallDamageAltitude.get()) {
            return false;
        }

        if (isEnd(level)) {
            int mode = isPlayer ? Config.COMMON.fallDamageMode.get() : Config.COMMON.mobFallDamageMode.get();
            if (mode == 1) {
                return false;
            }
        }
        return true;
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

    // Public getters for mixins
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