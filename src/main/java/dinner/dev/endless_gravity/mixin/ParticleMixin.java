package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public abstract class ParticleMixin {
    @Shadow @Final protected ClientLevel level;
    @Shadow protected double y;
    @Shadow protected double yd;

    @Inject(method = "tick", at = @At("HEAD"))
    private void endless_gravity$reduceGravity(CallbackInfo ci) {
        double offset = 0.0;
        if (EndlessGravityAPI.isEnd(this.level)) {
            if (!GravityHandler.isEndEntityGravity()) return;
            offset = GravityHandler.getParticleGravityOffset();
        } else {
            if (!GravityHandler.isOverworld(this.level)) return;
            if (!GravityHandler.isOverworldEntityGravity()) return;
            if (!GravityHandler.isEnableAtmosphere()) return;
            if (this.y <= EndlessGravityAPI.BASE) return;

            offset = EndlessGravityAPI.getAtmosphereOffset(this.y);
        }
        if (offset > 0.0) {
            // Apply a subtle fraction of the offset for a slow-motion effect
            // without reversing the particle's direction.
            this.yd += offset * 0.3;
        }
    }
}
