package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.DragHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
    private Vec3 eg$modifyItemDrag(Vec3 instance, double x, double y, double z) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!DragHelper.shouldCompensate(self)) return instance.multiply(x, y, z);
        double p = DragHelper.getProgress(self);
        return instance.multiply(x + (1.0 - x) * p, y + (1.0 - y) * p, z + (1.0 - z) * p);
    }
}
