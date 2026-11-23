package dinner.dev.endless_gravity.network;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Pushes the current config snapshot to every connected client. */
public final class ConfigSync {

    private ConfigSync() {}

    public static void broadcast(MinecraftServer server) {
        if (server == null) return;
        PacketDistributor.sendToAllPlayers(ConfigSyncPayload.fromServer(server));
    }
}
