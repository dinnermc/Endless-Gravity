package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
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
        // Only affect block-breaking particles (TerrainParticle == block dust), never other particle types.
        if (!((Object) this instanceof TerrainParticle)) return;

        double offset = 0.0;
        if (EndlessGravityAPI.isEnd(this.level)) {
            if (!GravityHandler.isEndEntityGravity()) return;
            offset = GravityHandler.getParticleGravityOffset();
        } else {
            // Same cancellations as applyAtmosphereEffects: Sable-managed dimensions stay out.
            // The NeoForge build also bails when Create: Cosmonautics is installed; Fabric has no
            // Cosmonautics integration, so that check is intentionally omitted here.
            if (!GravityHandler.isOverworld(this.level)) return;
            if (!GravityHandler.isOverworldEntityGravity()) return;
            if (!GravityHandler.isEnableAtmosphere()) return;
            if (this.y <= EndlessGravityAPI.BASE) return;

            offset = EndlessGravityAPI.getAtmosphereOffset(this.y);
        }
        if (offset > 0.0) {
            // Add a fraction of the offset to vertical velocity each tick for a slow-motion fall.
            this.yd += offset * 0.3;
        }
    }
}
