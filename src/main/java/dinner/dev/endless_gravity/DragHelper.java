package dinner.dev.endless_gravity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class DragHelper {
    private DragHelper() {}

    public static double getProgress(Entity entity) {
        double realY = EndlessGravityAPI.getRealY(entity);
        return EndlessGravityAPI.getDragProgress(realY) * Config.COMMON.atmosphereDrag.get();
    }

    public static boolean shouldCompensate(Entity entity) {
        Level level = entity.level();
        if (!EndlessGravityAPI.isOverworldOrSable(level)) return false;
        // Sable handles drag via dimension_physics pressure function
        if (EndlessGravityAPI.isSableManaged(level)) return false;
        if (!Config.COMMON.enableAtmosphere.get()) return false;
        double realY = EndlessGravityAPI.getRealY(entity);
        return realY > EndlessGravityAPI.BASE;
    }

    public static float horizontalFactor(Entity entity) {
        double p = getProgress(entity);
        if (p <= 0) return 1.0f;
        return 0.91f + (float)((1.0 - 0.91) * p);
    }

    public static float verticalFactor(Entity entity) {
        double p = getProgress(entity);
        if (p <= 0) return 1.0f;
        return 0.98f + (float)((1.0 - 0.98) * p);
    }
}
