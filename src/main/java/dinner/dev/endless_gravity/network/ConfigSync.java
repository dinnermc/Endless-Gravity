package dinner.dev.endless_gravity.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Pushes the current config snapshot to every connected client. */
public final class ConfigSync {

    private ConfigSync() {}

    public static void broadcast(MinecraftServer server) {
        if (server == null) return;
        ConfigSyncPayload payload = ConfigSyncPayload.fromConfig();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
