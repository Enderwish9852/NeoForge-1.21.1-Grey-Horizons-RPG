package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * RainCrownParticle
 *
 * NEW -- one of the little droplets thrown up when a raindrop hits the
 * ground.
 *
 * A splash is not one blob: the drop hits, and the water it was
 * carrying bursts up and outward in a crown of small droplets that fall
 * back. This is one of those droplets: it flies up, slows, and drops
 * back down.
 *
 * The ring on the ground is the other half of the same splash; see
 * RainSplashRingParticle.
 */
public class RainCrownParticle extends TextureSheetParticle {

    private final float upSpeed;

    protected RainCrownParticle(ClientLevel level, double x, double y, double z,
                                 double vx, double vy, double vz) {
        super(level, x, y, z);

        this.gravity    = 0.14f;
        this.hasPhysics = false;
        this.friction   = 0.92f;
        this.lifetime   = 10 + this.random.nextInt(6);
        this.quadSize   = 0.022f + this.random.nextFloat() * 0.016f;

        this.rCol = 0.90f;
        this.gCol = 0.94f;
        this.bCol = 0.98f;
        this.alpha = 0.75f;

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.upSpeed = (float) vy;
    }

    @Override
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;

        if (this.age++ >= this.lifetime) { this.remove(); return; }

        // the droplet slows as it goes up, then falls back
        this.yd -= 0.14f;
        if (this.yd < -0.25f) this.yd = -0.25f;
        this.xd *= 0.92f;
        this.zd *= 0.92f;

        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            RainCrownParticle particle = new RainCrownParticle(level, x, y, z, dx, dy, dz);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
