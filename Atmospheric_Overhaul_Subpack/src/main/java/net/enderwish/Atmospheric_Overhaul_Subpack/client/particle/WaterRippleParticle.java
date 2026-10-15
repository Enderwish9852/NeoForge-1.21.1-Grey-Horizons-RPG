package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * WaterRippleParticle
 *
 * NEW -- an expanding ring lying on the water surface: slow growth
 * (unlike the splash's quick pop), low alpha, long life, and it DRIFTS
 * with the synced wind so flow direction reads on the surface. This is
 * the visual hook the upcoming water-current system will attach to
 * (stronger currents -> more, faster-drifting ripples).
 */
public class WaterRippleParticle extends TextureSheetParticle {

    private final float startSize;

    protected WaterRippleParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);

        this.gravity    = 0.0f;
        this.hasPhysics = false;
        this.friction   = 1.0f;
        this.lifetime   = 20 + this.random.nextInt(15);
        this.startSize  = 0.10f + this.random.nextFloat() * 0.06f;
        this.quadSize   = this.startSize;

        this.rCol = 0.72f;
        this.gCol = 0.80f;
        this.bCol = 0.92f;
        this.alpha = 0.0f; // fades in over the first ticks

        // drift with the wind (surface current stand-in for now)
        var wind = ClientSeasonState.getWindState();
        this.xd = wind.getEffectiveDx() * 0.012;
        this.zd = wind.getEffectiveDz() * 0.012;
        this.yd = 0;
    }

    @Override
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) { this.remove(); return; }

        // fade in over 3 ticks, out over the last 8
        if (this.age < 3) this.alpha = (this.age / 3f) * 0.70f;
        else if (this.age > this.lifetime - 8)
            this.alpha = ((this.lifetime - this.age) / 8f) * 0.70f;

        // steady expansion: the "ring" spreading
        this.quadSize = this.startSize + (this.age / (float) this.lifetime) * 0.55f;

        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /**
     * Full-bright: the ring must read on water at night too. Translucent
     * particles are multiplied by the world light at their position, which
     * made moonlit water render them near-black.
     */
    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public static class Provider implements net.minecraft.client.particle.ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            WaterRippleParticle particle = new WaterRippleParticle(level, x, y, z);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
