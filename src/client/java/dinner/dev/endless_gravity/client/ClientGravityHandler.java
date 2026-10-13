package dinner.dev.endless_gravity.client;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravity;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-side player gravity, mirroring the Fabric {@code GravityHandlerClient}.
 *
 * <p>Player movement is client-authoritative, so the reduced gravity has to be
 * applied on the client for the local player's own prediction to match the
 * server. Without this the server moves the player up while the client predicts
 * vanilla gravity, and the resulting corrections show up as jitter. The server
 * still applies the same gravity authoritatively (see {@link GravityHandler}).
 */
@EventBusSubscriber(modid = EndlessGravity.MODID, value = Dist.CLIENT)
public final class ClientGravityHandler {

    private static boolean cacheEndEntityGravity;
    private static boolean cacheEnablePlayerGravity;
    private static double cachePlayerGravityOffset;
    private static boolean cacheOverworldEntityGravity;
    private static boolean cacheEnableAtmosphere;

    private ClientGravityHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();

        // The client tick events keep firing while the integrated server is paused, but the
        // player does not tick, so applying gravity here would accumulate velocity.
        if (client.isPaused()) return;

        refreshConfigCache();

        ClientLevel level = client.level;
        if (level == null) return;

        var player = client.player;
        if (player != null) {
            applyPlayerGravity(player, level);
        }
    }

    private static void refreshConfigCache() {
        cacheEndEntityGravity = Config.COMMON.endEntityGravity.get();
        cacheEnablePlayerGravity = Config.COMMON.enablePlayerGravity.get();
        cachePlayerGravityOffset = Config.COMMON.playerGravityOffset.get();
        cacheOverworldEntityGravity = Config.COMMON.overworldEntityGravity.get();
        cacheEnableAtmosphere = Config.COMMON.enableAtmosphere.get();
    }

    private static void applyPlayerGravity(Player player, ClientLevel level) {
        if (player.onGround() || player.isInWater() || player.isFallFlying()) return;
        if (player.getAbilities().flying) return;

        if (EndlessGravityAPI.isGravityImmune(player)) return;

        if (applyAtmosphereEffects(level, player)) return;

        if (!GravityHandler.isEnd(level)) return;
        if (!cacheEndEntityGravity) return;
        if (!cacheEnablePlayerGravity) return;

        applyGravity(player, cachePlayerGravityOffset);
    }

    private static boolean applyAtmosphereEffects(ClientLevel level, Player player) {
        if (!GravityHandler.isOverworld(level)) {
            return false;
        }
        if (!cacheOverworldEntityGravity) {
            return false;
        }
        if (!cacheEnableAtmosphere) {
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
