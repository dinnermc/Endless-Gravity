package dinner.dev.endless_gravity.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityHandler;

@Environment(EnvType.CLIENT)
public class GravityHandlerClient {

    private static final AABB WORLD_BOUNDS = new AABB(-30000000, -2048, -30000000, 30000000, 2048, 30000000);

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var level = client.level;
            if (level == null) return;

            var player = client.player;
            if (player != null) {
                applyPlayerGravity(player, level);
            }

            for (Entity entity : level.getEntitiesOfClass(Entity.class, WORLD_BOUNDS)) {
                if (!(entity instanceof Player)) {
                    applyEntityGravity(entity, level);
                }
            }
        });
    }

    private static void applyPlayerGravity(Player player, net.minecraft.client.multiplayer.ClientLevel level) {
        if (player.onGround() || player.isInWater() || player.isFallFlying()) return;
        if (player.getAbilities().flying) return;

        if (EndlessGravityAPI.isGravityImmune(player)) return;

        if (applyAtmosphereEffects(level, player)) return;

        if (!GravityHandler.isEnd(level)) return;
        if (!Config.COMMON.endEntityGravity.get()) return;
        if (!Config.COMMON.enablePlayerGravity.get()) return;

        applyGravity(player, Config.COMMON.playerGravityOffset.get());
    }

    private static void applyEntityGravity(Entity entity, net.minecraft.client.multiplayer.ClientLevel level) {
        if (entity.onGround() || entity.isInWater()) return;

        if (EndlessGravityAPI.isGravityImmune(entity)) return;

        if (applyAtmosphereEffects(level, entity)) return;

        if (!GravityHandler.isEnd(level)) return;
        if (!Config.COMMON.endEntityGravity.get()) return;

        double offset;
        if (entity instanceof ItemEntity) {
            if (!Config.COMMON.enableItemGravity.get()) return;
            offset = Config.COMMON.itemGravityOffset.get();
        } else {
            return;
        }

        applyGravity(entity, offset);
    }

    private static boolean applyAtmosphereEffects(net.minecraft.client.multiplayer.ClientLevel level, Entity entity) {
        if (!GravityHandler.isOverworld(level)) {
            return false;
        }
        if (!Config.COMMON.overworldEntityGravity.get()) {
            return false;
        }
        if (!Config.COMMON.enableAtmosphere.get()) {
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

    private static void applyGravity(Entity entity, double offset) {
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, offset, 0));
    }
}