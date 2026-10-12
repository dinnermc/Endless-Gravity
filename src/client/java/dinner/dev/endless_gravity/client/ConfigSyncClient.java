package dinner.dev.endless_gravity.client;

import dinner.dev.endless_gravity.EndlessGravity;
import dinner.dev.endless_gravity.network.ConfigSyncPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Client-side handling of the server's config sync.
 *
 * <p>The server's values are applied in memory for the session so that gameplay
 * matches the server. The player's own on-disk config is snapshotted before the
 * first sync and restored on disconnect, and the synced writes never trigger a
 * config save. In singleplayer the client and the integrated server share the
 * same config object, so the sync is skipped entirely.
 */
@EventBusSubscriber(modid = EndlessGravity.MODID, value = Dist.CLIENT)
public final class ConfigSyncClient {

    private static ConfigSyncPayload localBackup;

    private ConfigSyncClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Route the payload's client-side application through here (see ConfigSyncPayload#handle).
        ConfigSyncPayload.clientApplier = ConfigSyncClient::apply;
    }

    public static void apply(ConfigSyncPayload payload) {
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            return;
        }
        if (localBackup == null) {
            localBackup = ConfigSyncPayload.fromConfig();
        }
        payload.applyToConfig();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (localBackup != null) {
            localBackup.applyToConfig();
            localBackup = null;
        }
    }
}
