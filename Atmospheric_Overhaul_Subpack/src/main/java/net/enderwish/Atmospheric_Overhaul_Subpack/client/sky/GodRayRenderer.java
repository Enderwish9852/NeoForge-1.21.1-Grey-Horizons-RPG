package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
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
 *    matrices, one march from the camera toward the sun through
 *    OUR CloudDensityField as the intensity gate, and a 48x48
 *    cloud-transit map: each screen cell's view ray marched through
 *    the cloud layer (cloud space = world minus drift, the tile system's
 *    seed), so the occlusion buffer carries the REAL gap pattern and
 *    rays and clouds can never disagree;
 * 2. occlusion pass, half-res: the sun's ACTUAL DISC stamped only
 *    where the main target's DEPTH is exactly sky, so terrain
 *    silhouettes and tree canopies cut the light (ShaderInstance
 *    .setSampler takes the raw GL texture id);
 * 3. radial blur, half-res, run twice chained: short sharp reach then
 *    full-length soft reach, each accumulating toward the sun with a
 *    per-pass decay;
 * 4. composite: additive (ONE,ONE) onto the framebuffer the cloud
 *    render was drawing into (saved and rebound around the passes, so
 *    both Fabulous and plain rendering work), depth-tested at the far
 *    plane so rays land on sky pixels, never over near terrain, with
 *    the elevation-gated intensity and warm horizon tint.
 *
 * ── Round 3 rewrite ────────────────────────────────────────────────────
 * The old version had three faults that the user hit in game:
 *
 * a) THE FLASH BANG. The occlusion shader stamped a "glare" disc
 *    reaching SunRadius * 10.0. With the old constant 0.045 that is
 *    0.45 in screen-height units -- a blob 90% of the screen across --
 *    and the composite kept it at FULL strength for the first 0.45.
 *    The seed is now the sun's real disc, whose size comes from the
 *    projection matrix (so it matches the drawn sun exactly) plus a
 *    small halo.
 *
 * b) "ONLY AT CERTAIN ANGLES". SunRadius was a fixed screen fraction,
 *    so the light source never scaled with the FOV, and when the sun
 *    left the frame the blur degenerated into a faint wash. The sun's
 *    screen position now comes from the shared SunRenderer math, the
 *    seed radius is derived from the projection, and an off-screen sun
 *    switches to a broad, weaker seed at the frame edge so the shafts
 *    keep streaming in from the sun's direction.
 *
 * c) The sun behind the camera. The old code returned early
 *    (clip.w <= 0), so there were no rays at all for most of the day.
 *    Now the convergence point becomes the ANTI-SOLAR point and the
 *    shafts radiate from it, which is what real crepuscular rays do.
 *
 * v1 scope: sun only, no volumetric shadow pass (that would need a
 * sun-shadowmap render -- a separate feature).
 */
public final class GodRayRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("GHAtmospheric/GodRays");

    private GodRayRenderer() {}

    // ── Tuning (v1 constants) ─────────────────────────────────────────────

    private static final float BASE_INTENSITY = 0.50f;   // peak composite intensity
    private static final float BLUR1_DENSITY = 0.22f;    // pass 1: short sharp shafts
    private static final float BLUR1_DECAY = 0.955f;
    private static final float BLUR1_EXPOSURE = 0.13f;
    private static final float BLUR2_DENSITY = 1.00f;    // pass 2: full-length soft rays
    private static final float BLUR2_DECAY = 0.985f;
    private static final float BLUR2_EXPOSURE = 0.065f;
    private static final int MARCH_STEPS = 20;           // CPU sun-ray occlusion samples
    private static final double MARCH_MAX_BLOCKS = 700.0;
    private static final int MAP_SIZE = 48;              // cloud transit map cells per axis
    private static final int MAP_LAYER_SAMPLES = 3;      // density samples per map cell

    /**
     * Seed used when the sun is not in frame: a broad, weaker blob at
     * the frame edge (screen-height units) and how much of it there is.
     */
    private static final float OFFSCREEN_SEED_RADIUS = 0.22f;
    private static final float OFFSCREEN_SEED_STRENGTH = 0.55f;
    /** How far outside the frame the convergence point may sit. */
    private static final float OFFSCREEN_MARGIN = 0.12f;

    private static final Matrix4f IDENTITY = new Matrix4f();

    // ── GPU state (lazily created, render thread only) ───────────────────

    private static VertexBuffer quad;
    private static TextureTarget passA;
    private static TextureTarget passB;
    private static boolean loggedFirstRender;
    private static int cloudMapTex = -1;
    private static java.nio.ByteBuffer cloudMapBuf;

    /** Debug overlays: launch with -Dgh.raydebug=map or -Dgh.raydebug=buffer. */
    private static final boolean DEBUG_MAP = "map".equalsIgnoreCase(System.getProperty("gh.raydebug", ""));
    private static final boolean DEBUG_BUFFER = "buffer".equalsIgnoreCase(System.getProperty("gh.raydebug", ""));
    private static int frameCounter;
    private static int mapMin, mapMax, mapSum;
    private static float mapAvg;

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
        Vector3f sunWorld = SunRenderer.worldDirection(dayT);
        float elevation = sunWorld.y;
        if (elevation < -0.02f) return;   // sun below the horizon

        // where the shafts converge: the sun itself, or the anti-solar
        // point when the sun is behind the camera
        float[] conv = new float[3];
        SunRenderer.convergencePoint(viewMatrix, projectionMatrix, dayT, conv);
        boolean sunInFront = conv[2] > 0.5f;
        float sunU = conv[0];
        float sunV = conv[1];

        // gates: elevation ramp, rain dim, shafts through our own clouds
        float elevGate = Mth.clamp((elevation + 0.02f) / 0.14f, 0.0f, 1.0f);
        float rain = level.getRainLevel(partialTick);
        float openness = cloudOpenness(level, sunWorld, camX, camY, camZ);
        float intensity = BASE_INTENSITY * elevGate * (1.0f - 0.78f * rain) * openness;
        if (CloudTileManager.isStorm(level)) intensity *= 0.45f;
        if (intensity <= 0.01f && !DEBUG_MAP && !DEBUG_BUFFER) return;

        // tint: warm at the horizon, near-white higher up
        float high = Mth.clamp(elevation / 0.35f, 0.0f, 1.0f);
        float tintR = 1.0f;
        float tintG = Mth.lerp(high, 0.78f, 0.99f);
        float tintB = Mth.lerp(high, 0.55f, 0.96f);

        com.mojang.blaze3d.pipeline.RenderTarget main = mc.getMainRenderTarget();
        float aspect = (float) main.width / main.height;
        ensureTargets(main.width, main.height);
        ensureQuad();
        buildCloudMap(viewMatrix, projectionMatrix, camX, camY, camZ);

        // ── the light source ─────────────────────────────────────────────
        // the sun's real disc, straight off the projection matrix, so it
        // is exactly the disc SunRenderer draws
        float discRadius = SunRenderer.angularRadiusToUv(SunRenderer.DISC_ANGLE, projectionMatrix);
        boolean onScreen = sunU >= 0.0f && sunU <= 1.0f && sunV >= 0.0f && sunV <= 1.0f;

        float seedRadius;
        float seedStrength;
        float falloffInner;
        float falloffOuter;
        if (sunInFront && onScreen) {
            // the sun is in frame: its disc IS the light source
            seedRadius = discRadius;
            seedStrength = 1.0f;
            falloffInner = 0.12f;
            falloffOuter = 1.05f;
        } else {
            // the sun is off-frame (or behind us): a broad, weaker seed
            // just outside the frame, so the shafts keep streaming in
            // from the sun's direction instead of dying
            sunU = Mth.clamp(sunU, -OFFSCREEN_MARGIN, 1.0f + OFFSCREEN_MARGIN);
            sunV = Mth.clamp(sunV, -OFFSCREEN_MARGIN, 1.0f + OFFSCREEN_MARGIN);
            seedRadius = OFFSCREEN_SEED_RADIUS;
            seedStrength = OFFSCREEN_SEED_STRENGTH;
            falloffInner = 0.45f;
            falloffOuter = 2.20f;
        }

        frameCounter++;
        if (frameCounter % 100 == 1) {
            LOGGER.info("rays #{}: sun=({}, {}) front={} onScreen={} elev={} openness={} intensity={} seed={} map[min={} avg={} max={}]",
                    frameCounter,
                    String.format("%.2f", sunU), String.format("%.2f", sunV),
                    sunInFront, onScreen,
                    String.format("%.2f", elevation), String.format("%.2f", openness),
                    String.format("%.2f", intensity),
                    String.format("%.3f", seedRadius),
                    mapMin, String.format("%.1f", mapAvg), mapMax);
        }

        int[] boundFbo = new int[1];
        GL30C.glGetIntegerv(GL30C.GL_DRAW_FRAMEBUFFER_BINDING, boundFbo);
        int prevFbo = boundFbo[0];

        // ── half-res passes (overwrite, no depth on the targets) ──────────
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        // pass 1: occlusion -- the sun's disc masked to sky pixels, into A
        passA.bindWrite(true);
        occlusion.setSampler("DiffuseSampler", main.getDepthTextureId());
        occlusion.setSampler("CloudMap", cloudMapTex);
        uniform2(occlusion, "SunScreen", sunU, sunV);
        uniform1(occlusion, "SeedRadius", seedRadius);
        uniform1(occlusion, "SeedStrength", seedStrength);
        uniform1(occlusion, "ScreenAspect", aspect);
        drawQuad(occlusion);

        // pass 2 + 3: radial blur A -> B (short sharp) -> A (full-length soft)
        if (!DEBUG_MAP) {
            passB.bindWrite(true);
            blur.setSampler("DiffuseSampler", passA.getColorTextureId());
            uniform2(blur, "SunScreen", sunU, sunV);
            uniform1(blur, "RayDensity", BLUR1_DENSITY);
            uniform1(blur, "RayDecay", BLUR1_DECAY);
            uniform1(blur, "RayExposure", BLUR1_EXPOSURE);
            uniform1(blur, "ScreenAspect", aspect);
            drawQuad(blur);

            passA.bindWrite(true);
            blur.setSampler("DiffuseSampler", passB.getColorTextureId());
            uniform1(blur, "RayDensity", BLUR2_DENSITY);
            uniform1(blur, "RayDecay", BLUR2_DECAY);
            uniform1(blur, "RayExposure", BLUR2_EXPOSURE);
            drawQuad(blur);
        }

        // ── composite onto the framebuffer the clouds drew into ──────────
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prevFbo);
        GlStateManager._viewport(0, 0, main.width, main.height);

        if (DEBUG_MAP || DEBUG_BUFFER) {
            // debug overlay: raw view of the chosen stage, fullscreen overwrite
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            composite.setSampler("DiffuseSampler", DEBUG_MAP ? cloudMapTex : passA.getColorTextureId());
            uniform1(composite, "DebugMode", DEBUG_MAP ? 1.0f : 2.0f);
            drawQuad(composite);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            return;
        }

        RenderSystem.enableDepthTest();   // quad at QUAD_Z: rays only where sky remains
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        composite.setSampler("DiffuseSampler", passA.getColorTextureId());
        uniform1(composite, "DebugMode", 0.0f);
        uniform2(composite, "SunScreen", sunU, sunV);
        uniform3(composite, "RayColor", tintR, tintG, tintB);
        uniform1(composite, "RayIntensity", intensity);
        uniform1(composite, "ScreenAspect", aspect);
        uniform2(composite, "FalloffRange", falloffInner, falloffOuter);
        drawQuad(composite);

        // restore the state the cloud draw leaves behind
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();

        if (!loggedFirstRender) {
            loggedFirstRender = true;
            LOGGER.info("first rays: sun=({}, {}) front={} elev={} openness={} intensity={} seed={}",
                    String.format("%.2f", sunU), String.format("%.2f", sunV),
                    sunInFront,
                    String.format("%.2f", elevation), String.format("%.2f", openness),
                    String.format("%.2f", intensity), String.format("%.3f", seedRadius));
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

    // ── CPU: cloud transit map (the gap pattern the shafts stream through) ──

    /**
     * Builds a MAP_SIZE^2 cloud-transit map over the screen: each cell's
     * view ray (inverse projection, inverse view) is marched through the
     * cloud layer in cloud space (world minus drift, the tile system's
     * seed), 1 = open gap, 0 = sealed cloud. Uploaded as a small linear
     * texture the occlusion pass multiplies the sun stamp by, so the
     * radial blur drags the REAL cloud gap pattern into shafts -- rays
     * and rendered clouds can never disagree.
     */
    private static void buildCloudMap(Matrix4f viewMatrix, Matrix4f projectionMatrix,
                                      double camX, double camY, double camZ) {
        ensureCloudMap();
        long seed = CloudTileManager.INSTANCE.cloudSeed();
        double driftX = CloudTileManager.INSTANCE.cloudDriftX();
        double driftZ = CloudTileManager.INSTANCE.cloudDriftZ();
        double threshold = CloudMesher.THRESHOLD;
        Matrix4f projInv = new Matrix4f(projectionMatrix).invert();
        Matrix4f viewInv = new Matrix4f(viewMatrix).invert();
        Vector4f near = new Vector4f();
        Vector3f viewDir = new Vector3f();
        mapMin = 255;
        mapMax = 0;
        mapSum = 0;
        cloudMapBuf.clear();
        for (int iy = 0; iy < MAP_SIZE; iy++) {
            for (int ix = 0; ix < MAP_SIZE; ix++) {
                near.set(((ix + 0.5f) / MAP_SIZE) * 2.0f - 1.0f,
                         ((iy + 0.5f) / MAP_SIZE) * 2.0f - 1.0f,
                         -1.0f, 1.0f);
                projInv.transform(near);
                viewDir.set(near.x / near.w, near.y / near.w, near.z / near.w).normalize();
                Vector3f worldDir = viewInv.transformDirection(viewDir);
                float open = transitThroughLayer(worldDir, camX, camY, camZ,
                        seed, driftX, driftZ, threshold);
                int vi = (int) (Mth.clamp(open, 0.0f, 1.0f) * 255.0f + 0.5f);
                mapMin = Math.min(mapMin, vi);
                mapMax = Math.max(mapMax, vi);
                mapSum += vi;
                byte v = (byte) vi;
                cloudMapBuf.put(v).put(v).put(v).put((byte) 255);
            }
        }
        mapAvg = (float) mapSum / (MAP_SIZE * MAP_SIZE);
        cloudMapBuf.flip();
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, cloudMapTex);
        GL30C.glTexSubImage2D(GL30C.GL_TEXTURE_2D, 0, 0, 0, MAP_SIZE, MAP_SIZE,
                GL30C.GL_RGBA, GL30C.GL_UNSIGNED_BYTE, cloudMapBuf);
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, 0);
    }

    /** How open the cloud layer is along one view ray, 1 = clear, 0 = walled. */
    private static float transitThroughLayer(Vector3f dir, double camX, double camY, double camZ,
                                             long seed, double driftX, double driftZ,
                                             double threshold) {
        double bottom = CloudDensityField.LAYER_BOTTOM;
        double top = CloudDensityField.LAYER_TOP;
        if (Math.abs(dir.y()) < 0.02f) return 1.0f;   // horizon-grazer: no stable crossing
        double tBottom = (bottom - camY) / dir.y();
        double tTop = (top - camY) / dir.y();
        double tLo = Math.min(tBottom, tTop);
        double tHi = Math.max(tBottom, tTop);
        if (tHi <= 0.0) return 1.0f;                  // layer entirely behind the ray
        tLo = Math.max(tLo, 0.0);
        if (tHi - tLo < 4.0) return 1.0f;             // only grazes the layer edge
        double sum = 0.0;
        for (int i = 0; i < MAP_LAYER_SAMPLES; i++) {
            double t = tLo + (tHi - tLo) * (i + 0.5) / MAP_LAYER_SAMPLES;
            double d = CloudDensityField.sample(seed,
                    camX + dir.x() * t - driftX,
                    camY + dir.y() * t,
                    camZ + dir.z() * t - driftZ);
            sum += Mth.clamp(d / threshold, 0.0, 1.0);
        }
        double avg = sum / MAP_LAYER_SAMPLES;
        return (float) Mth.clamp(1.0 - avg * 1.35, 0.0, 1.0);
    }

    /** Allocates the small cloud-transit map texture and its upload buffer. */
    private static void ensureCloudMap() {
        if (cloudMapTex > 0) return;
        cloudMapTex = GL30C.glGenTextures();
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, cloudMapTex);
        GL30C.glTexImage2D(GL30C.GL_TEXTURE_2D, 0, GL30C.GL_RGBA8, MAP_SIZE, MAP_SIZE, 0,
                GL30C.GL_RGBA, GL30C.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MIN_FILTER, GL30C.GL_LINEAR);
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MAG_FILTER, GL30C.GL_LINEAR);
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_S, GL30C.GL_CLAMP_TO_EDGE);
        GL30C.glTexParameteri(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_T, GL30C.GL_CLAMP_TO_EDGE);
        GL30C.glBindTexture(GL30C.GL_TEXTURE_2D, 0);
        cloudMapBuf = org.lwjgl.BufferUtils.createByteBuffer(MAP_SIZE * MAP_SIZE * 4);
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
