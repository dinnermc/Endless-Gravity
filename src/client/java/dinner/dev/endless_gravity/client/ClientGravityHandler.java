package dinner.dev.endless_gravity.client;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravity;
import dinner.dev.endless_gravity.EndlessGravityClient;
import dinner.dev.endless_gravity.GravityResolver;
import dinner.dev.endless_gravity.api.GravityChannel;
import dinner.dev.endless_gravity.api.GravityContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-side player gravity, mirroring the server-side resolution.
 *
 * <p>Player movement is client-authoritative, so the reduced gravity has to be
 * applied on the client for the local player's own prediction to match the
 * server. Without this the server moves the player up while the client predicts
 * vanilla gravity, and the resulting corrections show up as jitter. The server
 * still applies the same gravity authoritatively (see {@link dinner.dev.endless_gravity.GravityHandler}).
 */
@EventBusSubscriber(modid = EndlessGravity.MODID, value = Dist.CLIENT)
public final class ClientGravityHandler {

    private ClientGravityHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();

        while (EndlessGravityClient.TOGGLE_HUD_KEY.consumeClick()) {
            boolean next = !Config.COMMON.enableGravityHud.get();
            Config.COMMON.enableGravityHud.set(next);
            ConfigSyncClient.persistClientOnlyChanges();
            if (client.player != null) {
                client.player.displayClientMessage(
                        Component.translatable("endless_gravity.hud.gravity")
                                .append(Component.literal(": "))
                                .append(Component.translatable("endless_gravity.config." + next)),
                        true);
            }
        }

        // The client tick events keep firing while the integrated server is paused, but the
        // player does not tick, so applying gravity here would accumulate velocity.
        if (client.isPaused()) return;

        ClientLevel level = client.level;
        if (level == null) return;

        Player player = client.player;
        if (player == null) return;

        GravityHud.tick(client);

        if (!GravityResolver.canApply(player, GravityChannel.PLAYER)) return;

        GravityContext context = GravityResolver.compute(player, GravityChannel.PLAYER);
        if (context.applicable()) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, context.offset(), 0));
        }
        GravityResolver.applyAtmosphericDrag(player);
    }
}
