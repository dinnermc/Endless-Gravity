package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.DragHelper;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractArrow.class)
public class AbstractArrowMixin {
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal = 0), index = 0)
    private double eg$modifyArrowDrag(double factor) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (!DragHelper.shouldCompensate(self)) return factor;
        double p = DragHelper.getProgress(self);
        return factor + (1.0 - factor) * p;
    }
}
