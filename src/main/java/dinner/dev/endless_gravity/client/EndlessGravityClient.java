package dinner.dev.endless_gravity.client;

import com.mojang.blaze3d.platform.InputConstants;
import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.GravityGameRules;
import dinner.dev.endless_gravity.api.BiomeGravityRegistry;
import dinner.dev.endless_gravity.network.ConfigSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;

public class EndlessGravityClient implements ClientModInitializer {

    public static KeyMapping TOGGLE_HUD_KEY;

    private static ConfigSyncPayload localBackup;
    private static ConfigSyncPayload lastSynced;

    /**
     * Persists only client-local settings while connected to a remote server, by temporarily
     * restoring the player's local gameplay backup, saving, and reapplying the server snapshot.
     */
    public static void persistClientOnlyChanges() {
        if (!Config.isServerSyncActive() || localBackup == null) {
            Config.save();
            return;
        }
        boolean hud = Config.COMMON.enableGravityHud.get();
        int hudPos = Config.COMMON.hudPosition.get();
        boolean hudPressure = Config.COMMON.hudShowPressure.get();
        boolean hudCompact = Config.COMMON.hudCompact.get();
        double particleOffset = Config.COMMON.particleGravityOffset.get();
        boolean lowPass = Config.COMMON.enableLowPassFilter.get();
        double lowPassGain = Config.COMMON.lowPassGain.get();
        double lowPassGainHF = Config.COMMON.lowPassGainHF.get();

        localBackup.applyToConfig();
        Config.COMMON.enableGravityHud.setSynced(hud);
        Config.COMMON.hudPosition.setSynced(hudPos);
        Config.COMMON.hudShowPressure.setSynced(hudPressure);
        Config.COMMON.hudCompact.setSynced(hudCompact);
        Config.COMMON.particleGravityOffset.setSynced(particleOffset);
        Config.COMMON.enableLowPassFilter.setSynced(lowPass);
        Config.COMMON.lowPassGain.setSynced(lowPassGain);
        Config.COMMON.lowPassGainHF.setSynced(lowPassGainHF);
        Config.setServerSyncActive(false);
        try {
            Config.save();
        } finally {
            Config.setServerSyncActive(true);
            if (lastSynced != null) {
                lastSynced.applyToConfig();
                GravityGameRules.setClientSyncedEnabled(lastSynced.gameRuleEnabled());
                BiomeGravityRegistry.setSyncedRules(lastSynced.biomeRules());
            }
        }
    }

    @Override
    public void onInitializeClient() {
        TOGGLE_HUD_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.endless_gravity.toggle_hud",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                "key.categories.endless_gravity"
        ));

        GravityHandlerClient.register();
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> GravityHud.render(guiGraphics));

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    // Singleplayer shares the config with the integrated server, so the
                    // sync would be a no-op; skipping it also avoids snapshotting there.
                    if (context.client().hasSingleplayerServer()) {
                        GravityGameRules.setClientSyncedEnabled(null);
                        return;
                    }
                    // Snapshot the player's own values once, before the first server sync.
                    if (localBackup == null) {
                        localBackup = ConfigSyncPayload.fromConfig();
                    }
                    // Block gameplay config writes while the server's values are applied.
                    Config.setServerSyncActive(true);
                    lastSynced = payload;
                    payload.applyToConfig();
                    GravityGameRules.setClientSyncedEnabled(payload.gameRuleEnabled());
                    BiomeGravityRegistry.setSyncedRules(payload.biomeRules());
                }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            Config.setServerSyncActive(false);
            lastSynced = null;
            GravityGameRules.setClientSyncedEnabled(null);
            BiomeGravityRegistry.clearSyncedRules();
            if (localBackup != null) {
                localBackup.applyToConfig();
                localBackup = null;
            }
        });
    }
}
