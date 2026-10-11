package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void endlessgravity$applyGravity(CallbackInfo ci) {
        AbstractArrow entity = (AbstractArrow) (Object) this;
        Level level = entity.level();
        if (level == null || !level.isClientSide()) return;
        if (entity.onGround() || entity.isInWater()) return;

        // Múltiples checks para detectar flecha "clavada/estática":
        // 1. shakeTime > 0 = flecha golpeó bloque recientemente (vanilla la usa para animación de vibración)
        boolean hitSomething = entity.shakeTime > 0;
        
        // 2. Velocidad casi nula
        Vec3 vel = entity.getDeltaMovement();
        boolean verySlow = Math.abs(vel.x) < 0.01 && Math.abs(vel.y) < 0.01 && Math.abs(vel.z) < 0.01;
        
        // 3. Colisionando con bloque sólido en su posición
        BlockPos pos = BlockPos.containing(entity.position());
        boolean colliding = !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        
        // 4. Lleva un tiempo en el mundo sin moverse (tick count)
        boolean oldEnough = entity.tickCount > 20;

        // Si CUALQUIERA de estas condiciones sugiere que está clavada, no aplicar gravedad
        if ((hitSomething || (verySlow && colliding)) && oldEnough) {
            return;
        }

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