package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.LocalWindCalculator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * RainDropParticle
 *
 * Custom wind-aware rain particle rendered as a camera-facing streak
 * that SMOOTHLY shrinks toward a small dot as the camera's look angle
 * approaches vertical.
 *
 * Wind blending: particles within LocalWindCalculator.DETECTION_RANGE
 * blocks of the player smoothly blend toward that player's FELT wind
 * (obstruction-adjusted); particles farther out use pure GLOBAL wind.
 * This is a smooth distance-based blend rather than a hard cutoff, same
 * reasoning as the foreshortening blend below — avoids a visible "pop"
 * at the boundary.
 */
public class RainDropParticle extends TextureSheetParticle {

    public static final float WIND_INFLUENCE   = 0.45f;
    public static final float BASE_FALL_SPEED  = 0.35f;
    public static final float MAX_FALL_SPEED   = 0.65f;

    private static final float STREAK_LENGTH_SCALE = 3.2f;
    private static final float STREAK_WIDTH_SCALE   = 0.35f;
    private static final float DOT_LENGTH_SCALE     = 0.5f;

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

        // Blend between global and felt wind based on distance from the player.
        float windDx = ClientSeasonState.getWindDx();
        float windDz = ClientSeasonState.getWindDz();

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            float distFromPlayer = (float) player.position()
                    .distanceTo(new Vec3(this.x, this.y, this.z));
            float blendFactor = Mth.clamp(
                    1.0f - (distFromPlayer / LocalWindCalculator.DETECTION_RANGE), 0f, 1f);

            float feltDx = ClientSeasonState.getFeltWindDx();
            float feltDz = ClientSeasonState.getFeltWindDz();

            windDx = Mth.lerp(blendFactor, windDx, feltDx);
            windDz = Mth.lerp(blendFactor, windDz, feltDz);
        }

        this.xd = windDx * WIND_INFLUENCE;
        this.zd = windDz * WIND_INFLUENCE;
        this.yd = -fallSpeed;

        this.move(this.xd, this.yd, this.zd);

        if (this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).isSolid()) {
            this.level.addParticle(ModParticles.RAIN_SPLASH.get(), this.x, this.y, this.z, 0.0, 0.0, 0.0);
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
        Vector3f camRight   = new Vector3f(1, 0, 0).rotate(camRot);
        Vector3f camUp      = new Vector3f(0, 1, 0).rotate(camRot);
        Vector3f camForward = new Vector3f(0, 0, -1).rotate(camRot);

        Vector3f velocity = new Vector3f((float) this.xd, (float) this.yd, (float) this.zd);
        if (velocity.lengthSquared() > 1.0E-8f) {
            velocity.normalize();
        } else {
            velocity.set(0f, -1f, 0f);
        }

        float alignment = Math.abs(velocity.dot(camForward));
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

        addVertex(buffer, v1, u1, texV1, light);
        addVertex(buffer, v2, u1, texV0, light);
        addVertex(buffer, v3, u0, texV0, light);
        addVertex(buffer, v4, u0, texV1, light);

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