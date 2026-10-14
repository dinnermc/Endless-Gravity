package dinner.dev.endless_gravity.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.entity.player.Player;
import dinner.dev.endless_gravity.GravityResolver;
import dinner.dev.endless_gravity.api.GravityChannel;
import dinner.dev.endless_gravity.api.GravityContext;

@Environment(EnvType.CLIENT)
public class GravityHandlerClient {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // The client tick events keep firing while the integrated server is paused, but the
            // player does not tick, so applying gravity here would accumulate velocity.
            if (client.isPaused()) return;

            if (client.level == null) return;

            Player player = client.player;
            if (player != null) {
                applyPlayerGravity(player);
            }
        });
    }

    private static void applyPlayerGravity(Player player) {
        if (!GravityResolver.canApply(player, GravityChannel.PLAYER)) return;

        GravityContext context = GravityResolver.compute(player, GravityChannel.PLAYER);
        if (context.applicable()) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, context.offset(), 0));
        }
    }
}
