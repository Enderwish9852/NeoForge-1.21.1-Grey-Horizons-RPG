package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * RainSplashParticle
 *
 * REDESIGN: previous version grew to 2.5x its start size (0.15-0.22 up
 * to ~0.55 blocks) with zero velocity — looked like an expanding blob/
 * explosion rather than a splash, and had no motion at all so it
 * couldn't read as a "bounce." Now: much smaller (0.06-0.09 start),
 * far gentler growth (1.3x max), and a genuine small upward kick that
 * decelerates over its short lifetime — an actual little hop rather
 * than a static expanding quad.
 */
public class RainSplashParticle extends TextureSheetParticle {

    private final float startSize;

    protected RainSplashParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);

        this.gravity    = 0.0f;
        this.hasPhysics = false;
        this.friction   = 1.0f;
        this.lifetime   = 8 + this.random.nextInt(4);
        this.startSize  = 0.06f + this.random.nextFloat() * 0.03f;
        this.quadSize   = this.startSize;

        this.rCol  = 0.80f;
        this.gCol  = 0.82f;
        this.bCol  = 0.85f;
        this.alpha = 0.65f;

        // Small upward "pop" — this is what actually reads as a bounce,
        // rather than just a shape growing in place.
        this.xd = (this.random.nextFloat() - 0.5f) * 0.02f;
        this.yd = 0.05f + this.random.nextFloat() * 0.03f;
        this.zd = (this.random.nextFloat() - 0.5f) * 0.02f;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float lifeFraction = (float) this.age / this.lifetime;
        this.quadSize = Mth.lerp(lifeFraction, startSize, startSize * 1.3f);
        this.alpha = Mth.lerp(lifeFraction, 0.65f, 0.0f);

        this.yd -= 0.015f; // arcs back down within the same short lifetime
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            RainSplashParticle particle = new RainSplashParticle(level, x, y, z);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
