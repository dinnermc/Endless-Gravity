package dinner.dev.endless_gravity.client;

import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityHandler;
import net.minecraft.world.level.Level;

public final class ParticleGravity {
    private static final double VANILLA_ENTITY_GRAVITY = 0.08;

    private ParticleGravity() {}

    /** 0.0 = gravedad normal, 1.0 = sin gravedad. */
    public static double factor(Level level, double y) {
        if (EndlessGravityAPI.isEnd(level)) {
            if (!GravityHandler.isEndEntityGravity()) return 0.0;
            return clamp(GravityHandler.getParticleGravityOffset() / VANILLA_ENTITY_GRAVITY);
        }

        // Misma lógica que applyAtmosphereEffects: Sable-managed y Cosmonautics quedan fuera
        if (!GravityHandler.isOverworld(level)) return 0.0;
        if (EndlessGravityAPI.isCosmonauticsInstalled()) return 0.0;
        if (!GravityHandler.isOverworldEntityGravity()) return 0.0;
        if (!GravityHandler.isEnableAtmosphere()) return 0.0;
        if (y <= EndlessGravityAPI.BASE) return 0.0;

        return clamp(EndlessGravityAPI.getAtmosphereOffset(y) / VANILLA_ENTITY_GRAVITY);
    }

    private static double clamp(double v) {
        return v < 0.0 ? 0.0 : Math.min(v, 1.0);
    }
}