package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * RainSplashRingParticle
 *
 * NEW -- the flat ring a raindrop leaves on the ground where it
 * landed.
 *
 * It lies ON the ground, like the water ripples do, and spreads out
 * quickly: a drop's splash is over in a moment, where a ripple on
 * water goes on for much longer. The old splash was a small blob
 * popping straight up, which read as a blob of paint rather than as
 * water hitting the ground.
 *
 * It is drawn flat, the same way the water ripple is: a camera facing
 * billboard can never lie on the ground, so the quad is built in the XZ
 * plane, with both windings so it shows from above and below.
 */
public class RainSplashRingParticle extends TextureSheetParticle {

    private final float startSize;
    private final float growth;

    protected RainSplashRingParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);

        this.gravity    = 0.0f;
        this.hasPhysics = false;
        this.friction   = 1.0f;
        this.lifetime   = 9 + this.random.nextInt(4);
        this.startSize  = 0.06f + this.random.nextFloat() * 0.03f;
        this.quadSize   = this.startSize;
        this.growth     = 0.30f + this.random.nextFloat() * 0.10f;

        this.rCol = 0.86f;
        this.gCol = 0.90f;
        this.bCol = 0.95f;
        this.alpha = 0.0f;   // fades in over the first ticks
    }

    @Override
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) { this.remove(); return; }

        if (this.age < 2) {
            this.alpha = (this.age / 2f) * 0.55f;
        } else {
            this.alpha = ((this.lifetime - this.age) / (float) (this.lifetime - 2)) * 0.55f;
        }

        this.quadSize = this.startSize + (this.age / (float) this.lifetime) * this.growth;
    }

    /**
     * Flat on the ground: the quad is built in the XZ plane, with both
     * windings so it is visible from above and from below.
     */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cam.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cam.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cam.z());

        float r = this.getQuadSize(partialTicks);
        float u0 = this.getU0(), u1 = this.getU1();
        float v0 = this.getV0(), v1 = this.getV1();
        int light = this.getLightColor(partialTicks);
        float cr = this.rCol, cg = this.gCol, cb = this.bCol, a = this.alpha;

        // normal +Y: seen from above
        buffer.addVertex(x - r, y, z - r).setUv(u0, v1).setColor(cr, cg, cb, a).setLight(light);
        buffer.addVertex(x - r, y, z + r).setUv(u0, v0).setColor(cr, cg, cb, a).setLight(light);
        buffer.addVertex(x + r, y, z + r).setUv(u1, v0).setColor(cr, cg, cb, a).setLight(light);
        buffer.addVertex(x + r, y, z - r).setUv(u1, v1).setColor(cr, cg, cb, a).setLight(light);

        // normal -Y: seen from below
        buffer.addVertex(x + r, y, z - r).setUv(u1, v1).setColor(cr, cg, cb, a).setLight(light);
        buffer.addVertex(x + r, y, z + r).setUv(u1, v0).setColor(cr, cg, cb, a).setLight(light);
        buffer.addVertex(x - r, y, z + r).setUv(u0, v0).setColor(cr, cg, cb, a).setLight(light);
        buffer.addVertex(x - r, y, z - r).setUv(u0, v1).setColor(cr, cg, cb, a).setLight(light);
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
            RainSplashRingParticle particle = new RainSplashRingParticle(level, x, y, z);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
