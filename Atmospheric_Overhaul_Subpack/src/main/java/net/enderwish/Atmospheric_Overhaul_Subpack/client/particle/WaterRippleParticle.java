package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * WaterRippleParticle
 *
 * NEW -- an expanding ring lying FLAT on the water surface.
 *
 * The old version was a plain TextureSheetParticle, and a
 * TextureSheetParticle is always a CAMERA-FACING billboard: it can
 * never lie flat, so a "ring on the water" drawn that way stands up
 * like a hoop instead of spreading across the surface. That is what the
 * vertical streaks were.
 *
 * This one overrides render() and builds its quad in the XZ plane
 * itself, so the ring spreads out flat on the water the way a real
 * ripple does. The quad is emitted TWICE, once with each winding, so
 * it is visible from above AND from below (underwater) -- with
 * back-face culling on, exactly one of the two survives per side, so
 * nothing is ever drawn twice.
 *
 * Spawned by WaterImpactHandler (an item or an entity crossing the
 * water surface) and by RainDropParticle (a raindrop hitting the
 * water). The particle's "strength" travels in the spawn's dz
 * argument: 0 = a raindrop dimple, 1 = a heavy body hitting the water.
 */
public class WaterRippleParticle extends TextureSheetParticle {

    /**
     * Draws flat on the surface. Same setup as
     * ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT but with the depth
     * WRITE off: a ripple is a decal on the water, it must not punch a
     * hole in the depth buffer (the god-ray pass depth-tests at the far
     * plane and would otherwise see the ripple's depth).
     */
    public static final ParticleRenderType FLAT_RIPPLE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "GH_FLAT_RIPPLE";
        }
    };

    private final float startSize;
    private final float growth;
    private final float peakAlpha;

    protected WaterRippleParticle(ClientLevel level, double x, double y, double z, float strength) {
        super(level, x, y, z);

        this.gravity    = 0.0f;
        this.hasPhysics = false;
        this.friction   = 1.0f;
        this.roll       = 0.0f;

        float s = Mth.clamp(strength, 0.0f, 2.0f);
        boolean dimple = s < 0.22f;

        if (dimple) {
            // a raindrop dimple: tiny, quick, faint
            this.lifetime  = 7 + this.random.nextInt(4);
            this.startSize = 0.05f + s * 0.25f;
            this.growth    = 0.10f;
            this.peakAlpha = 0.32f;
        } else {
            // a body hitting the water: a real spreading ring
            this.lifetime  = 16 + this.random.nextInt(10);
            this.startSize = 0.14f + s * 0.30f;
            this.growth    = 0.40f + s * 0.45f;
            this.peakAlpha = 0.60f;
        }
        this.quadSize = this.startSize;

        this.rCol = 0.78f;
        this.gCol = 0.86f;
        this.bCol = 0.96f;
        this.alpha = 0.0f;

        // drift with the wind (the surface current the ring rides on)
        var wind = ClientSeasonState.getWindState();
        this.xd = wind.getEffectiveDx() * 0.010;
        this.zd = wind.getEffectiveDz() * 0.010;
        this.yd = 0.0f;
    }

    @Override
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) { this.remove(); return; }

        // fade in over the first ticks, out over the last few
        if (this.age < 2) this.alpha = (this.age / 2f) * this.peakAlpha;
        else if (this.age > this.lifetime - 6)
            this.alpha = ((this.lifetime - this.age) / 6f) * this.peakAlpha;

        // the ring spreads out across the surface
        this.quadSize = this.startSize + (this.age / (float) this.lifetime) * this.growth;

        this.move(this.xd, this.yd, this.zd);
    }

    /**
     * Draws the quad FLAT on the water: four corners in the XZ plane
     * instead of a camera-facing billboard.
     *
     * The first four vertices wind one way (normal pointing up, seen
     * from above the water) and the last four the other way (normal
     * pointing down, seen from under the water). Back-face culling
     * then keeps exactly one of the two per viewing side.
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
        return FLAT_RIPPLE;
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

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            // the spawn's dz carries the impact strength
            WaterRippleParticle particle = new WaterRippleParticle(level, x, y, z, (float) dz);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
