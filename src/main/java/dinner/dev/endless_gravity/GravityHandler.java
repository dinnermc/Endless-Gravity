package dinner.dev.endless_gravity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = EndlessGravity.MODID)
public class GravityHandler {

    private static final double VEL_THRESHOLD = 0.005;

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
    private static boolean cacheEnableSableEndGravity;
    private static double cacheSableGravityY;
    private static double cacheSablePressure;
    private static double cacheSableDrag;
    private static int cacheSableDatapackPriority;
    private static boolean cacheOverworldSableGravity;
    private static int cacheOverworldSableDatapackPriority;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        refreshConfigCache();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        Level level = player.level();

        if (player.onGround() || player.isInWater() || player.isFallFlying()) return;
        if (player.getAbilities().flying) return;

        if (EndlessGravityAPI.isGravityImmune(player)) return;

        if (applyAtmosphereEffects(level, player)) return;

        if (EndlessGravityAPI.isSableManaged(level)) return;

        if (!isEndOrSable(level)) return;
        if (!cacheEndEntityGravity) return;
        if (!cacheEnablePlayerGravity) return;

        double offset = cachePlayerGravityOffset;
        applyGravity(player, offset);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player) return;

        Level level = entity.level();
        if (entity.onGround() || entity.isInWater()) return;

        if (EndlessGravityAPI.isGravityImmune(entity)) return;

        if (applyAtmosphereEffects(level, entity)) return;

        if (EndlessGravityAPI.isSableManaged(level)) return;

        double velY = entity.getDeltaMovement().y;
        if (Math.abs(velY) < VEL_THRESHOLD) return;

        if (!isEndOrSable(level)) return;
        if (!cacheEndEntityGravity) return;

        double offset;
        if (entity instanceof ItemEntity) {
            if (!cacheEnableItemGravity) return;
            offset = cacheItemGravityOffset;
        } else if (entity instanceof Projectile projectile) {
            if (projectile instanceof AbstractArrow) {
                if (!cacheEnableArrowGravity) return;
                offset = cacheArrowGravityOffset;
            } else {
                if (!cacheEnableThrownGravity) return;
                offset = cacheThrownGravityOffset;
            }
        } else if (entity instanceof FallingBlockEntity) {
            if (!cacheEnableBlockGravity) return;
            offset = cacheBlockGravityOffset;
        } else {
            return;
        }

        applyGravity(entity, offset);
    }

    private static boolean applyAtmosphereEffects(Level level, Entity entity) {
        if (EndlessGravityAPI.isCosmonauticsInstalled()) {
            return false;
        }
        if (!EndlessGravityAPI.isOverworldOrSable(level)) {
            return false;
        }
        if (!cacheOverworldEntityGravity) {
            return false;
        }
        if (!cacheEnableAtmosphere) {
            return false;
        }

        if (EndlessGravityAPI.isSableManaged(level)) {
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

    private static boolean isEndOrSable(Level level) {
        if (level.dimension() == Level.END) return true;
        return level.dimension().location().getNamespace().equals("sable");
    }

    private static void applyGravity(Entity entity, double offset) {
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, offset, 0));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFallDamage(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Level level = player.level();

        if (!EndlessGravityAPI.isCosmonauticsInstalled()
                && cacheEnableAtmosphere
                && EndlessGravityAPI.isOverworldOrSable(level)
                && EndlessGravityAPI.getRealY(player) > cacheNoFallDamageAltitude) {
            event.setDamageMultiplier(0.0F);
            event.setDistance(0.0F);
            return;
        }

        if (!isEndOrSable(level)) return;
        if (cacheFallDamageMode == 1) {
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
        cacheEnableSableEndGravity = Config.COMMON.endSableGravity.get();
        cacheSableGravityY = Config.COMMON.sableGravityY.get();
        cacheSablePressure = Config.COMMON.sablePressure.get();
        cacheSableDrag = Config.COMMON.sableDrag.get();
        cacheSableDatapackPriority = Config.COMMON.sableDatapackPriority.get();
        cacheOverworldSableGravity = Config.COMMON.overworldSableGravity.get();
        cacheOverworldSableDatapackPriority = Config.COMMON.overworldSableDatapackPriority.get();
    }
}