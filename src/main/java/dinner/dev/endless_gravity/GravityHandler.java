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

@EventBusSubscriber(modid = EndlessGravity.MODID)
public class GravityHandler {

    private static final double VEL_THRESHOLD = 0.005;

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
        if (!Config.COMMON.endEntityGravity.get()) return;
        if (!Config.COMMON.enablePlayerGravity.get()) return;

        double offset = Config.COMMON.playerGravityOffset.get();
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
        if (!Config.COMMON.endEntityGravity.get()) return;

        double offset;
        if (entity instanceof ItemEntity) {
            if (!Config.COMMON.enableItemGravity.get()) return;
            offset = Config.COMMON.itemGravityOffset.get();
        } else if (entity instanceof Projectile projectile) {
            if (projectile instanceof AbstractArrow) {
                if (!Config.COMMON.enableArrowGravity.get()) return;
                offset = Config.COMMON.arrowGravityOffset.get();
            } else {
                if (!Config.COMMON.enableThrownGravity.get()) return;
                offset = Config.COMMON.thrownGravityOffset.get();
            }
        } else if (entity instanceof FallingBlockEntity) {
            if (!Config.COMMON.enableBlockGravity.get()) return;
            offset = Config.COMMON.blockGravityOffset.get();
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
        if (!Config.COMMON.overworldEntityGravity.get()) {
            return false;
        }
        if (!Config.COMMON.enableAtmosphere.get()) {
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

        // Cap the lift at the entity's own gravity or items would fly up.
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

        // No fall damage above the configurable altitude; the air is too thin to hurt.
        if (!EndlessGravityAPI.isCosmonauticsInstalled()
                && Config.COMMON.enableAtmosphere.get()
                && EndlessGravityAPI.isOverworldOrSable(level)
                && EndlessGravityAPI.getRealY(player) > Config.COMMON.noFallDamageAltitude.get()) {
            event.setDamageMultiplier(0.0F);
            event.setDistance(0.0F);
            return;
        }

        if (!isEndOrSable(level)) return;
        handleFallDamage(event, player);
    }

    private static void handleFallDamage(LivingFallEvent event, ServerPlayer player) {
        int mode = Config.COMMON.fallDamageMode.get();

        if (mode == 1) {
            event.setDamageMultiplier(0.0F);
            event.setDistance(0.0F);
        } else if (mode == 2) {
            double velY = Math.abs(player.getDeltaMovement().y);
            double minVel = Config.COMMON.fallDamageMinVelocity.get();
            if (velY < minVel) {
                event.setCanceled(true);
                return;
            }
            double scale = Config.COMMON.fallDamageVelocityScale.get();
            float velocityDamage = (float) (velY * scale);

            event.setDamageMultiplier(1.0F);
            event.setDistance(velocityDamage + 3.0F);
        }
    }
}
