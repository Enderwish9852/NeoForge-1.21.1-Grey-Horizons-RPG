package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class RainSplashParticle extends TextureSheetParticle {

    private final float startSize;

    protected RainSplashParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);

        this.gravity    = 0.0f;
        this.hasPhysics = false;
        this.friction   = 1.0f;
        this.lifetime   = 6 + this.random.nextInt(4);
        this.startSize  = 0.08f + this.random.nextFloat() * 0.04f;
        this.quadSize   = this.startSize;

        this.rCol  = 0.80f;
        this.gCol  = 0.82f;
        this.bCol  = 0.85f;
        this.alpha = 0.5f;

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
        this.quadSize = Mth.lerp(lifeFraction, startSize, startSize * 2.2f);
        this.alpha = Mth.lerp(lifeFraction, 0.5f, 0.0f);
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
