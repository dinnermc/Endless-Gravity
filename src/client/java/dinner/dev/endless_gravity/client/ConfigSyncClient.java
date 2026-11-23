package dinner.dev.endless_gravity.client;

import dinner.dev.endless_gravity.EndlessGravity;
import dinner.dev.endless_gravity.GravityGameRules;
import dinner.dev.endless_gravity.api.BiomeGravityRegistry;
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
    private static ConfigSyncPayload lastSynced;
    private static boolean serverSyncActive;

    private ConfigSyncClient() {}

    /**
     * True while a remote server's synced config is applied over the local one. While active,
     * gameplay config edits must not overwrite the player's own file or desync from the server.
     */
    public static boolean isServerSyncActive() {
        return serverSyncActive;
    }

    /**
     * Persists only client-local settings while connected to a remote server, by temporarily
     * restoring the player's local gameplay backup, saving, and reapplying the server snapshot.
     */
    public static void persistClientOnlyChanges() {
        if (!serverSyncActive || localBackup == null) {
            dinner.dev.endless_gravity.Config.COMMON_SPEC.save();
            return;
        }
        // Snapshot the current (possibly edited) client-only values before restoring gameplay backup.
        boolean hud = dinner.dev.endless_gravity.Config.COMMON.enableGravityHud.get();
        int hudPos = dinner.dev.endless_gravity.Config.COMMON.hudPosition.get();
        boolean hudPressure = dinner.dev.endless_gravity.Config.COMMON.hudShowPressure.get();
        boolean hudCompact = dinner.dev.endless_gravity.Config.COMMON.hudCompact.get();
        double particleOffset = dinner.dev.endless_gravity.Config.COMMON.particleGravityOffset.get();
        boolean lowPass = dinner.dev.endless_gravity.Config.COMMON.enableLowPassFilter.get();
        double lowPassGain = dinner.dev.endless_gravity.Config.COMMON.lowPassGain.get();
        double lowPassGainHF = dinner.dev.endless_gravity.Config.COMMON.lowPassGainHF.get();

        localBackup.applyToConfig();
        dinner.dev.endless_gravity.Config.COMMON.enableGravityHud.set(hud);
        dinner.dev.endless_gravity.Config.COMMON.hudPosition.set(hudPos);
        dinner.dev.endless_gravity.Config.COMMON.hudShowPressure.set(hudPressure);
        dinner.dev.endless_gravity.Config.COMMON.hudCompact.set(hudCompact);
        dinner.dev.endless_gravity.Config.COMMON.particleGravityOffset.set(particleOffset);
        dinner.dev.endless_gravity.Config.COMMON.enableLowPassFilter.set(lowPass);
        dinner.dev.endless_gravity.Config.COMMON.lowPassGain.set(lowPassGain);
        dinner.dev.endless_gravity.Config.COMMON.lowPassGainHF.set(lowPassGainHF);
        dinner.dev.endless_gravity.Config.COMMON_SPEC.save();

        if (lastSynced != null) {
            lastSynced.applyToConfig();
            GravityGameRules.setClientSyncedEnabled(lastSynced.gameRuleEnabled());
            BiomeGravityRegistry.setSyncedRules(lastSynced.biomeRules());
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Route the payload's client-side application through here (see ConfigSyncPayload#handle).
        ConfigSyncPayload.clientApplier = ConfigSyncClient::apply;
    }

    public static void apply(ConfigSyncPayload payload) {
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            GravityGameRules.setClientSyncedEnabled(null);
            return;
        }
        if (localBackup == null) {
            localBackup = ConfigSyncPayload.fromConfig();
        }
        serverSyncActive = true;
        lastSynced = payload;
        payload.applyToConfig();
        GravityGameRules.setClientSyncedEnabled(payload.gameRuleEnabled());
        BiomeGravityRegistry.setSyncedRules(payload.biomeRules());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        serverSyncActive = false;
        lastSynced = null;
        GravityGameRules.setClientSyncedEnabled(null);
        BiomeGravityRegistry.clearSyncedRules();
        if (localBackup != null) {
            localBackup.applyToConfig();
            localBackup = null;
        }
    }
}
