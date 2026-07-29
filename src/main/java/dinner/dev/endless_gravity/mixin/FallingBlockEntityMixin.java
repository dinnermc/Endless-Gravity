package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.DragHelper;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"), index = 0)
    private double eg$modifyFallingBlockDrag(double factor) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (!DragHelper.shouldCompensate(self)) return factor;
        double p = DragHelper.getProgress(self);
        return factor + (1.0 - factor) * p;
    }
}
