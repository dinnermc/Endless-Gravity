package dinner.dev.endless_gravity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import dinner.dev.endless_gravity.CustomDimConfig;
import dinner.dev.endless_gravity.api.GravityChannel;
import dinner.dev.endless_gravity.api.GravityContext;

@EventBusSubscriber(modid = EndlessGravity.MODID)
public class GravityHandler {

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
    private static int cacheMobFallDamageMode;
    private static boolean cacheEnableSableEndGravity;
    private static double cacheSableGravityY;
    private static double cacheSablePressure;
    private static double cacheSableDrag;
    private static int cacheSableDatapackPriority;
    private static boolean cacheOverworldSableGravity;
    private static int cacheOverworldSableDatapackPriority;
    private static double cacheParticleGravityOffset;
    private static boolean cacheEnableMobGravity;
    private static double cacheMobGravityOffset;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        refreshConfigCache();
    }

    @SubscribeEvent
    public static void onEntityTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();

        // Only apply to living entities (players, mobs, etc.)
        if (!(entity instanceof LivingEntity)) return;

        Level level = entity.level();

        // Server-authoritative: skip on the logical client. The local player's reduced gravity
        // is applied client-side by ClientGravityHandler, so singleplayer must not apply it twice.
        if (level.isClientSide()) return;

        // Skip gravity when paused (prevents velocity accumulation during pause)
        MinecraftServer server = level.getServer();
        if (server != null && server.isPaused()) return;

        GravityChannel channel = (entity instanceof Player) ? GravityChannel.PLAYER : GravityChannel.MOB;
        if (!GravityResolver.canApply(entity, channel)) return;

        GravityContext context = GravityResolver.compute(entity, channel);
        if (context.applicable()) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, context.offset(), 0));
        }
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

    public static boolean isEndOrSable(Level level) {
        if (level.dimension() == Level.END) return true;
        return level.dimension().location().getNamespace().equals("sable");
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFallDamage(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        boolean isPlayer = entity instanceof Player;

        Level level = entity.level();

        // Custom dimension fall damage
        String dimId = level.dimension().location().toString();
        Config.getCustomDimConfig(dimId).ifPresent(config -> {
            if (config.isEnableAtmosphere() && 
                EndlessGravityAPI.getRealY(entity) > config.getAtmosphereMaxAltitude()) {
                event.setDamageMultiplier(0.0F);
                event.setDistance(0.0F);
                return;
            }
        });

        // Vanilla fall damage
        if (!EndlessGravityAPI.isCosmonauticsInstalled()
                && cacheEnableAtmosphere
                && EndlessGravityAPI.isOverworldOrSable(level)
                && EndlessGravityAPI.getRealY(entity) > cacheNoFallDamageAltitude) {
            event.setDamageMultiplier(0.0F);
            event.setDistance(0.0F);
            return;
        }

        if (!isEndOrSable(level)) return;
        int mode = isPlayer ? cacheFallDamageMode : cacheMobFallDamageMode;
        if (mode == 1) {
            event.setDamageMultiplier(0.0F);
            event.setDistance(0.0F);
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
        cacheMobFallDamageMode = Config.COMMON.mobFallDamageMode.get();
        cacheEnableSableEndGravity = Config.COMMON.endSableGravity.get();
        cacheSableGravityY = Config.COMMON.sableGravityY.get();
        cacheSablePressure = Config.COMMON.sablePressure.get();
        cacheSableDrag = Config.COMMON.sableDrag.get();
        cacheSableDatapackPriority = Config.COMMON.sableDatapackPriority.get();
        cacheOverworldSableGravity = Config.COMMON.overworldSableGravity.get();
        cacheOverworldSableDatapackPriority = Config.COMMON.overworldSableDatapackPriority.get();
        cacheParticleGravityOffset = Config.COMMON.particleGravityOffset.get();
        cacheEnableMobGravity = Config.COMMON.enableMobGravity.get();
        cacheMobGravityOffset = Config.COMMON.mobGravityOffset.get();
    }

    // Public getters for mixins
    public static boolean isEndEntityGravity() { return cacheEndEntityGravity; }
    public static boolean isEnablePlayerGravity() { return cacheEnablePlayerGravity; }
    public static double getPlayerGravityOffset() { return cachePlayerGravityOffset; }
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
    public static boolean isEnableSableEndGravity() { return cacheEnableSableEndGravity; }
    public static double getSableGravityY() { return cacheSableGravityY; }
    public static double getSablePressure() { return cacheSablePressure; }
    public static double getSableDrag() { return cacheSableDrag; }
    public static int getSableDatapackPriority() { return cacheSableDatapackPriority; }
    public static boolean isOverworldSableGravity() { return cacheOverworldSableGravity; }
    public static int getOverworldSableDatapackPriority() { return cacheOverworldSableDatapackPriority; }
    public static double getParticleGravityOffset() { return cacheParticleGravityOffset; }
    public static boolean isEnableMobGravity() { return cacheEnableMobGravity; }
    public static double getMobGravityOffset() { return cacheMobGravityOffset; }
}