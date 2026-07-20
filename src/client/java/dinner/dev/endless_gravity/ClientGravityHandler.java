package dinner.dev.endless_gravity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Queue;

@EventBusSubscriber(modid = EndlessGravity.MODID, value = Dist.CLIENT)
public class ClientGravityHandler {

    private static final float VANILLA_THRESHOLD = 0.05f;

    private static Field particleGravityField;
    private static Field particlesMapField;
    private static boolean fieldsResolved = false;

    private static void resolveFields() {
        if (fieldsResolved) return;
        try {
            particleGravityField = Particle.class.getDeclaredField("gravity");
            particleGravityField.setAccessible(true);

            particlesMapField = ParticleEngine.class.getDeclaredField("particles");
            particlesMapField.setAccessible(true);

            fieldsResolved = true;
        } catch (Exception e) {
            fieldsResolved = true;
        }
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Config.COMMON.enableParticles.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.level.dimension() != Level.END) return;
        if (mc.player.tickCount % 4 != 0) return;

        resolveFields();
        if (particleGravityField == null || particlesMapField == null) return;

        try {
            Map<?, Queue<Particle>> map =
                    (Map<?, Queue<Particle>>) particlesMapField.get(mc.particleEngine);

            if (map == null) return;

            float multiplier = Config.COMMON.particleGravityMultiplier.get().floatValue();

            for (Queue<Particle> queue : map.values()) {
                for (Particle particle : queue) {
                    float g = particleGravityField.getFloat(particle);
                    if (g > VANILLA_THRESHOLD) {
                        particleGravityField.setFloat(particle, g * multiplier);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
