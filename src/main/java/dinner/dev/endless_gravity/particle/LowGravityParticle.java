package dinner.dev.endless_gravity.particle;

import dinner.dev.endless_gravity.EndlessGravityAPI;
import dinner.dev.endless_gravity.GravityHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LowGravityParticle extends TextureSheetParticle {

    private final double gravityMultiplier;

    public LowGravityParticle(ClientLevel level, double x, double y, double z,
                              double xSpeed, double ySpeed, double zSpeed,
                              net.minecraft.client.particle.SpriteSet sprites, double gravityMultiplier) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.gravityMultiplier = gravityMultiplier;
        this.setSpriteFromAge(sprites);
        this.lifetime = 40 + level.random.nextInt(20);
        this.quadSize = 0.2f + level.random.nextFloat() * 0.15f;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            double gravity = GravityHandler.getParticleGravityOffset() * gravityMultiplier;
            this.xd += 0;
            this.yd += gravity;
            this.zd += 0;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final net.minecraft.client.particle.SpriteSet sprites;

        public Provider(net.minecraft.client.particle.SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, net.minecraft.client.multiplayer.ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            double multiplier = EndlessGravityAPI.getAtmosphereProgress((int) y);
            return new LowGravityParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, multiplier);
        }
    }
}