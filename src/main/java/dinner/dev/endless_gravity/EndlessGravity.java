package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.client.EndlessGravityClient;
import dinner.dev.endless_gravity.network.ConfigSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class EndlessGravity implements ModInitializer {
    public static final String MODID = "endless_gravity";
    public static final Logger LOGGER = LogUtils.getLogger();

    
    @Override
    public void onInitialize() {
        Config.load();
        GravityHandler.register();

        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.ID, ConfigSyncPayload.STREAM_CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (player != null) {
                ServerPlayNetworking.send(player, ConfigSyncPayload.fromConfig());
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> Config.saveIfDirty());

        LOGGER.info("Endless Gravity loaded");
    }
}