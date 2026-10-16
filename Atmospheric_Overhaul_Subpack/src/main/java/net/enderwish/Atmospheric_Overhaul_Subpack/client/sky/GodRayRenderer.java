package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL30C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GodRayRenderer
 *
 * NEW -- screen-space god rays (crepuscular shafts), same technique the
 * studied shaderpacks build on but without any Iris/shadow-map pass:
 *
 * 1. CPU, per frame: the sun's world direction from vanilla's own sky
 *    rotation chain (YP(-90) then XP(timeOfDay*360), LevelRenderer
 *    1657-1659), its screen position through the mixin's view+projection
 *    matrices, and one march from the camera toward the sun through
 *    OUR CloudDensityField (sampled in cloud space: world minus drift,
 *    with the tile system's seed) -- the gaps in the voxel clouds are
 *    what the shafts stream through, so rays and clouds can never
 *    disagree;
 * 2. occlusion pass, half-res: the sun disc stamped only where the
 *    main target's DEPTH is exactly sky, so terrain silhouettes cut
 *    the light (ShaderInstance.setSampler takes the raw GL texture id);
 * 3. radial blur, half-res, run twice chained: short sharp reach then
 *    full-length soft reach, each accumulating toward SunScreen with a
 *    per-pass decay;
 * 4. composite: additive (ONE,ONE) onto the framebuffer the cloud
 *    render was drawing into (saved and rebound around the passes, so
 *    both Fabulous and plain rendering work), depth-tested at the far
 *    plane so rays land on sky pixels, never over near terrain, with
 *    the elevation-gated intensity and warm horizon tint.
 *
 * v1 scope: sun only, no volumetric shadow pass (that would need a
 * sun-shadowmap render -- a separate feature).
 */
public final class GodRayRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("GHAtmospheric/GodRays");

    private GodRayRenderer() {}

    // ── Tuning (v1 constants) ─────────────────────────────────────────────

    private static final float BASE_INTENSITY = 0.75f;   // peak composite intensity
    private static final float SUN_RADIUS = 0.05f;       // disc radius, height fraction
    private static final float BLUR1_DENSITY = 0.28f;   // pass 1: short sharp shafts
    private static final float BLUR1_DECAY = 0.94f;
    private static final float BLUR1_EXPOSURE = 1.00f;
    private static final float BLUR2_DENSITY = 1.00f;    // pass 2: full-length soft rays
    private static final float BLUR2_DECAY = 0.968f;
    private static final float BLUR2_EXPOSURE = 1.50f;
    private static final int MARCH_STEPS = 20;           // CPU cloud-occlusion samples
    private static final double MARCH_MAX_BLOCKS = 700.0;

    private static final Matrix4f IDENTITY = new Matrix4f();

    // ── GPU state (lazily created, render thread only) ───────────────────

    private static VertexBuffer quad;
    private static TextureTarget passA;
    private static TextureTarget passB;
    private static boolean loggedFirstRender;

    /** Quad NDC z: past nearly everything, still LEQUAL under sky's 1.0. */
    private static final float QUAD_Z = 0.999f;

    // ── Entry (from the renderClouds mixin, after the cloud draw) ───────

    public static void render(Matrix4f viewMatrix, Matrix4f projectionMatrix,
                              double camX, double camY, double camZ, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        ShaderInstance occlusion = GodRayShaderRegistry.occlusionShader;
        ShaderInstance blur = GodRayShaderRegistry.blurShader;
        ShaderInstance composite = GodRayShaderRegistry.compositeShader;
        if (occlusion == null || blur == null || composite == null) return;

        float dayT = level.getTimeOfDay(partialTick);

        // sun direction in WORLD space (no camera rotation) -- elevation
        Matrix4f sunWorldMat = new Matrix4f()
                .rotate(Axis.YP.rotationDegrees(-90.0f))
                .rotate(Axis.XP.rotationDegrees(dayT * 360.0f));
        Vector3f sunWorld = sunWorldMat.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));
        float elevation = sunWorld.y;
        if (elevation < -0.02f) return;   // sun below the horizon

        // sun position on screen through the real view + projection
        Matrix4f sunViewMat = new Matrix4f(viewMatrix)
                .rotate(Axis.YP.rotationDegrees(-90.0f))
                .rotate(Axis.XP.rotationDegrees(dayT * 360.0f));
        Vector3f sunView = sunViewMat.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));
        Vector4f clip = new Vector4f(sunView.x * 100.0f, sunView.y * 100.0f, sunView.z * 100.0f, 1.0f);
        projectionMatrix.transform(clip);
        if (clip.w <= 0.0f) return;       // sun behind the camera
        float sunU = clip.x / clip.w * 0.5f + 0.5f;
        float sunV = clip.y / clip.w * 0.5f + 0.5f;

        // gates: elevation ramp, rain dim, shafts through our own clouds
        float elevGate = Mth.clamp((elevation + 0.02f) / 0.14f, 0.0f, 1.0f);
        float rain = level.getRainLevel(partialTick);
        float openness = cloudOpenness(level, sunWorld, camX, camY, camZ);
        float intensity = BASE_INTENSITY * elevGate * (1.0f - 0.78f * rain) * openness;
        if (CloudTileManager.isStorm(level)) intensity *= 0.45f;
        if (intensity <= 0.01f) return;

        // tint: warm at the horizon, near-white higher up
        float high = Mth.clamp(elevation / 0.35f, 0.0f, 1.0f);
        float tintR = 1.0f;
        float tintG = Mth.lerp(high, 0.78f, 0.99f);
        float tintB = Mth.lerp(high, 0.55f, 0.96f);

        com.mojang.blaze3d.pipeline.RenderTarget main = mc.getMainRenderTarget();
        float aspect = (float) main.width / main.height;
        ensureTargets(main.width, main.height);
        ensureQuad();

        int[] boundFbo = new int[1];
        GL30C.glGetIntegerv(GL30C.GL_DRAW_FRAMEBUFFER_BINDING, boundFbo);
        int prevFbo = boundFbo[0];

        // ── half-res passes (overwrite, no depth on the targets) ──────────
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        // pass 1: occlusion -- sun disc masked to sky pixels, into A
        passA.bindWrite(true);
        occlusion.setSampler("DiffuseSampler", main.getDepthTextureId());
        uniform2(occlusion, "SunScreen", sunU, sunV);
        uniform1(occlusion, "SunRadius", SUN_RADIUS);
        uniform1(occlusion, "ScreenAspect", aspect);
        drawQuad(occlusion);

        // pass 2: radial blur A -> B (short sharp reach)
        passB.bindWrite(true);
        blur.setSampler("DiffuseSampler", passA.getColorTextureId());
        uniform2(blur, "SunScreen", sunU, sunV);
        uniform1(blur, "RayDensity", BLUR1_DENSITY);
        uniform1(blur, "RayDecay", BLUR1_DECAY);
        uniform1(blur, "RayExposure", BLUR1_EXPOSURE);
        drawQuad(blur);

        // pass 3: radial blur B -> A (full-length soft reach)
        passA.bindWrite(true);
        blur.setSampler("DiffuseSampler", passB.getColorTextureId());
        uniform1(blur, "RayDensity", BLUR2_DENSITY);
        uniform1(blur, "RayDecay", BLUR2_DECAY);
        uniform1(blur, "RayExposure", BLUR2_EXPOSURE);
        drawQuad(blur);

        // ── composite onto the framebuffer the clouds drew into ──────────
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prevFbo);
        GlStateManager._viewport(0, 0, main.width, main.height);

        RenderSystem.enableDepthTest();   // quad at QUAD_Z: rays only where sky remains
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        composite.setSampler("DiffuseSampler", passA.getColorTextureId());
        uniform2(composite, "SunScreen", sunU, sunV);
        uniform3(composite, "RayColor", tintR, tintG, tintB);
        uniform1(composite, "RayIntensity", intensity);
        uniform1(composite, "ScreenAspect", aspect);
        drawQuad(composite);

        // restore the state the cloud draw leaves behind
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();

        if (!loggedFirstRender) {
            loggedFirstRender = true;
            LOGGER.info("first rays: sun=({}, {}), elev={}, openness={}, intensity={}",
                    String.format("%.2f", sunU), String.format("%.2f", sunV),
                    String.format("%.2f", elevation), String.format("%.2f", openness),
                    String.format("%.2f", intensity));
        }
    }

    // ── CPU: shafts through the voxel cloud field ────────────────────────

    /**
     * Marches the camera->sun segment through CloudDensityField (cloud
     * space = world minus drift, the tile system's seed) and returns how
     * open the sky above the camera is, 1 = clear shafts, 0 = sealed.
     */
    private static float cloudOpenness(Level level, Vector3f sunWorld,
                                       double camX, double camY, double camZ) {
        double bottom = CloudDensityField.LAYER_BOTTOM;
        double top = CloudDensityField.LAYER_TOP;
        if (camY >= top) return 1.0f;
        if (sunWorld.y <= 0.0f) return 1.0f;   // horizon-hugging sun: field barely blocks

        double startY = Math.max(camY + 4.0, bottom);
        double tStart = (startY - camY) / sunWorld.y;
        double tTop = (top - camY) / sunWorld.y;
        double tMax = Math.min(tTop, MARCH_MAX_BLOCKS);
        if (tMax <= tStart) return 1.0f;

        long seed = CloudTileManager.INSTANCE.cloudSeed();
        double driftX = CloudTileManager.INSTANCE.cloudDriftX();
        double driftZ = CloudTileManager.INSTANCE.cloudDriftZ();
        double threshold = CloudMesher.THRESHOLD;

        double sum = 0.0;
        for (int i = 0; i < MARCH_STEPS; i++) {
            double t = tStart + (tMax - tStart) * (i + 0.5) / MARCH_STEPS;
            double d = CloudDensityField.sample(seed,
                    camX + sunWorld.x * t - driftX,
                    camY + sunWorld.y * t,
                    camZ + sunWorld.z * t - driftZ);
            sum += Mth.clamp(d / threshold, 0.0, 1.0);
        }
        double avg = sum / MARCH_STEPS;
        return (float) Mth.clamp(1.0 - avg * 1.25, 0.0, 1.0);
    }

    // ── GPU helpers ───────────────────────────────────────────────────────

    private static void drawQuad(ShaderInstance shader) {
        quad.bind();   // drawWithShader never binds the VAO itself
        quad.drawWithShader(IDENTITY, IDENTITY, shader);
        VertexBuffer.unbind();
    }

    private static void ensureTargets(int width, int height) {
        int hw = Math.max(1, width / 2);
        int hh = Math.max(1, height / 2);
        if (passA == null) {
            passA = new TextureTarget(hw, hh, false, false);
            passB = new TextureTarget(hw, hh, false, false);
        } else if (passA.width != hw || passA.height != hh) {
            passA.resize(hw, hh, false);
            passB.resize(hw, hh, false);
        }
    }

    private static void ensureQuad() {
        if (quad != null) return;
        BufferBuilder buf = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(-1.0f, -1.0f, QUAD_Z).setUv(0.0f, 0.0f);
        buf.addVertex(1.0f, -1.0f, QUAD_Z).setUv(1.0f, 0.0f);
        buf.addVertex(1.0f, 1.0f, QUAD_Z).setUv(1.0f, 1.0f);
        buf.addVertex(-1.0f, 1.0f, QUAD_Z).setUv(0.0f, 1.0f);
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

    private static void uniform2(ShaderInstance shader, String name, float a, float b) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(a, b);
    }

    private static void uniform3(ShaderInstance shader, String name, float a, float b, float c) {
        Uniform u = shader.getUniform(name);
        if (u != null) u.set(a, b, c);
    }
}
