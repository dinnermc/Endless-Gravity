package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityHandler;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @Accessor("inGround")
    protected abstract boolean isInGround();

    @Inject(method = "tick", at = @At("TAIL"))
    private void endlessgravity$applyGravity(CallbackInfo ci) {
        AbstractArrow entity = (AbstractArrow) (Object) this;
        Level level = entity.level();
        if (level == null || !level.isClientSide()) return;
        if (entity.onGround() || entity.isInWater() || isInGround()) return;

        if (EndlessGravityAPI.isGravityImmune(entity)) return;

        if (applyAtmosphereEffects(level, entity)) return;

        if (!GravityHandler.isEnd(level)) return;
        if (!Config.COMMON.endEntityGravity.get()) return;
        if (!Config.COMMON.enableArrowGravity.get()) return;

        double offset = Config.COMMON.arrowGravityOffset.get();
        applyGravity(entity, offset);
    }

    private static boolean applyAtmosphereEffects(Level level, AbstractArrow entity) {
        if (!GravityHandler.isOverworld(level)) {
            return false;
        }
        if (!Config.COMMON.overworldEntityGravity.get()) {
            return false;
        }
        if (!Config.COMMON.enableAtmosphere.get()) {
            return false;
        }

        double realY = EndlessGravityAPI.getRealY(entity);
        if (realY <= EndlessGravityAPI.BASE) {
            return false;
        }

        double offset = EndlessGravityAPI.getAtmosphereOffset(realY);
        if (offset <= 0) {
            return false;
        }

        double entityGravity = entity.getGravity();
        if (offset > entityGravity) {
            offset = entityGravity;
        }
        if (offset <= 0) {
            return false;
        }

        applyGravity(entity, offset);
        return true;
    }

    private static void applyGravity(AbstractArrow entity, double offset) {
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, offset, 0));
    }
}