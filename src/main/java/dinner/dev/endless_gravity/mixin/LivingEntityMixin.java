package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityHandler;
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

        if (entity.onGround() || entity.isInWater() || entity.isFallFlying()) return;

        if (EndlessGravityAPI.isGravityImmune(entity)) return;

        String dimId = level.dimension().location().toString();
        if (Config.hasCustomGravity(dimId)) {
            GravityHandler.applyCustomDimensionGravity(entity, level);
            return;
        }

        if (GravityHandler.applyAtmosphereEffects(level, entity)) return;

        if (!GravityHandler.isEnd(level)) return;
        if (!GravityHandler.isEndEntityGravity()) return;
        if (!GravityHandler.isEnableMobGravity()) return;

        double offset = GravityHandler.getMobGravityOffset();
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, offset, 0));
    }
}
