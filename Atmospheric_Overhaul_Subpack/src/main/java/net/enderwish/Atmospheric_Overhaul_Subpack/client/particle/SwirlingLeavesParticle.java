package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * SwirlingLeavesParticle
 *
 * Tumbling leaf — much lighter than rain, so wind influence is far
 * stronger, and it visibly spins (via the inherited `roll` field,
 * confirmed accessible from Particle's base source) rather than falling
 * in a straight line. Uses the DEFAULT SingleQuadParticle billboard
 * render — no custom streak math needed, unlike RainDropParticle.
 *
 * Placeholder autumn brown-orange tint on a plain texture, same
 * technique as rain/splash.
 *
 * PARTICLE CLASS ONLY — spawn triggers (near trees, autumn bias,
 * density) not wired into any spawner yet.
 */
public class SwirlingLeavesParticle extends TextureSheetParticle {

    private static final float WIND_INFLUENCE = 0.9f;
    private static final float FALL_SPEED = 0.03f;
    private static final float SPIN_SPEED = 0.15f;

    protected SwirlingLeavesParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);

        this.gravity = 0.0f;
        this.lifetime = 100 + this.random.nextInt(60);
        this.hasPhysics = false;
        this.friction = 1.0f;
        this.quadSize = 0.08f + this.random.nextFloat() * 0.04f;

        this.rCol = 0.55f + this.random.nextFloat() * 0.2f;
        this.gCol = 0.30f + this.random.nextFloat() * 0.15f;
        this.bCol = 0.10f;
        this.alpha = 0.85f;

        this.roll = this.random.nextFloat() * (float) Math.PI * 2f;
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

        float jitterX = (this.random.nextFloat() - 0.5f) * 0.03f;
        float jitterZ = (this.random.nextFloat() - 0.5f) * 0.03f;

        this.xd = windDx * WIND_INFLUENCE + jitterX;
        this.zd = windDz * WIND_INFLUENCE + jitterZ;
        this.yd = -FALL_SPEED + (float) Math.sin(this.age * 0.1) * 0.01f;

        this.oRoll = this.roll;
        this.roll += SPIN_SPEED;

        this.move(this.xd, this.yd, this.zd);

        if (this.onGround) {
            this.remove();
        }
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
            SwirlingLeavesParticle particle = new SwirlingLeavesParticle(level, x, y, z);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
