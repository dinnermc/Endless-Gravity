package dinner.dev.endless_gravity.client;

import dinner.dev.endless_gravity.EndlessGravity;
import dinner.dev.endless_gravity.network.ConfigSyncPayload;
import dinner.dev.endless_gravity.particle.LowGravityParticle;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

public class EndlessGravityClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        GravityHandlerClient.register();

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.ID, (payload, context) ->
                context.client().execute(payload::applyToConfig));

        net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.getInstance().register(EndlessGravity.LOW_GRAVITY_PARTICLE, LowGravityParticle.Provider::new);
    }
}