package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * RainSplashParticle
 *
 * TUNING FIX: previous version was too small/brief/faint to actually
 * notice in a live game (quadSize 0.08-0.12, lifetime 6-9 ticks,
 * alpha 0.5) even though it was spawning correctly — RainDropParticle
 * genuinely does call addParticle(RAIN_SPLASH, ...) whenever a drop
 * hits a solid block. Bumped size/lifetime/alpha so the flash reads
 * clearly without changing its fundamental "quick splash" character.
 */
public class RainSplashParticle extends TextureSheetParticle {

    private final float startSize;

    protected RainSplashParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);

        this.gravity    = 0.0f;
        this.hasPhysics = false;
        this.friction   = 1.0f;
        this.lifetime   = 10 + this.random.nextInt(5);
        this.startSize  = 0.15f + this.random.nextFloat() * 0.07f;
        this.quadSize   = this.startSize;

        this.rCol  = 0.80f;
        this.gCol  = 0.82f;
        this.bCol  = 0.85f;
        this.alpha = 0.7f;

        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
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
        this.quadSize = Mth.lerp(lifeFraction, startSize, startSize * 2.5f);
        this.alpha = Mth.lerp(lifeFraction, 0.7f, 0.0f);
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
