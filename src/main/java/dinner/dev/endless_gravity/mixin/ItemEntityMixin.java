package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityHandler;
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

        if (entity.onGround() || entity.isInWater()) return;

        if (EndlessGravityAPI.isGravityImmune(entity)) return;

        if (GravityHandler.applyAtmosphereEffects(level, entity)) return;

        if (!GravityHandler.isEnd(level)) return;
        if (!Config.COMMON.endEntityGravity.get()) return;
        if (!Config.COMMON.enableItemGravity.get()) return;

        double offset = Config.COMMON.itemGravityOffset.get();
        applyGravity(entity, offset);
    }

    private static void applyGravity(ItemEntity entity, double offset) {
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, offset, 0));
    }
}