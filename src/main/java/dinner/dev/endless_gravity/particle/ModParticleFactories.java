package dinner.dev.endless_gravity.particle;

import dinner.dev.endless_gravity.EndlessGravity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = EndlessGravity.MODID, value = Dist.CLIENT)
public class ModParticleFactories {

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.EXHAUST_SMOKE.get(), ModParticleFactories::createExhaust);
        event.registerSpriteSet(ModParticles.TAIL_SMOKE.get(), ModParticleFactories::createTailSmoke);
    }

    private static ParticleProvider<SimpleParticleType> createExhaust(SpriteSet sprites) {
        return (SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) ->
                new ExhaustParticle(level, x, y, z, vx, vy, vz, sprites);
    }

    private static ParticleProvider<SimpleParticleType> createTailSmoke(SpriteSet sprites) {
        return (SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) ->
                new TailSmokeParticle(level, x, y, z, vx, vy, vz, sprites);
    }
}
