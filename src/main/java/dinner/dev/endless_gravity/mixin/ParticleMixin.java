package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.CustomDimConfig;
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
    @Shadow protected float gravity;

    @Inject(method = "tick", at = @At("HEAD"))
    private void endless_gravity$reduceGravity(CallbackInfo ci) {
        // Only affect block-breaking particles (TerrainParticle == block dust), never other particle types.
        if (!((Object) this instanceof TerrainParticle)) return;

        double offset = 0.0;
        if (EndlessGravityAPI.isEnd(this.level)) {
            if (!GravityHandler.isEndEntityGravity()) return;
            offset = GravityHandler.getParticleGravityOffset();
        } else {
            // Particles retain the built-in Overworld atmosphere behavior only.
            if (!GravityHandler.isOverworld(this.level)) return;
            if (EndlessGravityAPI.isCosmonauticsInstalled()) return;
            CustomDimConfig profile = Config.getCustomDimConfig(this.level.dimension().location().toString()).orElse(null);
            if (profile == null || profile.getMode() != CustomDimConfig.Mode.ATMOSPHERE) return;
            offset = profile.getAtmosphereGravityOffset(this.y);
        }
        if (offset > 0.0) {
            // Apply a subtle fraction of the offset for slow motion effect
            // This makes particles move slowly without reversing direction
            this.yd += offset * 0.3;
        }
    }
}