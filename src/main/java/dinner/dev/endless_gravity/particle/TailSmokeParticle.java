package dinner.dev.endless_gravity.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public class TailSmokeParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected TailSmokeParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.quadSize = 0.6F + level.random.nextFloat() * 0.4F;
        this.lifetime = 500 + level.random.nextInt(200);
        this.friction = 0.985F;
        this.gravity = 0.0F;
        this.oRoll = this.roll = level.random.nextFloat() * ((float) Math.PI * 2.0F);
        float brightness = 0.6F + level.random.nextFloat() * 0.4F;
        this.setColor(brightness, brightness, brightness);
        this.setAlpha(0.0F);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float t = this.age / (float) this.lifetime;
        this.oRoll = this.roll;
        this.roll += 0.002F;
        this.yd += 0.0005F;
        this.quadSize += 0.001F;
        float alpha;
        if (t < 0.05F) {
            alpha = 0.75F * (t / 0.05F);
        } else if (t > 0.6F) {
            alpha = 0.75F * (1.0F - (t - 0.6F) / 0.4F);
        } else {
            alpha = 0.75F;
        }
        this.setAlpha(alpha);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
