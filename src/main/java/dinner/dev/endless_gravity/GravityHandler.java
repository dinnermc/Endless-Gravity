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
        if (!Config.COMMON.enablePlayerGravity.get()) return;

        Player player = event.getEntity();
        Level level = player.level();

        if (level.dimension() != Level.END) return;
        if (player.onGround() || player.isInWater() || player.isFallFlying()) return;
        if (player.getAbilities().flying) return;

        double offset = Config.COMMON.playerGravityOffset.get();
        player.setDeltaMovement(
                player.getDeltaMovement().add(0, offset, 0)
        );
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player) return;

        Level level = entity.level();
        if (level.dimension() != Level.END) return;
        if (entity.onGround() || entity.isInWater()) return;

        double velY = entity.getDeltaMovement().y;
        if (Math.abs(velY) < VEL_THRESHOLD) return;

        if (entity instanceof ItemEntity item) {
            if (!Config.COMMON.enableItemGravity.get()) return;
            double offset = Config.COMMON.itemGravityOffset.get();
            item.setDeltaMovement(
                    item.getDeltaMovement().add(0, offset, 0)
            );
        } else if (entity instanceof Projectile projectile) {
            double offset;
            if (projectile instanceof AbstractArrow) {
                if (!Config.COMMON.enableArrowGravity.get()) return;
                offset = Config.COMMON.arrowGravityOffset.get();
            } else {
                if (!Config.COMMON.enableThrownGravity.get()) return;
                offset = Config.COMMON.thrownGravityOffset.get();
            }
            projectile.setDeltaMovement(
                    projectile.getDeltaMovement().add(0, offset, 0)
            );
        } else if (entity instanceof FallingBlockEntity block) {
            if (!Config.COMMON.enableBlockGravity.get()) return;
            double offset = Config.COMMON.blockGravityOffset.get();
            block.setDeltaMovement(
                    block.getDeltaMovement().add(0, offset, 0)
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFallDamage(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().dimension() != Level.END) return;

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
