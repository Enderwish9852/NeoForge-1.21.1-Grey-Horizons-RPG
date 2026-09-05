package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * RainDropParticle
 *
 * Custom wind-aware rain particle rendered as an elongated streak that
 * visually rotates to align with the current wind angle AS SEEN FROM
 * THE CAMERA, rather than a flat camera-facing square dot.
 *
 * Why this needs a custom render() override (not just roll in tick()):
 * SingleQuadParticle's LOOKAT_XYZ facing mode orients the quad's local
 * X/Y axes to the camera's right/up vectors. To make a streak visually
 * point toward the particle's actual world-space fall direction, we need
 * to project that world-space velocity onto the camera's right/up axes
 * — which requires the Camera object, only available in render(), not
 * tick(). Computing roll from raw velocity magnitude alone (no camera
 * reference) produces an angle with no directional sign, which looked
 * inconsistent/random from different viewing angles.
 *
 * DEGENERATE CASE — looking near-vertically (straight up/down): the
 * camera's forward vector becomes nearly parallel to world-up, which is
 * also roughly the particle's fall direction. The right/up projection
 * becomes numerically unstable at this angle (tiny look-angle changes
 * cause large swings in projected components), producing the "streaks
 * fan out radially / look horizontal" bug. Real rain viewed straight up
 * also reads as short dots rather than directional streaks anyway, so
 * we detect this case (camera forward dot world-up near ±1) and fall
 * back to zero roll — a plain camera-facing streak — instead of letting
 * the unstable projection produce garbage angles.
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

    private static final float STREAK_LENGTH_SCALE = 3.2f; // how many quadSizes long the streak is
    private static final float STREAK_WIDTH_SCALE   = 0.35f; // how many quadSizes wide the streak is

    // How close (dot product) the camera's forward vector can get to
    // world-up/down before we consider the roll projection unstable.
    // 0.9 ≈ within ~25 degrees of looking straight up or down.
    private static final float VERTICAL_LOOK_THRESHOLD = 0.9f;

    private final float fallSpeed;

    protected RainDropParticle(ClientLevel level, double x, double y, double z, float intensity) {
        super(level, x, y, z);

        float clampedIntensity = Mth.clamp(intensity, 0f, 1f);
        this.fallSpeed = Mth.lerp(clampedIntensity, BASE_FALL_SPEED, MAX_FALL_SPEED);

        this.gravity    = 0.0f;   // we drive yd manually, no vanilla gravity accel
        this.lifetime   = 60;     // ~3 seconds, enough to fall through view range
        this.hasPhysics = false;  // no vanilla block-collision bounce
        this.friction   = 1.0f;   // no vanilla air drag, we set velocity directly
        this.quadSize   = 0.05f + clampedIntensity * 0.02f; // slightly bigger streaks in heavier rain

        // Desaturated grey, low alpha — real rain against overcast sky,
        // not a saturated blue droplet.
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

        // Despawn once it reaches/passes a solid surface
        if (this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isSolid()) {
            this.remove();
        }
    }

    /**
     * Overridden to compute roll from the particle's world-space velocity
     * projected onto the camera's actual right/up vectors, giving a
     * directionally-correct streak angle regardless of which way the
     * camera is currently facing — except near-vertical looks, where the
     * projection is unstable and we fall back to zero roll instead.
     */
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Quaternionf quaternionf = new Quaternionf();
        this.getFacingCameraMode().setRotation(quaternionf, camera, partialTicks);

        Quaternionf camRot = camera.rotation();
        Vector3f camRight   = new Vector3f(1, 0, 0).rotate(camRot);
        Vector3f camUp      = new Vector3f(0, 1, 0).rotate(camRot);
        Vector3f camForward = new Vector3f(0, 0, -1).rotate(camRot);

        float computedRoll;

        // Degenerate case guard — looking near straight up or down.
        if (Math.abs(camForward.y()) > VERTICAL_LOOK_THRESHOLD) {
            computedRoll = 0.0f;
        } else {
            Vector3f velocity = new Vector3f((float) this.xd, (float) this.yd, (float) this.zd);
            if (velocity.lengthSquared() > 1.0E-6f) {
                velocity.normalize();
            }

            float rightComponent = velocity.dot(camRight);
            float upComponent    = velocity.dot(camUp);

            computedRoll = (float) Math.atan2(rightComponent, -upComponent);
        }

        this.oRoll = this.roll;
        this.roll = computedRoll;

        quaternionf.rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));

        this.renderRotatedQuad(buffer, camera, quaternionf, partialTicks);
    }

    /**
     * Builds a stretched rectangle instead of vanilla's uniform square —
     * long along local Y (length axis), thin along local X (width axis).
     * Combined with the camera-relative roll computed in render(), this
     * reads as a streak angled toward the actual fall/wind direction.
     */
    @Override
    protected void renderRotatedQuad(VertexConsumer buffer, Quaternionf quaternion,
                                     float x, float y, float z, float partialTicks) {
        float size = this.getQuadSize(partialTicks);
        float halfWidth  = size * STREAK_WIDTH_SCALE;
        float halfLength = size * STREAK_LENGTH_SCALE;

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        renderStreakVertex(buffer, quaternion, x, y, z, halfWidth,  -halfLength, u1, v1, light);
        renderStreakVertex(buffer, quaternion, x, y, z, halfWidth,   halfLength, u1, v0, light);
        renderStreakVertex(buffer, quaternion, x, y, z, -halfWidth,  halfLength, u0, v0, light);
        renderStreakVertex(buffer, quaternion, x, y, z, -halfWidth, -halfLength, u0, v1, light);
    }

    private void renderStreakVertex(VertexConsumer buffer, Quaternionf quaternion,
                                    float x, float y, float z,
                                    float xOffset, float yOffset,
                                    float u, float v, int packedLight) {
        Vector3f vertex = new Vector3f(xOffset, yOffset, 0.0f)
                .rotate(quaternion)
                .add(x, y, z);
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
