package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * RainDropParticle
 *
 * Custom wind-aware rain particle rendered as a camera-facing streak
 * that SMOOTHLY shrinks toward a small dot as the camera's look angle
 * approaches vertical (up OR down), rather than snapping between two
 * separate render modes at a hard angle threshold.
 *
 * Why a smooth blend instead of a mode switch: looking straight up vs.
 * straight down are NOT the same case geometrically. Looking up puts
 * the camera nearly along the particle's fall line — genuine
 * foreshortening, a dot is correct. Looking down does not inherently
 * foreshorten every particle in view the same way; a blanket "camera
 * pitch beyond X degrees = dot" rule doesn't check foreshortening per
 * particle, so it produced wrong-looking results specifically when
 * looking down. Blending streak length toward zero based on how
 * foreshortened THIS PARTICLE's velocity actually looks from the
 * camera (not just overall camera pitch) fixes both cases with one
 * continuous formula and removes the visible "pop" at a hard cutoff.
 *
 * Geometry is built entirely in world space (length axis = velocity,
 * width axis = camera-plane perpendicular), then submitted in DOUBLE
 * winding order (front + back) since a freestanding quad's winding can
 * face away from the camera depending on angle and get backface-culled
 * — mirrors vanilla's own renderSnowAndRain(), which explicitly calls
 * RenderSystem.disableCull() before drawing its rain quads for the
 * same reason.
 *
 * Fall speed and streak length both scale with weather intensity, so
 * light drizzle looks visually different from a downpour.
 *
 * WIND_INFLUENCE, BASE_FALL_SPEED, and MAX_FALL_SPEED are public so
 * ClientDebugHandler can compute the live deflection angle for the F3
 * overlay using the same numbers driving the real particle motion.
 */
public class RainDropParticle extends TextureSheetParticle {

    public static final float WIND_INFLUENCE   = 0.45f;
    public static final float BASE_FALL_SPEED  = 0.35f; // blocks/tick at low intensity
    public static final float MAX_FALL_SPEED   = 0.65f; // blocks/tick at max intensity

    private static final float STREAK_LENGTH_SCALE = 3.2f; // max streak half-length multiplier
    private static final float STREAK_WIDTH_SCALE   = 0.35f; // half-width multiplier (constant)
    private static final float DOT_LENGTH_SCALE     = 0.5f;  // half-length multiplier when fully foreshortened (small dot)

    private final float fallSpeed;

    protected RainDropParticle(ClientLevel level, double x, double y, double z, float intensity) {
        super(level, x, y, z);

        float clampedIntensity = Mth.clamp(intensity, 0f, 1f);
        this.fallSpeed = Mth.lerp(clampedIntensity, BASE_FALL_SPEED, MAX_FALL_SPEED);

        this.gravity    = 0.0f;
        this.lifetime   = 60;
        this.hasPhysics = false;
        this.friction   = 1.0f;
        this.quadSize   = 0.05f + clampedIntensity * 0.02f;

        this.rCol = 0.78f;
        this.gCol = 0.80f;
        this.bCol = 0.82f;
        this.alpha = 0.35f;
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
        this.yd = -fallSpeed;

        this.move(this.xd, this.yd, this.zd);

        if (this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isSolid()) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();

        float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x());
        float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y());
        float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z());

        Quaternionf camRot = camera.rotation();
        Vector3f camRight = new Vector3f(1, 0, 0).rotate(camRot);
        Vector3f camUp    = new Vector3f(0, 1, 0).rotate(camRot);
        // Camera's forward/view direction — used to measure how end-on
        // (foreshortened) THIS particle's velocity looks from here.
        Vector3f camForward = new Vector3f(0, 0, -1).rotate(camRot);

        Vector3f velocity = new Vector3f((float) this.xd, (float) this.yd, (float) this.zd);
        if (velocity.lengthSquared() > 1.0E-8f) {
            velocity.normalize();
        } else {
            velocity.set(0f, -1f, 0f);
        }

        // Foreshortening factor: how aligned the velocity is with the
        // camera's forward axis. 0 = fully side-on (full streak), 1 =
        // fully end-on (fully foreshortened, should look like a dot).
        float alignment = Math.abs(velocity.dot(camForward));
        // Smoothstep-style easing so the transition isn't linear/abrupt.
        float foreshorten = alignment * alignment * (3f - 2f * alignment);

        float vRight = velocity.dot(camRight);
        float vUp    = velocity.dot(camUp);
        float screenLenSq = vRight * vRight + vUp * vUp;
        float sx, sy;
        if (screenLenSq > 1.0E-6f) {
            float screenLen = (float) Math.sqrt(screenLenSq);
            sx = vRight / screenLen;
            sy = vUp / screenLen;
        } else {
            sx = 0f;
            sy = 1f;
        }

        Vector3f lengthAxis = new Vector3f(camRight).mul(sx).add(new Vector3f(camUp).mul(sy));
        Vector3f widthAxis  = new Vector3f(camRight).mul(-sy).add(new Vector3f(camUp).mul(sx));

        float size = this.getQuadSize(partialTicks);
        float halfWidth = size * STREAK_WIDTH_SCALE;
        // Blend half-length smoothly from full streak length down to a
        // small dot-like length as foreshortening approaches 1.
        float halfLength = size * Mth.lerp(foreshorten, STREAK_LENGTH_SCALE, DOT_LENGTH_SCALE);

        Vector3f center = new Vector3f(px, py, pz);
        Vector3f lengthOffset = new Vector3f(lengthAxis).mul(halfLength);
        Vector3f widthOffset  = new Vector3f(widthAxis).mul(halfWidth);

        Vector3f head = new Vector3f(center).add(lengthOffset);
        Vector3f tail = new Vector3f(center).sub(lengthOffset);

        Vector3f v1 = new Vector3f(tail).add(widthOffset);
        Vector3f v2 = new Vector3f(head).add(widthOffset);
        Vector3f v3 = new Vector3f(head).sub(widthOffset);
        Vector3f v4 = new Vector3f(tail).sub(widthOffset);

        float u0 = this.getU0();
        float u1 = this.getU1();
        float texV0 = this.getV0();
        float texV1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        // Front winding
        addVertex(buffer, v1, u1, texV1, light);
        addVertex(buffer, v2, u1, texV0, light);
        addVertex(buffer, v3, u0, texV0, light);
        addVertex(buffer, v4, u0, texV1, light);

        // Back winding — see class javadoc.
        addVertex(buffer, v4, u0, texV1, light);
        addVertex(buffer, v3, u0, texV0, light);
        addVertex(buffer, v2, u1, texV0, light);
        addVertex(buffer, v1, u1, texV1, light);
    }

    private void addVertex(VertexConsumer buffer, Vector3f vertex, float u, float v, int packedLight) {
        buffer.addVertex(vertex.x(), vertex.y(), vertex.z())
                .setUv(u, v)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setLight(packedLight);
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
            RainDropParticle particle = new RainDropParticle(level, x, y, z, (float) dy);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}