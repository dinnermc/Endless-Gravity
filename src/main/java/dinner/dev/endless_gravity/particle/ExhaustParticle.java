package dinner.dev.endless_gravity.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public class ExhaustParticle extends TextureSheetParticle {

    private static final float VIOLET_R = 0.55F;
    private static final float VIOLET_G = 0.05F;
    private static final float VIOLET_B = 1.0F;
    private static final float ORANGE_R = 1.0F;
    private static final float ORANGE_G = 0.65F;
    private static final float ORANGE_B = 0.0F;

    private final SpriteSet sprites;
    private final float baseSize;
    private final float brightness;
    private final float varyR;
    private final float varyG;
    private final float varyB;
    private final float transitionSpeed;

    protected ExhaustParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.baseSize = 0.28F + level.random.nextFloat() * 0.08F;
        this.quadSize = this.baseSize;
        this.lifetime = 20 + level.random.nextInt(10);
        this.friction = 0.94F + level.random.nextFloat() * 0.03F;
        this.gravity = 0.0F;
        this.oRoll = this.roll = level.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.brightness = 0.8F + level.random.nextFloat() * 0.4F;
        this.varyR = 0.85F + level.random.nextFloat() * 0.3F;
        this.varyG = 0.85F + level.random.nextFloat() * 0.3F;
        this.varyB = 0.85F + level.random.nextFloat() * 0.3F;
        this.transitionSpeed = 0.8F + level.random.nextFloat() * 0.4F;
        this.setColor(clamp(VIOLET_R * this.brightness * this.varyR),
                clamp(VIOLET_G * this.brightness * this.varyG),
                clamp(VIOLET_B * this.brightness * this.varyB));
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float t = this.age / (float) this.lifetime;
        float tp = t / this.transitionSpeed;
        if (tp > 1.0F) {
            tp = 1.0F;
        }
        float r;
        float g;
        float b;
        if (tp <= 0.5F) {
            float phase = tp * 2.0F;
            phase *= phase;
            r = lerp(VIOLET_R, ORANGE_R, phase);
            g = lerp(VIOLET_G, ORANGE_G, phase);
            b = lerp(VIOLET_B, ORANGE_B, phase);
        } else {
            float phase = (tp - 0.5F) * 2.0F;
            r = lerp(ORANGE_R, ORANGE_R * 0.6F, phase);
            g = lerp(ORANGE_G, ORANGE_G * 0.6F, phase);
            b = lerp(ORANGE_B, ORANGE_B * 0.6F, phase);
        }
        this.oRoll = this.roll;
        this.roll += 0.03F;
        this.setColor(clamp(r * this.brightness * this.varyR),
                clamp(g * this.brightness * this.varyG),
                clamp(b * this.brightness * this.varyB));
        this.setAlpha(0.95F * (1.0F - t * t));
        this.quadSize = this.baseSize * (1.0F + 0.9F * t * t);
        this.setSpriteFromAge(sprites);
    }

    private static float clamp(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
