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

        // When Sable manages this dimension, its physics engine handles gravity
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

        // Apply atmosphere gravity BEFORE velocity threshold check,
        // so entities with purely horizontal motion still get reduced gravity.
        if (applyAtmosphereEffects(level, entity)) return;

        // When Sable manages this dimension, its physics engine handles gravity
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

    /**
     * Applies atmosphere-based gravity and drag for the Overworld and Sable sub-levels.
     * Returns true if effects were applied.
     */
    private static boolean applyAtmosphereEffects(Level level, Entity entity) {
        if (!EndlessGravityAPI.isOverworldOrSable(level)) {
            return false;
        }
        if (!Config.COMMON.overworldEntityGravity.get()) {
            return false;
        }
        if (!Config.COMMON.enableAtmosphere.get()) {
            return false;
        }

        // For Sable sub-levels, gravity reduction is handled by the ForceGroup in ServerSubLevelMixin.
        // Don't apply additional upward force here to avoid double-applying.
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

        // Light entities (items, projectiles) have weaker gravity than players:
        // cap the upward force at their own gravity so thin air makes them
        // weightless instead of accelerating them upward.
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

        if (EndlessGravityAPI.isOverworldOrSable(level) && Config.COMMON.enableAtmosphere.get()) {
            double realY = EndlessGravityAPI.getRealY(player);
            if (realY > EndlessGravityAPI.BASE) {
                double offset = EndlessGravityAPI.getAtmosphereOffset(realY);
                if (offset > 0) {
                    handleFallDamage(event, player);
                    return;
                }
            }
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
