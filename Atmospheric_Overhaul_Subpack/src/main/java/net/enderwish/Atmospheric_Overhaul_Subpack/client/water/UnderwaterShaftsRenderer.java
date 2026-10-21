package net.enderwish.Atmospheric_Overhaul_Subpack.client.water;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.sky.SunRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL30C;

/**
 * UnderwaterShaftsRenderer
 *
 * NEW -- sunlight coming down through the water surface, seen from
 * under the water.
 *
 * The shafts are the sun's light refracted at the surface, broken into
 * bands by the ripples on it, and scattered by the water. The shader
 * (gh_water_shafts) rebuilds the view ray, samples along it through the
 * water, and for every sample walks back up the refracted sun ray to
 * the surface point that lit it, reading the caustic pattern there.
 * The deeper the light had to travel, the more the water absorbed, so
 * looking down fades out and looking up towards the surface glows.
 *
 * Runs at renderLevel TAIL, right AFTER the underwater wobble pass:
 * the wobble distorts what is already drawn, but the shafts are light
 * IN the water, not a view through the surface, so they must not be
 * distorted by it.
 *
 * Only when the sun is actually above the horizon -- at night there is
 * nothing to refract.
 */
public final class UnderwaterShaftsRenderer {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("GHAtmospheric/UnderwaterShafts");

    private UnderwaterShaftsRenderer() {}

    /** How strong the sun's light is, at the zenith. */
    private static final float MAX_STRENGTH = 1.0f;
    /** How quickly the light dies as the sun drops towards the horizon. */
    private static final float HORIZON_FADE = 0.22f;

    private static final Matrix4f IDENTITY = new Matrix4f();

    private static VertexBuffer quad;
    private static boolean loggedFirstRender;

    /**
     * Called at renderLevel TAIL from LevelRendererWaterUniformsMixin,
     * after the wobble pass.
     */
    public static void render(Camera camera, Matrix4f viewMatrix, Matrix4f projectionMatrix,
                              float partialTick) {
        if (!WaterShaderUniforms.ENABLED) return;
        if (camera.getFluidInCamera() != FogType.WATER) return;
        ShaderInstance shader = WaterPostShaderRegistry.shaftsShader;
        if (shader == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        RenderTarget main = mc.getMainRenderTarget();
        ensureQuad();

        // the sun, and how much of it is above the horizon
        float dayT = mc.level.getTimeOfDay(partialTick);
        Vector3f sun = SunRenderer.worldDirection(dayT);
        if (sun.y <= 0.0f) return;      // nothing enters the water at night

        // the light bends at the surface: air -> water, eta = 1/1.333
        Vector3f refracted = refract(new Vector3f(-sun.x, -sun.y, -sun.z),
                new Vector3f(0.0f, 1.0f, 0.0f), 1.0f / 1.333f);
        if (refracted == null) return;

        Vec3 cam = camera.getPosition();
        double surfaceY = findSurfaceY(mc.level, cam);
        if (Double.isNaN(surfaceY)) return;

        float strength = MAX_STRENGTH * Mth.clamp(sun.y / HORIZON_FADE, 0.0f, 1.0f);
        float rain = mc.level.getRainLevel(partialTick);
        strength *= 1.0f - 0.6f * rain;
        if (strength <= 0.001f) return;

        // warm at the horizon, near-white higher up
        float high = Mth.clamp(sun.y / 0.35f, 0.0f, 1.0f);
        float tintR = 1.0f;
        float tintG = Mth.lerp(high, 0.80f, 0.98f);
        float tintB = Mth.lerp(high, 0.58f, 0.95f);

        // the walk-cycle head bob is applied to the projection matrix;
        // the shafts are real light in real water and should not
        // inherit a fake movement of the camera
        Matrix4f steadyProjection = SunRenderer.unbobbedProjection(projectionMatrix);
        Matrix4f invProj = new Matrix4f(steadyProjection).invert();
        Matrix4f invView = new Matrix4f(viewMatrix).invert();

        int[] boundFbo = new int[1];
        GL30C.glGetIntegerv(GL30C.GL_DRAW_FRAMEBUFFER_BINDING, boundFbo);
        int prevFbo = boundFbo[0];

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);

        uniformMat(shader, "RayInvProjMat", invProj);
        uniformMat(shader, "RayInvViewMat", invView);
        uniform3(shader, "CameraPos", (float) cam.x, (float) cam.y, (float) cam.z);
        uniform3(shader, "SunDir", refracted.x, refracted.y, refracted.z);
        uniform3(shader, "SunColor", tintR, tintG, tintB);
        uniform1(shader, "SunStrength", strength);
        uniform1(shader, "SurfaceY", (float) surfaceY);
        // the caustic clock is in SECONDS: the game clock counts ticks,
        // and feeding ticks to the shader dragged the pattern across
        // the water at over ten blocks a second, which read as random
        // light patches flashing rather than light through a surface
        double timeSeconds = (mc.level.getGameTime() % 100000L + partialTick) / 20.0;
        uniform1(shader, "Time", (float) timeSeconds);

        drawQuad(shader);

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();

        if (!loggedFirstRender) {
            loggedFirstRender = true;
            LOGGER.info("first underwater shafts: surface={} strength={} dir=({}, {}, {})",
                    String.format("%.2f", surfaceY), String.format("%.2f", strength),
                    String.format("%.3f", refracted.x), String.format("%.3f", refracted.y),
                    String.format("%.3f", refracted.z));
        }
    }

    /**
     * The height of the water surface above the camera: the first
     * non-water block above the camera's column, less the 1/8 of a
     * block the water's surface sits below the block top.
     */
    private static double findSurfaceY(ClientLevel level, Vec3 cam) {
        int bx = Mth.floor(cam.x);
        int bz = Mth.floor(cam.z);
        int by = Mth.floor(cam.y);
        for (int dy = 0; dy < 48; dy++) {
            BlockPos pos = new BlockPos(bx, by + dy, bz);
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.is(Fluids.WATER)) {
                // the water fills up to the block below this one
                return pos.getY() - 0.125;
            }
        }
        return Double.NaN;
    }

    /**
     * GLSL's refract(): bends a direction at a surface. Returns null on
     * total internal reflection.
     */
    private static Vector3f refract(Vector3f incident, Vector3f normal, float eta) {
        float cosI = -incident.dot(normal);
        float k = 1.0f - eta * eta * (1.0f - cosI * cosI);
        if (k < 0.0f) return null;
        return incident.mul(eta)
                .add(normal.mul(eta * cosI - (float) Math.sqrt(k)))
                .normalize();
    }

    // ── GPU helpers (same pattern as the other passes) ───────────────────

    private static void drawQuad(ShaderInstance shader) {
        quad.bind();
        quad.drawWithShader(IDENTITY, IDENTITY, shader);
        VertexBuffer.unbind();
    }

    private static void ensureQuad() {
        if (quad != null) return;
        BufferBuilder buf = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(-1.0f, -1.0f, 0.999f).setUv(0.0f, 0.0f);
        buf.addVertex(1.0f, -1.0f, 0.999f).setUv(1.0f, 0.0f);
        buf.addVertex(1.0f, 1.0f, 0.999f).setUv(1.0f, 1.0f);
        buf.addVertex(-1.0f, 1.0f, 0.999f).setUv(0.0f, 1.0f);
        MeshData mesh = buf.buildOrThrow();
        quad = new VertexBuffer(VertexBuffer.Usage.STATIC);
        quad.bind();
        quad.upload(mesh);
        VertexBuffer.unbind();
    }

    private static void uniform1(ShaderInstance shader, String name, float v) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(v);
    }

    private static void uniform3(ShaderInstance shader, String name, float a, float b, float c) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(a, b, c);
    }

    private static void uniformMat(ShaderInstance shader, String name, Matrix4f m) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(m);
    }
}
