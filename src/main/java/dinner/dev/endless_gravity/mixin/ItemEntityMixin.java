package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.GravityResolver;
import dinner.dev.endless_gravity.api.GravityChannel;
import dinner.dev.endless_gravity.api.GravityContext;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void endlessgravity$applyGravity(CallbackInfo ci) {
        ItemEntity entity = (ItemEntity) (Object) this;
        Level level = entity.level();
        if (level == null) return;

        if (!GravityResolver.canApply(entity, GravityChannel.ITEM)) return;

        GravityContext context = GravityResolver.compute(entity, GravityChannel.ITEM);
        if (context.applicable()) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, context.offset(), 0));
        }
    }
}
