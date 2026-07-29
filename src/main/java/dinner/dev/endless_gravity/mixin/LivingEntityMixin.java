package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.DragHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "travel", at = @At("TAIL"))
    private void eg$fixDrag(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!DragHelper.shouldCompensate(self)) return;
        if (!self.isControlledByLocalInstance()) return;
        if (self.shouldDiscardFriction()) return;
        if (self.isInWater() || self.isInLava() || self.isFallFlying()) return;

        double progress = DragHelper.getProgress(self);
        if (progress <= 0) return;

        Vec3 motion = self.getDeltaMovement();
        double hFactor = (0.91 + 0.09 * progress) / 0.91;
        double vFactor = self instanceof FlyingAnimal
            ? hFactor
            : (0.98 + 0.02 * progress) / 0.98;
        self.setDeltaMovement(motion.x * hFactor, motion.y * vFactor, motion.z * hFactor);
    }
}
