package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
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
 * SnowFlakeParticle
 *
 * NEW -- a falling flake, for the winter precipitation.
 *
 * The rain particle is a streak: fast, straight, and it splashes when
 * it lands. Snow is none of those things. It falls slowly, it is
 * pushed sideways by the wind far more than rain is (it has time to be
 * pushed), and it wanders as it falls rather than dropping straight.
 * Each flake carries its own sway phase, so a fall of snow does not
 * look like a curtain of parallel lines.
 */
public class SnowFlakeParticle extends TextureSheetParticle {

    /** How fast a flake falls. Snow drifts; rain drops. */
    private static final float FALL_SPEED = 0.075f;
    /**
     * How much of the wind a flake picks up, against the wind's own
     * speed. The wind's effective velocity is already a movement per
     * tick, so this has to be small: at 1.6 the flakes were travelling
     * sideways at several times their own falling speed and never came
     * down at all.
     */
    private static final float WIND_CATCH = 0.15f;
    /** The sideways wander, in blocks per tick. */
    private static final float SWAY = 0.03f;

    private final float swayPhase;
    private final float swaySpeed;
    private final float baseX;
    private final float baseZ;

    protected SnowFlakeParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);

        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.friction = 1.0f;
        this.lifetime = 90 + this.random.nextInt(70);

        this.quadSize = 0.035f + this.random.nextFloat() * 0.035f;
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        this.alpha = 0.80f;

        this.swayPhase = this.random.nextFloat() * (float) (Math.PI * 2.0);
        this.swaySpeed = 0.03f + this.random.nextFloat() * 0.035f;
        this.baseX = (float) x;
        this.baseZ = (float) z;

        this.yd = -(FALL_SPEED + this.random.nextFloat() * FALL_SPEED * 0.6);
        this.updateWind(0.0f);
    }

    /** The wind pushes the flake about, and it wanders as it falls. */
    private void updateWind(float age) {
        var wind = ClientSeasonState.getWindState();
        float sway = (float) Math.sin(this.swayPhase + age * this.swaySpeed) * SWAY;
        this.xd = wind.getEffectiveDx() * WIND_CATCH + sway;
        this.zd = wind.getEffectiveDz() * WIND_CATCH;
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

        this.updateWind(this.age);
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /**
     * Full-bright: a flake is lit by the sky, not by the block light
     * where it happens to be falling, so at night it should still be
     * visible against the dark.
     */
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
            SnowFlakeParticle particle = new SnowFlakeParticle(level, x, y, z);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
