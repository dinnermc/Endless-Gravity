package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.GravityHandler;
import dinner.dev.endless_gravity.GravityResolver;
import dinner.dev.endless_gravity.api.GravityChannel;
import dinner.dev.endless_gravity.api.GravityContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies reduced gravity to non-player living entities (mobs) on the server,
 * and adjusts fall distance for proportional/disabled fall-damage modes.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void endlessgravity$applyMobGravity(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Players have their own (side-aware) handling.
        if (entity instanceof Player) return;

        Level level = entity.level();
        // Server-authoritative: matches NeoForge and avoids firing modifiers twice in singleplayer.
        if (level == null || level.isClientSide()) return;

        if (!GravityResolver.canApply(entity, GravityChannel.MOB)) return;

        GravityContext context = GravityResolver.compute(entity, GravityChannel.MOB);
        if (context.applicable()) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, context.offset(), 0));
        }
        GravityResolver.applyAtmosphericDrag(entity);
    }

    @ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float endlessgravity$adjustFallDistance(float fallDistance) {
        return GravityHandler.adjustFallDistance((LivingEntity) (Object) this, fallDistance);
    }
}
