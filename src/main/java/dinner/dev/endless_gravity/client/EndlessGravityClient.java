package dinner.dev.endless_gravity.client;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.network.ConfigSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class EndlessGravityClient implements ClientModInitializer {

    private static ConfigSyncPayload localBackup;

    @Override
    public void onInitializeClient() {
        GravityHandlerClient.register();
        ParticleGravityHandler.register();

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    // Singleplayer shares the config with the integrated server, so the
                    // sync would be a no-op; skipping it also avoids snapshotting there.
                    if (context.client().hasSingleplayerServer()) {
                        return;
                    }
                    // Snapshot the player's own values once, before the first server sync.
                    if (localBackup == null) {
                        localBackup = ConfigSyncPayload.fromConfig();
                    }
                    // Block config writes while the server's values are applied so that
                    // opening the config screen can't persist them over the local file.
                    Config.setServerSyncActive(true);
                    payload.applyToConfig();
                }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (localBackup != null) {
                localBackup.applyToConfig();
                localBackup = null;
            }
            Config.setServerSyncActive(false);
        });
    }
}