package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * DustMoteParticle
 *
 * Very light, slow-drifting ambient particle — mostly horizontal
 * movement with wind, subtle vertical bob, long lifetime, very low
 * alpha. Fits wasteland/dry biomes; could later scale with a
 * "dust storm" weather via intensity alone, matching the project's
 * universal-weather-types philosophy (no new JSON category needed).
 *
 * PARTICLE CLASS ONLY — no spawner wiring yet.
 */
public class DustMoteParticle extends TextureSheetParticle {

    private static final float WIND_INFLUENCE = 0.25f;
    private final float bobPhase;

    protected DustMoteParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);

        this.gravity = 0.0f;
        this.lifetime = 200 + this.random.nextInt(150);
        this.hasPhysics = false;
        this.friction = 1.0f;
        this.quadSize = 0.02f + this.random.nextFloat() * 0.015f;
        this.bobPhase = this.random.nextFloat() * (float) Math.PI * 2f;

        this.rCol = 0.75f;
        this.gCol = 0.68f;
        this.bCol = 0.55f;
        this.alpha = 0.15f;
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
        this.yd = (float) Math.sin((this.age + bobPhase) * 0.05) * 0.004f;

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
            DustMoteParticle particle = new DustMoteParticle(level, x, y, z);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
