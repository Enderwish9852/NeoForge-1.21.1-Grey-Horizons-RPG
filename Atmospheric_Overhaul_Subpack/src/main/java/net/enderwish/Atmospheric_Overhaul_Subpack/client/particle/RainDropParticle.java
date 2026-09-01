package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * RainDropParticle
 *
 * Custom wind-aware rain particle. Reads live wind velocity from
 * ClientSeasonState every tick so it drifts with the current WindState
 * instead of falling straight down like vanilla rain.
 *
 * WIND_INFLUENCE and FALL_SPEED are public so ClientDebugHandler can
 * compute the actual live deflection angle for the F3 overlay using
 * the same numbers driving the real particle motion.
 *
 * NOTE: this spawns ALONGSIDE vanilla rain for now (vanilla rain is not
 * cancelled). Once wind behavior looks right, the next step is a mixin
 * to cancel vanilla's rain rendering so only this system shows.
 */
public class RainDropParticle extends TextureSheetParticle {

    public static final float WIND_INFLUENCE = 0.45f;
    public static final float FALL_SPEED     = 0.45f; // blocks/tick downward

    protected RainDropParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.gravity    = 0.0f;   // we drive yd manually, no vanilla gravity accel
        this.lifetime   = 40;     // ~2 seconds, enough to fall through view range
        this.quadSize   = 0.06f;
        this.hasPhysics = false;  // no vanilla block-collision bounce
        this.friction   = 1.0f;   // no vanilla air drag, we set velocity directly
        this.setColor(0.6f, 0.7f, 1.0f); // placeholder blue tint until real texture
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
        this.yd = -FALL_SPEED;

        this.move(this.xd, this.yd, this.zd);

        // Despawn once it reaches/passes a solid surface
        if (this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isSolid()) {
            this.remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // ── Factory — hands each new instance a sprite from the JSON texture list ──

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            RainDropParticle particle = new RainDropParticle(level, x, y, z);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
