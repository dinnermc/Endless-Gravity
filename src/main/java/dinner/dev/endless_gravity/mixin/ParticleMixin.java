package dinner.dev.endless_gravity.mixin;

import dinner.dev.endless_gravity.Config;
import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityGameRules;
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
        if (!GravityGameRules.enabled(this.level)) return;

        double offset = 0.0;
        if (EndlessGravityAPI.isEnd(this.level)) {
            if (!Config.COMMON.endEntityGravity.get()) return;
            offset = Config.COMMON.particleGravityOffset.get();
        } else {
            Config.CustomDimConfig profile = Config.getCustomDimConfig(this.level.dimension().location()).orElse(null);
            if (profile == null) return;
            if (profile.isEnableAtmosphere()) {
                if (GravityHandler.isOverworld(this.level)
                        && (!Config.COMMON.overworldEntityGravity.get() || !Config.COMMON.enableAtmosphere.get())) {
                    return;
                }
                offset = profile.getAtmosphereGravityOffset(this.y);
            } else {
                offset = profile.getBlockGravityOffset() != 0.0
                        ? profile.getBlockGravityOffset()
                        : profile.getPlayerGravityOffset();
            }
        }
        if (offset > 0.0) {
            // Add a fraction of the offset to vertical velocity each tick for a slow-motion fall.
            this.yd += offset * 0.3;
        }
    }
}
