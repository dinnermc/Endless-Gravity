package dinner.dev.endless_gravity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public final class DragHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private DragHelper() {}

    public static double getProgress(Entity entity) {
        double realY = EndlessGravityAPI.getRealY(entity);
        return EndlessGravityAPI.getDragProgress(realY) * Config.COMMON.atmosphereDrag.get();
    }

    public static boolean shouldCompensate(Entity entity) {
        Level level = entity.level();
        if (!EndlessGravityAPI.isOverworldOrSable(level)) {
            LOGGER.debug("shouldCompensate: false - not Overworld/Sable (dim={})", level.dimension().location());
            return false;
        }
        if (EndlessGravityAPI.isSableManaged(level)) {
            LOGGER.debug("shouldCompensate: false - Sable managed (dim={})", level.dimension().location());
            return false;
        }
        if (!Config.COMMON.enableAtmosphere.get()) {
            LOGGER.debug("shouldCompensate: false - atmosphere disabled");
            return false;
        }
        double realY = EndlessGravityAPI.getRealY(entity);
        boolean result = realY > EndlessGravityAPI.BASE;
        LOGGER.debug("shouldCompensate: {} (entity={}, dim={}, realY={})", result, entity, level.dimension().location(), realY);
        return result;
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
