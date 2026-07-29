package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.event.FallDamageCalculationEvent;
import dinner.dev.endless_gravity.event.GravityApplicationEvent;
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
import net.neoforged.neoforge.common.NeoForge;
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

        Double layerOffset = getOverworldLayerOffset(level, player.getY());
        if (layerOffset != null) {
            if (!Config.COMMON.enablePlayerGravity.get()) return;
            applyGravity(player, layerOffset);
            return;
        }

        if (level.dimension() != Level.END) return;
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

        double velY = entity.getDeltaMovement().y;
        if (Math.abs(velY) < VEL_THRESHOLD) return;

        Double layerOffset = getOverworldLayerOffset(level, entity.getY());
        if (layerOffset != null) {
            if (!entityTypeEnabled(entity)) return;
            applyGravity(entity, layerOffset);
            return;
        }

        if (level.dimension() != Level.END) return;

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

    private static Double getOverworldLayerOffset(Level level, double y) {
        if (level.dimension() != Level.OVERWORLD) return null;
        if (!Config.COMMON.enableOverworldGravity.get()) return null;

        int startY = Config.COMMON.overworldGravityStartY.get();
        if (y < startY) return null;

        int layerHeight = Config.COMMON.overworldGravityLayerHeight.get();
        int maxLayers = Config.COMMON.overworldGravityMaxLayers.get();
        double perLayer = Config.COMMON.overworldGravityPerLayer.get();

        int layer = Math.min(maxLayers, (int) ((y - startY) / layerHeight) + 1);
        return Math.min(layer * perLayer, 0.08);
    }

    private static boolean entityTypeEnabled(Entity entity) {
        if (entity instanceof ItemEntity) return Config.COMMON.enableItemGravity.get();
        if (entity instanceof AbstractArrow) return Config.COMMON.enableArrowGravity.get();
        if (entity instanceof Projectile) return Config.COMMON.enableThrownGravity.get();
        if (entity instanceof FallingBlockEntity) return Config.COMMON.enableBlockGravity.get();
        return false;
    }

    private static void applyGravity(Entity entity, double offset) {
        GravityApplicationEvent gravityEvent = new GravityApplicationEvent(entity, offset);
        NeoForge.EVENT_BUS.post(gravityEvent);
        if (gravityEvent.isCanceled()) return;

        offset = gravityEvent.getOffset();
        entity.setDeltaMovement(
                entity.getDeltaMovement().add(0, offset, 0)
        );
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFallDamage(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Level level = player.level();

        Double layerOffset = getOverworldLayerOffset(level, player.getY());
        if (layerOffset != null && layerOffset > 0) {
            handleFallDamage(event, player, layerOffset);
            return;
        }

        if (level.dimension() != Level.END) return;
        handleFallDamage(event, player, null);
    }

    private static void handleFallDamage(LivingFallEvent event, ServerPlayer player, Double layerOffset) {
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

            FallDamageCalculationEvent damageEvent = new FallDamageCalculationEvent(player, 1.0F, velocityDamage + 3.0F);
            NeoForge.EVENT_BUS.post(damageEvent);
            if (damageEvent.isCanceled()) {
                event.setCanceled(true);
                return;
            }

            event.setDamageMultiplier(damageEvent.getDamageMultiplier());
            event.setDistance(damageEvent.getDistance());
        }
    }
}
