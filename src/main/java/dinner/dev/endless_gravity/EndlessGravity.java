package dinner.dev.endless_gravity;

import dinner.dev.endless_gravity.compat.sable.AtmosphereForceGroups;
import dinner.dev.endless_gravity.network.ConfigSyncPayload;
import dinner.dev.endless_gravity.particle.LowGravityParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(EndlessGravity.MODID)
public class EndlessGravity {
    public static final String MODID = "endless_gravity";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final net.minecraft.core.particles.SimpleParticleType LOW_GRAVITY_PARTICLE = new net.minecraft.core.particles.SimpleParticleType(true);

    public EndlessGravity(net.neoforged.bus.api.IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC);
        if (ModList.get().isLoaded("sable")) {
            AtmosphereForceGroups.init(modEventBus);
        }
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToClient(
                    ConfigSyncPayload.ID,
                    ConfigSyncPayload.STREAM_CODEC,
                    ConfigSyncPayload::handle
            );
        });

        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, ConfigSyncPayload.fromConfig());
            }
        });

        // Register particle type
        net.neoforged.neoforge.registries.DeferredRegister<ParticleType<?>> particles = DeferredRegister.create(net.minecraft.core.registries.Registries.PARTICLE_TYPE, MODID);
        particles.register("low_gravity_particle", () -> LOW_GRAVITY_PARTICLE);
        particles.register(modEventBus);

        // Register particle factory on client
        modEventBus.addListener(event -> {
            net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event1 = (RegisterParticleProvidersEvent) event;
            event1.registerSpriteSet(LOW_GRAVITY_PARTICLE, LowGravityParticle.Provider::new);
        });

        LOGGER.info("Endless Gravity loaded");
    }
}