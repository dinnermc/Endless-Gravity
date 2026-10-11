package dinner.dev.endless_gravity.client;

import dinner.dev.endless_gravity.network.ConfigSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class EndlessGravityClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) ->
                context.client().execute(payload::applyToConfig));
    }
}