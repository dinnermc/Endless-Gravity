package dinner.dev.endless_gravity.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityHandler;

@Environment(EnvType.CLIENT)
public class GravityHandlerClient {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var level = client.level;
            if (level == null) return;

            var player = client.player;
            if (player != null) {
                applyPlayerGravity(player, level);
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

    private static boolean applyAtmosphereEffects(net.minecraft.client.multiplayer.ClientLevel level, Player player) {
        if (!GravityHandler.isOverworld(level)) {
            return false;
        }
        if (!Config.COMMON.overworldEntityGravity.get()) {
            return false;
        }
        if (!Config.COMMON.enableAtmosphere.get()) {
            return false;
        }

        double realY = EndlessGravityAPI.getRealY(player);
        if (realY <= EndlessGravityAPI.BASE) {
            return false;
        }

        double offset = EndlessGravityAPI.getAtmosphereOffset(realY);
        if (offset <= 0) {
            return false;
        }

        double entityGravity = player.getGravity();
        if (offset > entityGravity) {
            offset = entityGravity;
        }
        if (offset <= 0) {
            return false;
        }

        applyGravity(player, offset);
        return true;
    }

    private static void applyGravity(Player player, double offset) {
        player.setDeltaMovement(player.getDeltaMovement().add(0, offset, 0));
    }
}