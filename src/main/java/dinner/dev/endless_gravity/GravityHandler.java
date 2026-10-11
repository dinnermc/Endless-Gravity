package dinner.dev.endless_gravity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class GravityHandler {

    private static volatile MinecraftServer server;

    /** Valores de config cacheados por tick (actualizados en onLevelTick). */
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

    public static boolean applyAtmosphereEffects(Level level, Entity entity) {
        if (!isOverworld(level)) {
            return false;
        }
        if (!cacheOverworldEntityGravity) {
            return false;
        }
        if (!cacheEnableAtmosphere) {
            return false;
        }

        double realY = EndlessGravityAPI.getRealY(entity);
        if (realY <= EndlessGravityAPI.BASE) {
            return false;
        }

        double offset = EndlessGravityAPI.getAtmosphereOffset(realY);
        if (offset <= 0) {
            return false;
        }

        double entityGravity = entity.getGravity();
        if (offset > entityGravity) {
            offset = entityGravity;
        }
        if (offset <= 0) {
            return false;
        }

        applyGravity(entity, offset);
        return true;
    }

    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) return true;
        if (!source.is(DamageTypeTags.IS_FALL)) return true;

        Level level = player.level();

        if (cacheEnableAtmosphere
                && isOverworld(level)
                && EndlessGravityAPI.getRealY(player) > cacheNoFallDamageAltitude) {
            return false;
        }

        if (isEnd(level) && cacheFallDamageMode == 1) {
            return false;
        }
        return true;
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

    // Getters para mixins (evitan reflejo/boxeo repetido)
    public static boolean isEndEntityGravity() { return cacheEndEntityGravity; }
    public static boolean isOverworldEntityGravity() { return cacheOverworldEntityGravity; }
    public static boolean isEnableAtmosphere() { return cacheEnableAtmosphere; }
    public static double getAtmosphereGravityMax() { return cacheAtmosphereGravityMax; }
    public static double getNoFallDamageAltitude() { return cacheNoFallDamageAltitude; }
    public static double getAtmosphereMuffleGain() { return cacheAtmosphereMuffleGain; }
    public static double getAtmosphereMuffleGainHF() { return cacheAtmosphereMuffleGainHF; }
    public static boolean isEnableItemGravity() { return cacheEnableItemGravity; }
    public static double getItemGravityOffset() { return cacheItemGravityOffset; }
    public static boolean isEnableArrowGravity() { return cacheEnableArrowGravity; }
    public static double getArrowGravityOffset() { return cacheArrowGravityOffset; }
    public static boolean isEnableThrownGravity() { return cacheEnableThrownGravity; }
    public static double getThrownGravityOffset() { return cacheThrownGravityOffset; }
    public static boolean isEnableBlockGravity() { return cacheEnableBlockGravity; }
    public static double getBlockGravityOffset() { return cacheBlockGravityOffset; }
    public static int getFallDamageMode() { return cacheFallDamageMode; }
    public static double getParticleGravityOffset() { return cacheParticleGravityOffset; }
}