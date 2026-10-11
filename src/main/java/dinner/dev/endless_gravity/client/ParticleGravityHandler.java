package dinner.dev.endless_gravity.client;

import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Queue;

public class ParticleGravityHandler {

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

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // We use a static method to avoid creating instances
            onClientTick(client);
        });
    }

    @SuppressWarnings("unchecked")
    private static void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (!EndlessGravityAPI.isEnd(client.level)) return;
        if (!GravityHandler.isEndEntityGravity()) return;
        if (client.player.tickCount % 4 != 0) return;

        resolveFields();
        if (particleGravityField == null || particlesMapField == null) return;

        try {
            Map<ParticleRenderType, Queue<Particle>> map =
                    (Map<ParticleRenderType, Queue<Particle>>) particlesMapField.get(client.particleEngine);

            if (map == null) return;

            double offset = GravityHandler.getParticleGravityOffset();

            for (Queue<Particle> queue : map.values()) {
                for (Particle particle : queue) {
                    float g = particleGravityField.getFloat(particle);
                    // Only adjust if gravity is positive and non-negligible to avoid affecting particles that already have zero or negative gravity
                    if (g > 0.001f) {
                        float newGravity = (float) (g - offset);
                        particleGravityField.setFloat(particle, newGravity);
                    }
                }
            }
        } catch (Exception e) {
            // silent
        }
    }
}