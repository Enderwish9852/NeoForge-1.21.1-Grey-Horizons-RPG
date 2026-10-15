package net.enderwish.Atmospheric_Overhaul_Subpack.client.sky;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * VoxelCloudEngine
 *
 * NEW -- a true 3D voxel-cloud field replacing vanilla's flat cloud
 * plane (rendered from the renderClouds mixin; vanilla implementation
 * is cancelled at HEAD).
 *
 * Three strata of low/mid/high clouds, each its own cell size, height
 * band, coverage and drift speed. A stratum's columns are a pure
 * function of world x/z (seeded value noise, 2-3 octaves), so the field
 * is endless, stable and cheap: no chunk storage, only the visible
 * window around the camera is meshed.
 *
 * Clouds drift along the synced WIND (ClientSeasonState): the drift is
 * an offset in the NOISE SAMPLING position (whole cells, baked at mesh
 * time) plus a sub-cell draw translate (applied per frame), so the
 * field advects smoothly while staying world-anchored. Storm weather
 * reads denser + darker. Faces are sun-shaded (top bright, sides mid,
 * bottom dark) and tinted by time of day (warm at sunrise/sunset, dark
 * blue-gray at night, gray under storm).
 *
 * Meshes are rebuilt lazily -- at most one stratum per game tick --
 * uploaded into persistent VertexBuffers and drawn with the position
 * color shader. All tuning constants live in STRATA below (config
 * exposure comes later).
 */
public final class VoxelCloudEngine {

    private VoxelCloudEngine() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("GHAtmospheric/VoxelClouds");
    private static boolean loggedFirstFrame;
    private static boolean loggedVerts;
    private static boolean loggedFirstDraw;

    public static boolean isEnabled() { return true; }

    // ── Tuning (constants for v1; expose in config later) ────────────────

    /** window radius in the LARGEST stratum's cells; smaller strata divide their own */
    private static final int WINDOW_MIN_BLOCKS = 384;

    private static final Stratum[] STRATA = {
            new Stratum(12, 126, 3, 0.30, 1.35, 0.028, 0.005, 0.88f),   // low scud, fast
            new Stratum(20, 152, 5, 0.34, 0.60, 0.016, 0.0035, 0.86f),  // cumulus band
            new Stratum(36, 186, 2, 0.20, 0.28, 0.010, 0.0025, 0.60f),  // high wisps, slow
    };
    // NOTE: heights are kept within ~128 blocks of a sea-level camera so
    // they survive the projection far plane (getDepthFar = renderDistance*4;
    // renderDistance 2 -> far 128). Vanilla's y=192 plane is invisible at
    // that setting.
    private record Stratum(int cell, int baseY, int maxThick,
                           double coverage, double speedBlocksPerTick,
                           double noiseFreq, double clusterFreq, float alpha) {}

    // ── GPU buffers + rebuild bookkeeping ───────────────────────────────

    private static final VertexBuffer[] BUFFERS = new VertexBuffer[STRATA.length];
    private static final float[] SLIDES = new float[STRATA.length * 2]; // sub-cell draw translate per stratum
    private static int nextRebuild = 0;       // round-robin stratum pointer
    private static long lastRebuiltTick = -1;

    // ── Entry (from the mixin, every frame) ─────────────────────────────

    /**
     * @param viewMatrix       the camera-rotation matrix vanilla passes as
     *                         renderClouds' frustumMatrix (rotation only,
     *                         no camera translation)
     * @param projectionMatrix the actual projection matrix
     */
    public static void render(Matrix4f viewMatrix, Matrix4f projectionMatrix,
                              double camX, double camY, double camZ) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        ensureMeshes(level, camX, camZ);

        ShaderInstance shader = GameRenderer.getPositionColorShader();
        if (shader == null) return;

        if (!loggedFirstFrame) {
            loggedFirstFrame = true;
            LOGGER.info("first frame: shader ok, cam=({}, {}, {}), fogStart={} fogEnd={}",
                    camX, camY, camZ, RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd());
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();          // vanilla clouds render NO_CULL
        FogRenderer.levelFogColor();

        // the terrain fog uniforms are still active here (fog end = render
        // distance, 32 blocks at RD 2) -- clouds sit far beyond it and the
        // position_color shader applies fog, so push fog out for this draw
        // and restore after
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        FogShape fogShape = RenderSystem.getShaderFogShape();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);

        for (int s = 0; s < STRATA.length; s++) {
            VertexBuffer vb = BUFFERS[s];
            if (vb == null) continue;

            Matrix4f mv = new Matrix4f(viewMatrix)
                    .translate((float) (-camX - SLIDES[s * 2]), (float) -camY,
                            (float) (-camZ - SLIDES[s * 2 + 1]));
            // REQUIRED: VertexBuffer._drawWithShader sets uniforms and calls
            // glDrawElements but NEVER binds the buffer's VAO (verified in
            // the NeoForge sources -- vanilla itself binds first). Without
            // this the draw ran against VAO 0 -> GL_INVALID_OPERATION ->
            // nothing ever rendered.
            vb.bind();
            vb.drawWithShader(mv, projectionMatrix, shader);
            VertexBuffer.unbind();

            if (!loggedFirstDraw) {
                loggedFirstDraw = true;
                int errAfter = GL11.glGetError();
                LOGGER.info("draw stratum {}: glErrAfterDraw=0x{}",
                        s, Integer.toHexString(errAfter));
            }
        }

        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.setShaderFogEnd(fogEnd);
        if (fogShape != null) RenderSystem.setShaderFogShape(fogShape);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static float windX() { return (float) ClientSeasonState.getWindState().getEffectiveDx(); }
    private static float windZ() { return (float) ClientSeasonState.getWindState().getEffectiveDz(); }

    private static float windFactor() {
        try {
            float s = ClientSeasonState.getWindState().getSpeed();
            return 0.5f + Math.min(2.0f, s) * 0.5f;
        } catch (Exception e) { return 1.0f; }
    }

    // ── Meshing ─────────────────────────────────────────────────────────

    private static void ensureMeshes(Level level, double camX, double camZ) {
        long tick = level.getGameTime();
        if (lastRebuiltTick == tick) return;
        lastRebuiltTick = tick;

        int s = nextRebuild;
        nextRebuild = (nextRebuild + 1) % STRATA.length;

        Stratum st = STRATA[s];
        int windowCells = Math.max(3, WINDOW_MIN_BLOCKS / st.cell);

        // wind advection: offset in the noise sampling position. Whole-cell
        // part is baked into the mesh; the remainder is a per-frame draw
        // translate (pattern-continuous across rebuilds).
        double drift = tick * st.speedBlocksPerTick * windFactor();
        double offX = drift * windX();
        double offZ = drift * windZ();
        double cellQx = Math.floor(offX / st.cell) * st.cell;
        double cellQz = Math.floor(offZ / st.cell) * st.cell;
        SLIDES[s * 2]     = (float) (offX - cellQx);
        SLIDES[s * 2 + 1] = (float) (offZ - cellQz);

        MeshData data = meshStratum(level, st, windowCells, isStorm(level),
                camX, camZ, cellQx, cellQz);

        if (data != null) {
            VertexBuffer vb = BUFFERS[s];
            if (vb == null) {
                vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
                BUFFERS[s] = vb;
                LOGGER.info("stratum {} first mesh: {} quads (windowCells={}, camX={} camZ={})",
                        s, data.drawState().vertexCount() / 4, windowCells, camX, camZ);
            }
            vb.bind();
            vb.upload(data);   // consumes (closes) the mesh
            VertexBuffer.unbind();
        } else if (BUFFERS[s] != null) {
            BUFFERS[s].close();
            BUFFERS[s] = null;
        }
    }

    private static MeshData meshStratum(Level level, Stratum st, int windowCells, boolean storm,
                                        double camX, double camZ,
                                        double sampleOffX, double sampleOffZ) {
        long cx = (long) Math.floor(camX / st.cell);
        long cz = (long) Math.floor(camZ / st.cell);

        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);

        float[] tint = cloudTint(level);
        float coverMul = storm ? 1.35f : 1.0f;

        for (long gx = cx - windowCells; gx <= cx + windowCells; gx++) {
            for (long gz = cz - windowCells; gz <= cz + windowCells; gz++) {
                double wx = (gx * st.cell + sampleOffX) * st.noiseFreq;
                double wz = (gz * st.cell + sampleOffZ) * st.noiseFreq;

                double n = fbm(wx, wz);
                // CLUSTER mask: large-scale noise modulates coverage so the
                // field forms distinct cloud MASSES with real sky between
                // them, instead of a uniform half-filled sheet (the tiled-
                // ceiling look). Per-stratum frequency: masses ~200-500 blocks.
                double cluster = Mth.clamp(0.2 + 1.6 * fbm(
                        (gx * st.cell + sampleOffX) * st.clusterFreq + 7.7,
                        (gz * st.cell + sampleOffZ) * st.clusterFreq + 3.3), 0.0, 1.0);
                double thr = 1.0 - Math.min(0.75, st.coverage * coverMul * cluster);

                // rim thinning: RAISE the threshold toward 1.0 at the window
                // border (the old *edgeFade made filling EASIER at the rim,
                // walling the horizon in cloud)
                double edgeFade = edgeFade(gx, gz, cx, cz, windowCells);
                thr = Math.min(1.0, thr + (1.0 - edgeFade));

                if (n > thr) continue;

                int thick = 1 + (int) (fbm(wx * 2.7 + 13.7, wz * 2.7 + 7.3) * st.maxThick);
                // LOW-frequency height offset: whole cloud masses share one
                // base height (per-cell jitter was drawing step seams that
                // read as a tiled ceiling)
                int y0 = st.baseY + (int) (fbm(wx * 0.06 + 99.1, wz * 0.06 + 31.7) * 5);

                for (int dy = 0; dy < thick; dy++) {
                    emitColumnCells(buf, st, gx, gz, y0 + dy,
                            dy == thick - 1, dy == 0, tint, sampleOffX, sampleOffZ);
                }
            }
        }

        return buf.build();   // null when the window holds no clouds
    }

    /** Emits the visible faces of one column cell, neighbor-aware. */
    private static void emitColumnCells(BufferBuilder buf, Stratum st,
                                        long gx, long gz, int y,
                                        boolean topExposed, boolean bottomExposed,
                                        float[] tint, double sampleOffX, double sampleOffZ) {
        float x0 = gx * (float) st.cell, x1 = x0 + st.cell;
        float z0 = gz * (float) st.cell, z1 = z0 + st.cell;

        boolean[] sideOpen = {
                !occupied(st, gx, gz - 1, sampleOffX, sampleOffZ),  // north -Z
                !occupied(st, gx, gz + 1, sampleOffX, sampleOffZ),  // south +Z
                !occupied(st, gx - 1, gz, sampleOffX, sampleOffZ),  // west -X
                !occupied(st, gx + 1, gz, sampleOffX, sampleOffZ)   // east +X
        };

        float a = st.alpha;
        if (topExposed)
            quad(buf, tint[0], tint[1], tint[2], a,
                    x0, y + 1, z0, x1, y + 1, z1);
        if (bottomExposed)
            quad(buf, tint[0] * 0.5f, tint[1] * 0.5f, tint[2] * 0.55f, a,
                    x0, y, z0, x1, y, z1);
        if (sideOpen[0])
            quad(buf, tint[0] * 0.78f, tint[1] * 0.78f, tint[2] * 0.80f, a,
                    x0, y, z0, x1, y + 1, z0);
        if (sideOpen[1])
            quad(buf, tint[0] * 0.78f, tint[1] * 0.78f, tint[2] * 0.80f, a,
                    x0, y, z1, x1, y + 1, z1);
        if (sideOpen[2])
            quad(buf, tint[0] * 0.66f, tint[1] * 0.66f, tint[2] * 0.68f, a,
                    x0, y, z0, x0, y + 1, z1);
        if (sideOpen[3])
            quad(buf, tint[0] * 0.66f, tint[1] * 0.66f, tint[2] * 0.68f, a,
                    x1, y, z0, x1, y + 1, z1);
    }

    /**
     * Axis-aligned planar quad: (x0,y0,z0) and (x1,y1,z1) are opposite corners.
     * One axis of the corner pair is always constant (the face plane) -- the
     * vertex loop must run along the two VARYING axes, otherwise the quad
     * collapses to a zero-area line (the old fixed formula produced degenerate
     * west/east faces, where X is the constant axis).
     */
    private static void quad(BufferBuilder buf,
                             float r, float g, float b, float a,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1) {
        if (y0 == y1 || z0 == z1) {
            // plane const Y (top/bottom) or const Z (north/south): spans X then (Z|Y)
            buf.addVertex(x0, y0, z0).setColor(r, g, b, a);
            buf.addVertex(x1, y0, z0).setColor(r, g, b, a);
            buf.addVertex(x1, y1, z1).setColor(r, g, b, a);
            buf.addVertex(x0, y1, z1).setColor(r, g, b, a);
        } else {
            // plane const X (west/east): spans Z then Y
            buf.addVertex(x0, y0, z0).setColor(r, g, b, a);
            buf.addVertex(x0, y0, z1).setColor(r, g, b, a);
            buf.addVertex(x1, y1, z1).setColor(r, g, b, a);
            buf.addVertex(x1, y1, z0).setColor(r, g, b, a);
        }
    }

    private static boolean occupied(Stratum st, long gx, long gz,
                                    double sampleOffX, double sampleOffZ) {
        // MUST match the fill test's interior rule (cluster mask, no rim
        // easing) or side faces get drawn between filled neighbors
        double wx = (gx * st.cell + sampleOffX) * st.noiseFreq;
        double wz = (gz * st.cell + sampleOffZ) * st.noiseFreq;
        double n = fbm(wx, wz);
        double cluster = Mth.clamp(0.2 + 1.6 * fbm(
                (gx * st.cell + sampleOffX) * st.clusterFreq + 7.7,
                (gz * st.cell + sampleOffZ) * st.clusterFreq + 3.3), 0.0, 1.0);
        double thr = 1.0 - Math.min(0.75, st.coverage * cluster);
        return n <= thr;
    }

    // ── Noise + tint helpers ────────────────────────────────────────────

    /** 2-3 octave value noise in [0,1). Cheap and stable. */
    private static double fbm(double x, double z) {
        double sum = valueNoise(x, z) * 0.55
                   + valueNoise(x * 2.13 + 5.5, z * 2.13 + 8.2) * 0.28
                   + valueNoise(x * 4.41 + 17.3, z * 4.41 + 3.9) * 0.17;
        return sum;
    }

    private static double valueNoise(double x, double z) {
        long xi = (long) Math.floor(x), zi = (long) Math.floor(z);
        double xf = x - xi, zf = z - zi;
        double u = xf * xf * (3 - 2 * xf), v = zf * zf * (3 - 2 * zf);

        double a = hash(xi, zi), b = hash(xi + 1, zi);
        double c = hash(xi, zi + 1), d = hash(xi + 1, zi + 1);
        return Mth.lerp(v, Mth.lerp(u, a, b), Mth.lerp(u, c, d));
    }

    private static double hash(long x, long z) {
        long h = x * 374761393L + z * 668265263L;
        h = (h ^ (h >> 13)) * 1274126177L;
        return ((h ^ (h >> 16)) & 0xFFFFL) / 65535.0;
    }

    private static float edgeFade(long gx, long gz, long cx, long cz, int windowCells) {
        double dx = Math.abs(gx - cx) / (double) windowCells;
        double dz = Math.abs(gz - cz) / (double) windowCells;
        double d = Math.max(dx, dz);
        // full clouds inside 60%, smooth fade to 0 at the border
        if (d < 0.6) return 1.0f;
        return (float) Mth.clamp(1.0 - (d - 0.6) / 0.4, 0, 1);
    }

    private static boolean isStorm(Level level) {
        try {
            String w = ClientSeasonState.getWeatherId();
            return w != null && (w.contains("storm") || w.contains("thunder")
                    || w.contains("rain") || w.contains("blizzard"));
        } catch (Exception e) { return level.isRaining(); }
    }

    /** Cloud color by time of day + weather, in-place 3-float array. */
    private static float[] cloudTint(Level level) {
        float[] out = new float[3];
        float dayFraction = level.getTimeOfDay(1.0f);   // 0 sunrise, .25 noon, .5 sunset

        boolean storm = isStorm(level);

        // base by day brightness (use skyDarken for storms/night)
        float bright = 1.0f - level.getSkyDarken() / 15f * 0.75f;

        float r = bright, g = bright, b = bright;

        // sunrise/sunset warm tint: near dayFraction 0 and 0.5
        float sunsetness = sunsetAmount(dayFraction);
        if (sunsetness > 0) {
            r = Mth.lerp(sunsetness, r, 1.00f * bright);
            g = Mth.lerp(sunsetness, g, 0.72f * bright);
            b = Mth.lerp(sunsetness, b, 0.50f * bright);
        }

        // night: pull toward dark blue-gray
        float nightness = nightAmount(dayFraction);
        r = Mth.lerp(nightness, r, 0.24f);
        g = Mth.lerp(nightness, g, 0.27f);
        b = Mth.lerp(nightness, b, 0.38f);

        if (storm) { r *= 0.55f; g *= 0.57f; b *= 0.62f; }

        out[0] = Mth.clamp(r, 0, 1);
        out[1] = Mth.clamp(g, 0, 1);
        out[2] = Mth.clamp(b, 0, 1);
        return out;
    }

    private static float sunsetAmount(float dayFraction) {
        // warm around sunrise (0) and sunset (0.5), wrapping
        float d = Math.min(dayFraction, 1f - dayFraction);        // dist to 0
        float s = Math.abs(dayFraction - 0.5f);                    // dist to 0.5
        float m = Math.min(d, s);
        return m > 0.08f ? 0 : 1f - m / 0.08f;
    }

    private static float nightAmount(float dayFraction) {
        // 1 deep night (~0.75), 0 during day
        float v = (float) Math.abs(Math.sin(dayFraction * Math.PI)); // 0 at 0/.5, 1 at .75
        return Mth.clamp((v - 0.7f) / 0.3f, 0, 1);
    }
}
