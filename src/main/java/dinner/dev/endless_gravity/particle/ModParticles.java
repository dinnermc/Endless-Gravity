package dinner.dev.endless_gravity.particle;

import dinner.dev.endless_gravity.EndlessGravity;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, EndlessGravity.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EXHAUST_SMOKE =
            PARTICLES.register("exhaust_smoke", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TAIL_SMOKE =
            PARTICLES.register("tail_smoke", () -> new SimpleParticleType(true));

    public static void init(IEventBus modEventBus) {
        PARTICLES.register(modEventBus);
    }
}
