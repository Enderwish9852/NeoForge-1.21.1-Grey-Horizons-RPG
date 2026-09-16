package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * FogWispParticle
 *
 * Slow-drifting, larger, soft ambient wisp for the "fog" weather type —
 * gives it a dedicated visual signature beyond reduced view distance.
 * Minimal wind influence (fog drifts on its own more than it's pushed),
 * long lifetime, pale low-alpha look.
 *
 * PARTICLE CLASS ONLY — no spawner wiring yet.
 */
public class FogWispParticle extends TextureSheetParticle {

    private static final float WIND_INFLUENCE = 0.10f;

    protected FogWispParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);

        this.gravity = 0.0f;
        this.lifetime = 300 + this.random.nextInt(200);
        this.hasPhysics = false;
        this.friction = 1.0f;
        this.quadSize = 0.4f + this.random.nextFloat() * 0.3f;

        this.rCol = 0.85f;
        this.gCol = 0.85f;
        this.bCol = 0.88f;
        this.alpha = 0.10f;
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

        float windDx = ClientSeasonState.getWindDx();
        float windDz = ClientSeasonState.getWindDz();

        this.xd = windDx * WIND_INFLUENCE;
        this.zd = windDz * WIND_INFLUENCE;
        this.yd = 0.0;

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
            FogWispParticle particle = new FogWispParticle(level, x, y, z);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
