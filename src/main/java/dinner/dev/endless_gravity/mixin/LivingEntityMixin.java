package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.GravityResolver;
import dinner.dev.endless_gravity.api.GravityChannel;
import dinner.dev.endless_gravity.api.GravityContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies reduced gravity to non-player living entities (mobs).
 * Players are handled separately by GravityHandler (server) and GravityHandlerClient (client).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void endlessgravity$applyMobGravity(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Players have their own (side-aware) handling.
        if (entity instanceof Player) return;

        Level level = entity.level();
        if (level == null) return;

        if (!GravityResolver.canApply(entity, GravityChannel.MOB)) return;

        GravityContext context = GravityResolver.compute(entity, GravityChannel.MOB);
        if (context.applicable()) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, context.offset(), 0));
        }
    }
}
