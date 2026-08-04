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
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = EndlessGravity.MODID, value = Dist.CLIENT)
public class ClientGravityHandler {

    private static final float VANILLA_THRESHOLD = 0.05f;

    private static Field particleGravityField;
    private static Field particlesMapField;
    private static boolean fieldsResolved = false;

    // Overworld/sub-level particles are scaled exactly once at spawn (weak keys, so old particles get GC'd)
    private static final Set<Particle> SCALED = Collections.newSetFromMap(new WeakHashMap<>());

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
        boolean isEnd = mc.level.dimension() == Level.END;
        boolean isOverworld = mc.level.dimension() == Level.OVERWORLD;
        boolean isSableSubLevel = mc.level.dimension().location().getNamespace().equals("sable");
        if (!isEnd && !isOverworld && !isSableSubLevel) return;
        if (!isEnd && !Config.COMMON.enableAtmosphere.get()) return;
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
                        if (isEnd) {
                            particleGravityField.setFloat(particle, g * multiplier);
                        } else {
                            // Scale once per particle at spawn: gravity lerps from vanilla (1.0)
                            // at full pressure down to the configured multiplier at vacuum,
                            // following the atmosphere layers (progress = 1 - pressure).
                            // Sub-level positions are projected out to global Overworld space.
                            if (!SCALED.add(particle)) continue;
                            double globalY = EndlessGravityAPI.getRealY(mc.level, particle.getBoundingBox().getCenter());
                            double progress = AtmosphereLayers.getProgress(globalY);
                            float m = 1.0F + (float) progress * (multiplier - 1.0F);
                            if (m >= 1.0F) continue;
                            particleGravityField.setFloat(particle, g * m);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
